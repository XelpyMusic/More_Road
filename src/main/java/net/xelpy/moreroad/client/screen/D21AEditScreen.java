package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.network.UpdateD21APayload;

/**
 * Éditeur moderne du D21 simple.
 * La structure reprend volontairement celle du D21A2 pour que tous les
 * panneaux directionnels aient les mêmes repères visuels.
 */
public class D21AEditScreen extends Screen {

    private final BlockPos blockPos;
    private final D21APanelData[] panels = new D21APanelData[D21ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex;
    private boolean panelEnabled = true;
    private D21AType selectedType = D21AType.WHITE;
    private boolean arrowRight;
    private boolean autorouteLogo;

    private EditBox destinationField;
    private EditBox distanceField;

    private final SignEditorUi.Rect[] tabRects = new SignEditorUi.Rect[D21ABlockEntity.MAX_PANELS];
    private final SignEditorUi.Rect[] tabToggleRects = new SignEditorUi.Rect[D21ABlockEntity.MAX_PANELS];
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect styleRect;
    private SignEditorUi.Rect structureRect;
    private SignEditorUi.Rect whiteRect;
    private SignEditorUi.Rect greenRect;
    private SignEditorUi.Rect blueRect;
    private SignEditorUi.Rect logoRect;
    private SignEditorUi.Rect directionLeftRect;
    private SignEditorUi.Rect directionRightRect;
    private SignEditorUi.Rect enabledRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private final SignEditorUi.Rect[] settingsPageRects = new SignEditorUi.Rect[3];
    private int settingsPage = 0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public D21AEditScreen(BlockPos blockPos, D21APanelData[] currentPanels) {
        super(Component.literal("Éditeur de panneau D21"));
        this.blockPos = blockPos.immutable();

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel = currentPanels != null && i < currentPanels.length ? currentPanels[i] : null;
            if (panel == null) {
                panel = i == 0 ? D21APanelData.firstPanelDefault() : D21APanelData.disabled();
            }
            this.panels[i] = panel;
        }
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(10, Math.min(28, this.width / 30));
        int marginY = Math.max(8, Math.min(20, this.height / 30));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;

        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1080.0F, 660.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int headerHeight = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsHeight = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footerHeight = SignEditorUi.adaptiveFooterHeight(this.scale);
        int bodyTop = this.windowY + headerHeight + tabsHeight;
        int bodyBottom = this.windowY + this.windowHeight - footerHeight;
        int bodyHeight = bodyBottom - bodyTop;
        int columnGap = s(tight ? 9 : 14);

        int leftWidth = Math.max(s(300), Math.round((this.windowWidth - pad * 2 - columnGap) * 0.46F));
        int rightWidth = this.windowWidth - pad * 2 - columnGap - leftWidth;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftWidth + columnGap;

        int tabGap = s(8);
        int tabsTotalWidth = this.windowWidth - pad * 2;
        int tabWidth = (tabsTotalWidth - tabGap * 3) / 4;
        int tabY = this.windowY + headerHeight;
        for (int i = 0; i < this.tabRects.length; i++) {
            int tx = this.windowX + pad + i * (tabWidth + tabGap);
            this.tabRects[i] = new SignEditorUi.Rect(tx, tabY, tabWidth, s(32));
            this.tabToggleRects[i] = new SignEditorUi.Rect(tx + tabWidth - s(34), tabY + s(7), s(28), s(18));
        }

        this.previewRect = new SignEditorUi.Rect(leftX, bodyTop, leftWidth, bodyHeight - s(8));

        int sectionGap = s(ultraTight ? 5 : tight ? 7 : 12);
        if (pagedUi()) {
            int pageHeight = 22;
            SignEditorUi.Rect pageBar = new SignEditorUi.Rect(rightX, bodyTop, rightWidth, pageHeight);
            SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, this.settingsPageRects.length, pageHeight, 4);
            System.arraycopy(pages, 0, this.settingsPageRects, 0, this.settingsPageRects.length);
            int cardY = bodyTop + pageHeight + 6;
            SignEditorUi.Rect fullCard = new SignEditorUi.Rect(rightX, cardY, rightWidth, Math.max(96, bodyHeight - pageHeight - 10));
            this.contentRect = fullCard;
            this.styleRect = fullCard;
            this.structureRect = fullCard;
        } else {
            int availableSections = Math.max(120, bodyHeight - s(ultraTight ? 2 : 8));
            int[] sectionHeights = SignEditorUi.fitSections(
                    availableSections,
                    sectionGap,
                    new float[]{0.27F, 0.43F, 0.30F},
                    new int[]{s(ultraTight ? 56 : tight ? 70 : 94), s(ultraTight ? 76 : tight ? 96 : 134), s(ultraTight ? 54 : tight ? 66 : 82)}
            );

            int y = bodyTop;
            this.contentRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[0]);
            y += sectionHeights[0] + sectionGap;
            this.styleRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[1]);
            y += sectionHeights[1] + sectionGap;
            this.structureRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[2]);
        }

        initFieldsAndControls();

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionWidth = Math.max(74, Math.max(s(150), Math.max(this.font.width("✓  Appliquer"), this.font.width("×  Annuler")) + 18));
        int actionGap = Math.max(4, s(10));
        int actionY = this.windowY + this.windowHeight - Math.max(s(38), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionWidth, actionY, actionWidth, actionH);
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - actionGap - actionWidth, actionY, actionWidth, actionH);

        loadSelectedPanelIntoWidgets();
        updatePagedVisibility();
        this.setInitialFocus(this.destinationField);
    }

    private void initFieldsAndControls() {
        int contentInnerX = this.contentRect.x() + s(10);
        int contentInnerWidth = this.contentRect.width() - s(20);
        int fieldY = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 46));
        int gap = pagedUi() ? 8 : s(8);
        int distanceWidth = Math.max(s(72), Math.round(contentInnerWidth * 0.24F));
        int destinationWidth = contentInnerWidth - distanceWidth - gap;

        this.destinationField = new EditBox(
                this.font,
                contentInnerX,
                fieldY,
                destinationWidth,
                pagedUi() ? 20 : s(24),
                Component.literal("Destination")
        );
        this.destinationField.setMaxLength(48);
        this.addRenderableWidget(this.destinationField);

        this.distanceField = new EditBox(
                this.font,
                contentInnerX + destinationWidth + gap,
                fieldY,
                distanceWidth,
                pagedUi() ? 20 : s(24),
                Component.literal("Kilométrage")
        );
        this.distanceField.setMaxLength(8);
        this.addRenderableWidget(this.distanceField);

        int styleInnerX = this.styleRect.x() + s(10);
        int styleInnerWidth = this.styleRect.width() - s(20);
        int styleGap = pagedUi() ? 8 : s(8);
        int colorY = this.styleRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 50));
        int colorHeight = pagedUi() ? 20 : s(24);
        int colorWidth = (styleInnerWidth - styleGap * 2) / 3;
        this.whiteRect = new SignEditorUi.Rect(styleInnerX, colorY, colorWidth, colorHeight);
        this.greenRect = new SignEditorUi.Rect(styleInnerX + colorWidth + styleGap, colorY, colorWidth, colorHeight);
        this.blueRect = new SignEditorUi.Rect(styleInnerX + (colorWidth + styleGap) * 2, colorY, colorWidth, colorHeight);

        this.logoRect = new SignEditorUi.Rect(styleInnerX, colorY + colorHeight + (pagedUi() ? 8 : s(2)), styleInnerWidth, pagedUi() ? 24 : s(compactUi() ? 24 : 34));

        int directionWidth = Math.max(s(80), Math.round(styleInnerWidth * 0.30F));
        int directionY = this.logoRect.y() + (pagedUi() ? 30 : s(compactUi() ? 26 : 40));
        this.directionRightRect = new SignEditorUi.Rect(styleInnerX + styleInnerWidth - directionWidth, directionY, directionWidth, pagedUi() ? 20 : s(24));
        this.directionLeftRect = new SignEditorUi.Rect(this.directionRightRect.x() - (pagedUi() ? 6 : s(6)) - directionWidth, directionY, directionWidth, pagedUi() ? 20 : s(24));

    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SignEditorUi.COLOR_OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SignEditorUi.drawModernWindow(
                graphics,
                this.font,
                this.windowX,
                this.windowY,
                this.windowWidth,
                this.windowHeight,
                "Éditeur de panneau D21",
                compactUi() ? "" : "Configuration en direct"
        );

        for (int i = 0; i < this.tabRects.length; i++) {
            D21APanelData panel = i == this.selectedPanelIndex ? currentPanelFromWidgets() : this.panels[i];
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.tabRects[i],
                    "Panneau " + (i + 1),
                    i == this.selectedPanelIndex,
                    true,
                    mouseX,
                    mouseY
            );
            SignEditorUi.drawModernToggle(
                    graphics,
                    this.font,
                    this.tabToggleRects[i],
                    "",
                    "",
                    panel.enabled(),
                    true,
                    mouseX,
                    mouseY
            );
        }

        D21APanelData[] previewPanels = this.panels.clone();
        previewPanels[this.selectedPanelIndex] = currentPanelFromWidgets();
        SignEditorUi.drawD21StackPreview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                previewPanels,
                this.selectedPanelIndex,
                CartoucheType.NONE,
                ""
        );

        if (pagedUi()) {
            SignEditorUi.drawPageTabs(graphics, this.font, this.settingsPageRects, new String[]{"Texte", "Style", "Info"}, this.settingsPage, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 0) {
            SignEditorUi.drawModernSection(graphics, this.font, this.contentRect, "1. CONTENU", pagedUi() ? "" : "Destination et kilométrage");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Destination", this.destinationField.getX(), this.destinationField.getY() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Kilométrage (km)", this.distanceField.getX(), this.distanceField.getY() - s(12));
            }
        }

        if (!pagedUi() || this.settingsPage == 1) {
            SignEditorUi.drawModernSection(graphics, this.font, this.styleRect, "2. STYLE", pagedUi() ? "" : "Couleur, logo autoroute et direction");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Couleur du panneau", this.whiteRect.x(), this.whiteRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc", this.selectedType == D21AType.WHITE, true, mouseX, mouseY);
            SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert", this.selectedType == D21AType.GREEN, true, mouseX, mouseY);
            SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu", this.selectedType == D21AType.BLUE, true, mouseX, mouseY);

            boolean logoAllowed = this.selectedType == D21AType.GREEN || this.selectedType == D21AType.BLUE;
            SignEditorUi.drawModernToggle(graphics, this.font, this.logoRect, "Logo autoroute", compactUi() ? "" : "Afficher le logo autoroute", this.autorouteLogo, logoAllowed, mouseX, mouseY);
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Direction", this.directionLeftRect.x(), this.directionLeftRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.directionLeftRect, "← Gauche", !this.arrowRight, true, mouseX, mouseY);
            SignEditorUi.drawModernButton(graphics, this.font, this.directionRightRect, "Droite →", this.arrowRight, true, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 2) {
            SignEditorUi.drawModernSection(graphics, this.font, this.structureRect, "3. STRUCTURE", pagedUi() ? "" : "Panneau simple à une ligne");
            if (!compactUi()) {
                graphics.text(this.font, Component.literal(SignEditorUi.fitText(this.font, "L'activation se règle directement dans les onglets Panneau 1 à 4.", this.structureRect.width() - Math.max(20, s(20)))), this.structureRect.x() + s(10), this.structureRect.y() + s(42), SignEditorUi.MODERN_MUTED, false);
            }
        }

        if (!compactUi() && !pagedUi()) {
            graphics.text(this.font, Component.literal(SignEditorUi.fitText(this.font, "Conseil : utilisez Panneau 1 à 4 ; le bouton à droite de chaque onglet active ou désactive l’étage.", this.windowWidth - Math.max(36, s(36)))), this.windowX + s(18), this.windowY + this.windowHeight - s(28), SignEditorUi.MODERN_MUTED, false);
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();

            for (int i = 0; i < this.tabRects.length; i++) {
                if (this.tabToggleRects[i].contains(x, y)) {
                    togglePanelEnabled(i);
                    return true;
                }
                if (this.tabRects[i].contains(x, y)) {
                    selectPanel(i);
                    return true;
                }
            }
            if (pagedUi()) {
                for (int i = 0; i < this.settingsPageRects.length; i++) {
                    if (this.settingsPageRects[i] != null && this.settingsPageRects[i].contains(x, y)) {
                        this.settingsPage = i;
                        updatePagedVisibility();
                        return true;
                    }
                }
            }
            if (!pagedUi() || this.settingsPage == 1) {
                if (this.whiteRect.contains(x, y)) {
                    selectType(D21AType.WHITE);
                    return true;
                }
                if (this.greenRect.contains(x, y)) {
                    selectType(D21AType.GREEN);
                    return true;
                }
                if (this.blueRect.contains(x, y)) {
                    selectType(D21AType.BLUE);
                    return true;
                }
                if (this.logoRect.contains(x, y) && (this.selectedType == D21AType.GREEN || this.selectedType == D21AType.BLUE)) {
                    this.autorouteLogo = !this.autorouteLogo;
                    return true;
                }
                if (this.directionLeftRect.contains(x, y)) {
                    this.arrowRight = false;
                    return true;
                }
                if (this.directionRightRect.contains(x, y)) {
                    this.arrowRight = true;
                    return true;
                }
            }
            if (this.applyRect.contains(x, y)) {
                save();
                return true;
            }
            if (this.cancelRect.contains(x, y)) {
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void togglePanelEnabled(int index) {
        if (index < 0 || index >= D21ABlockEntity.MAX_PANELS) {
            return;
        }

        if (index == this.selectedPanelIndex) {
            this.panelEnabled = !this.panelEnabled;
            return;
        }

        D21APanelData panel = this.panels[index];
        this.panels[index] = new D21APanelData(
                !panel.enabled(),
                panel.line1(),
                panel.line2(),
                panel.distance1(),
                panel.distance2(),
                panel.type(),
                panel.arrowRight(),
                panel.autorouteLogo(),
                panel.doubleLine(),
                panel.line1Font(),
                panel.line2Font()
        );
    }

    private void selectPanel(int newIndex) {
        if (newIndex < 0 || newIndex >= D21ABlockEntity.MAX_PANELS || newIndex == this.selectedPanelIndex) {
            return;
        }
        storeSelectedPanelFromWidgets();
        this.selectedPanelIndex = newIndex;
        loadSelectedPanelIntoWidgets();
        updatePagedVisibility();
        this.setInitialFocus(this.destinationField);
    }

    private void selectType(D21AType type) {
        this.selectedType = type == null ? D21AType.WHITE : type;
        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }
    }

    private D21APanelData currentPanelFromWidgets() {
        if (this.destinationField == null || this.distanceField == null) {
            return this.panels[this.selectedPanelIndex];
        }
        return new D21APanelData(
                this.panelEnabled,
                this.destinationField.getValue(),
                "",
                this.distanceField.getValue(),
                "",
                this.selectedType,
                this.arrowRight,
                this.autorouteLogo
        );
    }

    private void storeSelectedPanelFromWidgets() {
        this.panels[this.selectedPanelIndex] = currentPanelFromWidgets();
    }

    private void loadSelectedPanelIntoWidgets() {
        D21APanelData panel = this.panels[this.selectedPanelIndex];
        this.panelEnabled = panel.enabled();
        this.selectedType = panel.type();
        this.arrowRight = panel.arrowRight();
        this.autorouteLogo = panel.autorouteLogo();
        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }
        this.destinationField.setValue(panel.line1());
        this.distanceField.setValue(panel.distance());
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private boolean pagedUi() {
        return SignEditorUi.pagedCompactMode(this.scale, this.windowHeight);
    }

    private void updatePagedVisibility() {
        boolean visible = !pagedUi() || this.settingsPage == 0;
        this.destinationField.visible = visible;
        this.destinationField.active = visible;
        this.distanceField.visible = visible;
        this.distanceField.active = visible;
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        storeSelectedPanelFromWidgets();
        ClientPacketDistributor.sendToServer(
                new UpdateD21APayload(
                        this.blockPos,
                        this.panels[0],
                        this.panels[1],
                        this.panels[2],
                        this.panels[3]
                )
        );
        this.onClose();
    }
}
