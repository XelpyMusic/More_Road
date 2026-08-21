package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;
import net.xelpy.moreroad.network.UpdateD61APayload;

public class D61AEditScreen extends Screen {

    private final BlockPos blockPos;
    private final D61APanelData[] panels = new D61APanelData[D61ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex;
    private boolean panelEnabled = true;
    private boolean doubleLine;
    private D21AType selectedType = D21AType.WHITE;
    private boolean autorouteLogo;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private boolean arrowEnabled;
    private D61AArrowPosition arrowPosition = D61AArrowPosition.RIGHT;
    private D61AArrowDirection arrowDirection = D61AArrowDirection.UP;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;
    private EditBox cartoucheTextField;

    private final SignEditorUi.Rect[] tabRects = new SignEditorUi.Rect[D61ABlockEntity.MAX_PANELS];
    private final SignEditorUi.Rect[] tabToggleRects = new SignEditorUi.Rect[D61ABlockEntity.MAX_PANELS];
    private final SignEditorUi.Rect[] directionRects = new SignEditorUi.Rect[D61AArrowDirection.values().length];

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect styleRect;
    private SignEditorUi.Rect arrowRect;
    private SignEditorUi.Rect cartoucheRect;
    private SignEditorUi.Rect structureRect;

    private SignEditorUi.Rect line1FontRect;
    private SignEditorUi.Rect line2FontRect;
    private SignEditorUi.Rect whiteRect;
    private SignEditorUi.Rect greenRect;
    private SignEditorUi.Rect blueRect;
    private SignEditorUi.Rect logoRect;
    private SignEditorUi.Rect arrowEnabledRect;
    private SignEditorUi.Rect arrowLeftRect;
    private SignEditorUi.Rect arrowRightRect;
    private SignEditorUi.Rect cartoucheTypeRect;
    private SignEditorUi.Rect simpleRect;
    private SignEditorUi.Rect doubleRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private final SignEditorUi.Rect[] settingsPageRects = new SignEditorUi.Rect[5];
    private int settingsPage = 0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public D61AEditScreen(
            BlockPos blockPos,
            D61APanelData[] currentPanels,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(Component.literal("Éditeur de panneau D61"));
        this.blockPos = blockPos.immutable();
        this.cartoucheType = currentCartoucheType == null ? CartoucheType.NONE : currentCartoucheType;
        this.cartoucheText = currentCartoucheText == null ? "" : currentCartoucheText;

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            D61APanelData panel = currentPanels != null && i < currentPanels.length ? currentPanels[i] : null;
            if (panel == null) {
                panel = i == 0 ? D61APanelData.firstPanelDefault() : D61APanelData.disabled();
            }
            this.panels[i] = panel;
        }
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(6, Math.min(22, this.width / 36));
        int marginY = Math.max(6, Math.min(16, this.height / 36));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1240.0F, 860.0F);

        boolean compact = compactUi();
        boolean tight = tightCompactUi();

        int pad = s(tight ? 12 : compact ? 14 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsH = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 10 : 12);
        int sectionGap = s(tight ? 6 : 8);
        int bodyY = this.windowY + header + tabsH;
        int bodyH = this.windowHeight - header - tabsH - footer;

        int tabsGap = s(8);
        int tabsW = this.windowWidth - pad * 2;
        int tabW = (tabsW - tabsGap * 3) / 4;
        int tabY = this.windowY + header;
        for (int i = 0; i < this.tabRects.length; i++) {
            int tx = this.windowX + pad + i * (tabW + tabsGap);
            this.tabRects[i] = new SignEditorUi.Rect(tx, tabY, tabW, s(32));
            this.tabToggleRects[i] = new SignEditorUi.Rect(tx + tabW - s(34), tabY + s(7), s(28), s(18));
        }

        int leftW = Math.max(s(310), Math.round((this.windowWidth - pad * 2 - gap) * 0.44F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, Math.max(s(160), bodyH - s(tight ? 4 : 8)));

        if (pagedUi()) {
            int pageGap = 4;
            int pageHeight = 22;
            SignEditorUi.Rect pageBar = new SignEditorUi.Rect(rightX, bodyY, rightW, pageHeight);
            SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, this.settingsPageRects.length, pageHeight, pageGap);
            System.arraycopy(pages, 0, this.settingsPageRects, 0, this.settingsPageRects.length);

            int cardY = bodyY + pageHeight + 6;
            int cardHeight = Math.max(96, bodyH - pageHeight - 10);
            SignEditorUi.Rect fullCard = new SignEditorUi.Rect(rightX, cardY, rightW, cardHeight);
            this.contentRect = fullCard;
            this.styleRect = fullCard;
            this.arrowRect = fullCard;
            this.cartoucheRect = fullCard;
            this.structureRect = fullCard;
        } else {
            int available = Math.max(180, bodyH - (tight ? s(2) : s(8)));
            int[] sectionHeights = SignEditorUi.fitSections(
                    available,
                    sectionGap,
                    new float[]{0.24F, 0.18F, 0.25F, 0.15F, 0.18F},
                    new int[]{
                            s(tight ? 82 : compact ? 102 : 132),
                            s(tight ? 66 : compact ? 82 : 100),
                            s(tight ? 86 : compact ? 104 : 130),
                            s(tight ? 58 : compact ? 70 : 88),
                            s(tight ? 46 : compact ? 54 : 70)
                    }
            );
            int contentH = sectionHeights[0];
            int styleH = sectionHeights[1];
            int arrowH = sectionHeights[2];
            int cartoucheH = sectionHeights[3];
            int structureH = sectionHeights[4];

            int y = bodyY;
            this.contentRect = new SignEditorUi.Rect(rightX, y, rightW, contentH);
            y += contentH + sectionGap;
            this.styleRect = new SignEditorUi.Rect(rightX, y, rightW, styleH);
            y += styleH + sectionGap;
            this.arrowRect = new SignEditorUi.Rect(rightX, y, rightW, arrowH);
            y += arrowH + sectionGap;
            this.cartoucheRect = new SignEditorUi.Rect(rightX, y, rightW, cartoucheH);
            y += cartoucheH + sectionGap;
            this.structureRect = new SignEditorUi.Rect(rightX, y, rightW, structureH);
        }

        initContentControls();
        initStyleControls();
        initArrowControls();
        initCartoucheControls();
        initStructureControls();

        int actionY = this.windowY + this.windowHeight - s(36);
        int actionW = s(145);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, s(28));
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, s(28));

        loadSelectedPanelIntoWidgets();
        updateFieldVisibility();
        updateCartoucheFieldState();
        updatePagedVisibility();
        this.setInitialFocus(this.line1Field);
    }

    private void initContentControls() {
        int innerX = this.contentRect.x() + s(10);
        int innerW = this.contentRect.width() - s(20);
        int fieldH = pagedUi() ? 20 : s(22);
        int gap = pagedUi() ? 7 : s(7);
        int fontW = Math.max(s(86), Math.round(innerW * 0.22F));
        int distanceW = Math.max(s(60), Math.round(innerW * 0.16F));
        int textW = innerW - fontW - distanceW - gap * 2;
        int row1Y = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 36 : 46));
        int row2Y = row1Y + fieldH + (pagedUi() ? 8 : s(compactUi() ? 8 : 17));

        this.line1Field = new EditBox(this.font, innerX, row1Y, textW, fieldH, Component.literal("Destination 1"));
        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);
        this.line1FontRect = new SignEditorUi.Rect(innerX + textW + gap, row1Y, fontW, fieldH);
        this.distance1Field = new EditBox(this.font, this.line1FontRect.x() + fontW + gap, row1Y, distanceW, fieldH, Component.literal("Km 1"));
        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        this.line2Field = new EditBox(this.font, innerX, row2Y, textW, fieldH, Component.literal("Destination 2"));
        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);
        this.line2FontRect = new SignEditorUi.Rect(innerX + textW + gap, row2Y, fontW, fieldH);
        this.distance2Field = new EditBox(this.font, this.line2FontRect.x() + fontW + gap, row2Y, distanceW, fieldH, Component.literal("Km 2"));
        this.distance2Field.setMaxLength(8);
        this.addRenderableWidget(this.distance2Field);
    }

    private void initStyleControls() {
        int innerX = this.styleRect.x() + s(10);
        int innerW = this.styleRect.width() - s(20);
        int gap = pagedUi() ? 8 : s(8);
        int controlH = pagedUi() ? 20 : s(23);
        int colorY = this.styleRect.y() + (pagedUi() ? 34 : s(compactUi() ? 34 : 43));
        int colorW = (innerW - gap * 2) / 3;
        this.whiteRect = new SignEditorUi.Rect(innerX, colorY, colorW, controlH);
        this.greenRect = new SignEditorUi.Rect(innerX + colorW + gap, colorY, colorW, controlH);
        this.blueRect = new SignEditorUi.Rect(innerX + (colorW + gap) * 2, colorY, colorW, controlH);
        this.logoRect = new SignEditorUi.Rect(innerX, colorY + controlH + (pagedUi() ? 8 : s(6)), innerW, pagedUi() ? 24 : s(compactUi() ? 26 : 30));
    }

    private void initArrowControls() {
        int innerX = this.arrowRect.x() + s(10);
        int innerW = this.arrowRect.width() - s(20);
        int topY = this.arrowRect.y() + (pagedUi() ? 32 : s(compactUi() ? 30 : 38));
        int toggleH = pagedUi() ? 24 : s(compactUi() ? 26 : 30);
        this.arrowEnabledRect = new SignEditorUi.Rect(innerX, topY, innerW, toggleH);

        int posY = topY + toggleH + (pagedUi() ? 7 : s(3));
        int posW = Math.max(s(88), Math.round(innerW * 0.26F));
        int posH = pagedUi() ? 20 : s(22);
        int posGap = pagedUi() ? 7 : s(7);
        this.arrowLeftRect = new SignEditorUi.Rect(innerX, posY, posW, posH);
        this.arrowRightRect = new SignEditorUi.Rect(innerX + posW + posGap, posY, posW, posH);

        int dirsY = posY + posH + (pagedUi() ? 7 : s(7));
        int dirGap = pagedUi() ? 5 : s(5);
        int dirW = (innerW - dirGap * 7) / 8;
        int dirH = pagedUi() ? 20 : s(compactUi() ? 22 : 24);
        for (int i = 0; i < this.directionRects.length; i++) {
            this.directionRects[i] = new SignEditorUi.Rect(innerX + i * (dirW + dirGap), dirsY, dirW, dirH);
        }
    }

    private void initCartoucheControls() {
        int innerX = this.cartoucheRect.x() + s(10);
        int innerW = this.cartoucheRect.width() - s(20);
        int y = this.cartoucheRect.y() + (pagedUi() ? 34 : s(compactUi() ? 34 : 44));
        int gap = pagedUi() ? 8 : s(8);
        int typeW = Math.max(s(125), Math.round(innerW * 0.40F));
        int controlH = pagedUi() ? 20 : s(23);
        this.cartoucheTypeRect = new SignEditorUi.Rect(innerX, y, typeW, controlH);
        this.cartoucheTextField = new EditBox(this.font, innerX + typeW + gap, y, innerW - typeW - gap, controlH, Component.literal("Texte cartouche"));
        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(this.cartoucheText);
        this.addRenderableWidget(this.cartoucheTextField);
    }

    private void initStructureControls() {
        int innerX = this.structureRect.x() + s(10);
        int innerW = this.structureRect.width() - s(20);
        int y = this.structureRect.y() + (pagedUi() ? 34 : tightCompactUi() ? s(18) : compactUi() ? s(24) : s(42));
        int gap = pagedUi() ? 8 : s(8);
        int w = (innerW - gap) / 2;
        int controlH = pagedUi() ? 20 : s(23);
        this.simpleRect = new SignEditorUi.Rect(innerX, y, w, controlH);
        this.doubleRect = new SignEditorUi.Rect(innerX + w + gap, y, w, controlH);
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
                "D61",
                "Éditeur de panneau D61",
                compactUi() ? "" : "Configuration en direct"
        );

        for (int i = 0; i < this.tabRects.length; i++) {
            D61APanelData panel = i == this.selectedPanelIndex ? currentPanelFromWidgets() : this.panels[i];
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.tabRects[i],
                    compactUi() ? "Panneau " + (i + 1) : "Panneau " + (i + 1) + "  •  " + (panel.doubleLine() ? "2 lignes" : "1 ligne"),
                    i == this.selectedPanelIndex,
                    true,
                    mouseX,
                    mouseY
            );
            SignEditorUi.drawModernToggle(graphics, this.font, this.tabToggleRects[i], "", "", panel.enabled(), true, mouseX, mouseY);
        }

        SignEditorUi.drawD61StackPreview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                previewPanels(),
                this.selectedPanelIndex,
                this.cartoucheType,
                this.cartoucheTextField.getValue()
        );

        if (pagedUi()) {
            SignEditorUi.drawPageTabs(
                    graphics,
                    this.font,
                    this.settingsPageRects,
                    new String[]{"Texte", "Style", "Flèche", "Cart.", "Format"},
                    this.settingsPage,
                    mouseX,
                    mouseY
            );
            switch (this.settingsPage) {
                case 0 -> drawContent(graphics, mouseX, mouseY);
                case 1 -> drawStyle(graphics, mouseX, mouseY);
                case 2 -> drawArrow(graphics, mouseX, mouseY);
                case 3 -> drawCartouche(graphics, mouseX, mouseY);
                default -> drawStructure(graphics, mouseX, mouseY);
            }
        } else {
            drawContent(graphics, mouseX, mouseY);
            drawStyle(graphics, mouseX, mouseY);
            drawArrow(graphics, mouseX, mouseY);
            drawCartouche(graphics, mouseX, mouseY);
            drawStructure(graphics, mouseX, mouseY);
        }

        if (!compactUi()) {
            graphics.text(
                    this.font,
                    Component.literal("Conseil : l'activation de chaque panneau se règle directement dans les onglets du haut."),
                    this.windowX + s(18),
                    this.windowY + this.windowHeight - s(28),
                    SignEditorUi.MODERN_MUTED,
                    false
            );
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.contentRect, "1. CONTENU", pagedUi() ? "" : "Destinations, police et kilométrage");
        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Destination 1", this.line1Field.getX(), this.line1Field.getY() - s(11));
            SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line1FontRect.x(), this.line1FontRect.y() - s(11));
            SignEditorUi.drawFieldLabel(graphics, this.font, "Km", this.distance1Field.getX(), this.distance1Field.getY() - s(11));
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.line1FontRect, SignEditorUi.fontLabel(this.line1Font), false, true, mouseX, mouseY);
        if (this.doubleLine) {
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Destination 2", this.line2Field.getX(), this.line2Field.getY() - s(11));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line2FontRect.x(), this.line2FontRect.y() - s(11));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Km", this.distance2Field.getX(), this.distance2Field.getY() - s(11));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.line2FontRect, SignEditorUi.fontLabel(this.line2Font), false, true, mouseX, mouseY);
        }
    }

    private void drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.styleRect, "2. STYLE", pagedUi() ? "" : "Couleur du panneau et logo autoroute");
        SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc", this.selectedType == D21AType.WHITE, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert", this.selectedType == D21AType.GREEN, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu", this.selectedType == D21AType.BLUE, true, mouseX, mouseY);
        boolean logoAllowed = this.selectedType != D21AType.WHITE;
        SignEditorUi.drawModernToggle(graphics, this.font, this.logoRect, "Logo autoroute", compactUi() ? "" : "Afficher le pictogramme autoroute", this.autorouteLogo, logoAllowed, mouseX, mouseY);
    }

    private void drawArrow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.arrowRect, "3. FLÈCHE", pagedUi() ? "" : "Activation, position et direction");
        SignEditorUi.drawModernToggle(graphics, this.font, this.arrowEnabledRect, "Flèche", compactUi() ? "" : "Afficher une flèche directionnelle", this.arrowEnabled, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.arrowLeftRect, "À gauche", this.arrowPosition == D61AArrowPosition.LEFT, this.arrowEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.arrowRightRect, "À droite", this.arrowPosition == D61AArrowPosition.RIGHT, this.arrowEnabled, mouseX, mouseY);
        D61AArrowDirection[] values = D61AArrowDirection.values();
        for (int i = 0; i < values.length; i++) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.directionRects[i],
                    values[i].symbol(),
                    values[i] == this.arrowDirection,
                    this.arrowEnabled,
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawCartouche(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.cartoucheRect, "4. CARTOUCHE", pagedUi() ? "" : "Couleur et texte du cartouche");
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.cartoucheTypeRect,
                SignEditorUi.cartoucheLabel(this.cartoucheType),
                this.cartoucheType.isVisible(),
                true,
                mouseX,
                mouseY
        );
    }

    private void drawStructure(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.structureRect, "5. STRUCTURE", pagedUi() ? "" : "Format du panneau sélectionné");
        SignEditorUi.drawModernButton(graphics, this.font, this.simpleRect, "Simple (1 ligne)", !this.doubleLine, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.doubleRect, "Double (2 lignes)", this.doubleLine, true, mouseX, mouseY);
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

            if ((!pagedUi() || this.settingsPage == 0) && this.line1FontRect.contains(x, y)) {
                this.line1Font = this.line1Font.next();
                return true;
            }
            if ((!pagedUi() || this.settingsPage == 0) && this.doubleLine && this.line2FontRect.contains(x, y)) {
                this.line2Font = this.line2Font.next();
                return true;
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
                if (this.logoRect.contains(x, y) && this.selectedType != D21AType.WHITE) {
                    this.autorouteLogo = !this.autorouteLogo;
                    return true;
                }
            }
            if (!pagedUi() || this.settingsPage == 2) {
                if (this.arrowEnabledRect.contains(x, y)) {
                    this.arrowEnabled = !this.arrowEnabled;
                    return true;
                }
                if (this.arrowEnabled && this.arrowLeftRect.contains(x, y)) {
                    this.arrowPosition = D61AArrowPosition.LEFT;
                    return true;
                }
                if (this.arrowEnabled && this.arrowRightRect.contains(x, y)) {
                    this.arrowPosition = D61AArrowPosition.RIGHT;
                    return true;
                }
                if (this.arrowEnabled) {
                    D61AArrowDirection[] values = D61AArrowDirection.values();
                    for (int i = 0; i < this.directionRects.length; i++) {
                        if (this.directionRects[i].contains(x, y)) {
                            this.arrowDirection = values[i];
                            return true;
                        }
                    }
                }
            }
            if ((!pagedUi() || this.settingsPage == 3) && this.cartoucheTypeRect.contains(x, y)) {
                this.cartoucheType = this.cartoucheType.next();
                updateCartoucheFieldState();
                return true;
            }
            if (!pagedUi() || this.settingsPage == 4) {
                if (this.simpleRect.contains(x, y)) {
                    this.doubleLine = false;
                    updateFieldVisibility();
                    return true;
                }
                if (this.doubleRect.contains(x, y)) {
                    this.doubleLine = true;
                    updateFieldVisibility();
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

    private D61APanelData[] previewPanels() {
        D61APanelData[] result = this.panels.clone();
        result[this.selectedPanelIndex] = currentPanelFromWidgets();
        return result;
    }

    private D61APanelData currentPanelFromWidgets() {
        return new D61APanelData(
                this.panelEnabled,
                this.line1Field == null ? "" : this.line1Field.getValue(),
                this.line2Field == null ? "" : this.line2Field.getValue(),
                this.distance1Field == null ? "" : this.distance1Field.getValue(),
                this.distance2Field == null ? "" : this.distance2Field.getValue(),
                sanitizeType(this.selectedType),
                this.doubleLine,
                this.arrowEnabled,
                this.arrowPosition,
                this.arrowDirection,
                this.autorouteLogo,
                this.line1Font,
                this.line2Font
        );
    }

    private void togglePanelEnabled(int index) {
        if (index < 0 || index >= this.panels.length) {
            return;
        }
        if (index == this.selectedPanelIndex) {
            this.panelEnabled = !this.panelEnabled;
            return;
        }
        D61APanelData p = this.panels[index];
        this.panels[index] = new D61APanelData(
                !p.enabled(), p.line1(), p.line2(), p.distance1(), p.distance2(), p.type(), p.doubleLine(),
                p.arrowEnabled(), p.arrowPosition(), p.arrowDirection(), p.autorouteLogo(), p.line1Font(), p.line2Font()
        );
    }

    private void selectPanel(int newIndex) {
        if (newIndex < 0 || newIndex >= D61ABlockEntity.MAX_PANELS || newIndex == this.selectedPanelIndex) {
            return;
        }
        storeSelectedPanelFromWidgets();
        this.selectedPanelIndex = newIndex;
        loadSelectedPanelIntoWidgets();
        this.setInitialFocus(this.line1Field);
    }

    private void selectType(D21AType type) {
        this.selectedType = sanitizeType(type);
        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }
    }

    private void storeSelectedPanelFromWidgets() {
        this.panels[this.selectedPanelIndex] = currentPanelFromWidgets();
    }

    private void loadSelectedPanelIntoWidgets() {
        D61APanelData panel = this.panels[this.selectedPanelIndex];
        this.panelEnabled = panel.enabled();
        this.doubleLine = panel.doubleLine();
        this.selectedType = sanitizeType(panel.type());
        this.autorouteLogo = panel.autorouteLogo();
        this.line1Font = panel.line1Font();
        this.line2Font = panel.line2Font();
        this.arrowEnabled = panel.arrowEnabled();
        this.arrowPosition = panel.arrowPosition();
        this.arrowDirection = panel.arrowDirection();
        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }
        this.line1Field.setValue(panel.line1());
        this.line2Field.setValue(panel.line2());
        this.distance1Field.setValue(panel.distance1());
        this.distance2Field.setValue(panel.distance2());
        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        boolean contentVisible = !pagedUi() || this.settingsPage == 0;
        this.line1Field.visible = contentVisible;
        this.line1Field.active = contentVisible;
        this.distance1Field.visible = contentVisible;
        this.distance1Field.active = contentVisible;

        boolean line2Visible = contentVisible && this.doubleLine;
        this.line2Field.visible = line2Visible;
        this.line2Field.active = line2Visible;
        this.distance2Field.visible = line2Visible;
        this.distance2Field.active = line2Visible;
    }

    private void updateCartoucheFieldState() {
        if (this.cartoucheTextField != null) {
            boolean visible = !pagedUi() || this.settingsPage == 3;
            this.cartoucheTextField.visible = visible;
            this.cartoucheTextField.active = visible && this.cartoucheType.isVisible();
        }
    }

    private void updatePagedVisibility() {
        updateFieldVisibility();
        updateCartoucheFieldState();
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private boolean tightCompactUi() {
        return SignEditorUi.tightForScale(this.scale, this.windowHeight);
    }

    private boolean pagedUi() {
        return SignEditorUi.pagedCompactMode(this.scale, this.windowHeight);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        storeSelectedPanelFromWidgets();
        ClientPacketDistributor.sendToServer(
                new UpdateD61APayload(
                        this.blockPos,
                        this.panels[0],
                        this.panels[1],
                        this.panels[2],
                        this.panels[3],
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );
        this.onClose();
    }

    private static D21AType sanitizeType(D21AType type) {
        if (type == D21AType.GREEN) {
            return D21AType.GREEN;
        }
        if (type == D21AType.BLUE) {
            return D21AType.BLUE;
        }
        return D21AType.WHITE;
    }
}
