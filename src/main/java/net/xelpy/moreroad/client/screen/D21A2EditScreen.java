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
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.network.UpdateD21APayload;

/**
 * Éditeur moderne D21 / D21A2.
 *
 * V105 : refonte visuelle complète basée sur la maquette validée :
 * - fenêtre sombre occupant l'écran sans empilement de boutons vanilla ;
 * - aperçu large à gauche ;
 * - réglages regroupés à droite en quatre cartes ;
 * - sélecteurs P1 à P4 en haut ;
 * - contrôles personnalisés rendus directement par le Screen ;
 * - mise en page proportionnelle pour rester utilisable avec les GUI Scale élevées.
 */
public class D21A2EditScreen extends Screen {

    private final BlockPos blockPos;
    private final D21APanelData[] panels = new D21APanelData[D21ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex;
    private boolean panelEnabled = true;
    private boolean doubleLine;
    private D21AType selectedType = D21AType.WHITE;
    private boolean arrowRight;
    private boolean autorouteLogo;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private boolean line1Spacing;
    private boolean line2Spacing;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;
    private EditBox cartoucheTextField;

    private final SignEditorUi.Rect[] tabRects = new SignEditorUi.Rect[D21ABlockEntity.MAX_PANELS];
    private final SignEditorUi.Rect[] tabToggleRects = new SignEditorUi.Rect[D21ABlockEntity.MAX_PANELS];
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect styleRect;
    private SignEditorUi.Rect cartoucheRect;
    private SignEditorUi.Rect structureRect;

    private SignEditorUi.Rect line1FontRect;
    private SignEditorUi.Rect line2FontRect;
    private SignEditorUi.Rect line1SpacingRect;
    private SignEditorUi.Rect line2SpacingRect;
    private SignEditorUi.Rect whiteRect;
    private SignEditorUi.Rect greenRect;
    private SignEditorUi.Rect blueRect;
    private SignEditorUi.Rect logoRect;
    private SignEditorUi.Rect directionLeftRect;
    private SignEditorUi.Rect directionRightRect;
    private SignEditorUi.Rect cartoucheTypeRect;
    private SignEditorUi.Rect enabledRect;
    private SignEditorUi.Rect simpleRect;
    private SignEditorUi.Rect doubleRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private final SignEditorUi.Rect[] settingsPageRects = new SignEditorUi.Rect[4];
    private int settingsPage = 0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public D21A2EditScreen(
            BlockPos blockPos,
            D21APanelData[] currentPanels,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(Component.literal("Éditeur de panneau D21"));
        this.blockPos = blockPos.immutable();
        this.cartoucheType = currentCartoucheType == null ? CartoucheType.NONE : currentCartoucheType;
        this.cartoucheText = currentCartoucheText == null ? "" : currentCartoucheText;

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

        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1120.0F, 740.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int sectionGap = s(ultraTight ? 5 : tight ? 7 : 12);
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

        int tabY = this.windowY + headerHeight;
        int tabGap = s(8);
        int tabsTotalWidth = this.windowWidth - pad * 2;
        int tabWidth = Math.max(s(120), (tabsTotalWidth - tabGap * 3) / 4);
        for (int i = 0; i < this.tabRects.length; i++) {
            int tx = this.windowX + pad + i * (tabWidth + tabGap);
            this.tabRects[i] = new SignEditorUi.Rect(tx, tabY, tabWidth, SignEditorUi.safeControlHeight(this.font, s(32)));
            this.tabToggleRects[i] = SignEditorUi.tabToggleRect(
                    this.tabRects[i],
                    this.font
            );
        }

        this.previewRect = new SignEditorUi.Rect(leftX, bodyTop, leftWidth, bodyHeight - s(8));

        if (pagedUi()) {
            int pageHeight = 22;
            SignEditorUi.Rect pageBar = new SignEditorUi.Rect(rightX, bodyTop, rightWidth, pageHeight);
            SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, this.settingsPageRects.length, pageHeight, 4);
            System.arraycopy(pages, 0, this.settingsPageRects, 0, this.settingsPageRects.length);

            int cardY = bodyTop + pageHeight + 6;
            int cardHeight = Math.max(96, bodyHeight - pageHeight - 10);
            SignEditorUi.Rect fullCard = new SignEditorUi.Rect(rightX, cardY, rightWidth, cardHeight);
            this.contentRect = fullCard;
            this.styleRect = fullCard;
            this.cartoucheRect = fullCard;
            this.structureRect = fullCard;
        } else {
            int availableRightHeight = Math.max(140, bodyHeight - s(ultraTight ? 2 : 8));
            int[] sectionHeights = SignEditorUi.fitSections(
                    availableRightHeight,
                    sectionGap,
                    new float[]{0.23F, 0.30F, 0.22F, 0.25F},
                    new int[]{
                            s(ultraTight ? 58 : tight ? 72 : 92),
                            s(ultraTight ? 78 : tight ? 96 : 122),
                            s(ultraTight ? 58 : tight ? 72 : 92),
                            s(ultraTight ? 54 : tight ? 66 : 86)
                    }
            );

            int y = bodyTop;
            this.contentRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[0]);
            y += sectionHeights[0] + sectionGap;
            this.styleRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[1]);
            y += sectionHeights[1] + sectionGap;
            this.cartoucheRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[2]);
            y += sectionHeights[2] + sectionGap;
            this.structureRect = new SignEditorUi.Rect(rightX, y, rightWidth, sectionHeights[3]);
        }

        initContentFields();
        initCustomControls();

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionWidth = Math.max(74, Math.max(s(150), Math.max(this.font.width("✓  Appliquer"), this.font.width("×  Annuler")) + 18));
        int actionGap = Math.max(4, s(10));
        int actionY = this.windowY + this.windowHeight - Math.max(s(38), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - actionWidth,
                actionY,
                actionWidth,
                actionH
        );
        this.applyRect = new SignEditorUi.Rect(
                this.cancelRect.x() - actionGap - actionWidth,
                actionY,
                actionWidth,
                actionH
        );

        loadSelectedPanelIntoWidgets();
        updateFieldVisibility();
        updatePagedVisibility();
        this.setInitialFocus(this.line1Field);
    }

    private void initContentFields() {
        int innerX = this.contentRect.x() + s(10);
        int innerWidth = this.contentRect.width() - s(20);
        int fieldY = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 28 : 42));
        int fieldHeight = pagedUi() ? 20 : s(22);
        int gap = pagedUi() ? 8 : s(8);

        int distanceWidth = Math.max(s(58), Math.round(innerWidth * 0.15F));
        int fontWidth = Math.max(s(88), Math.round(innerWidth * 0.25F));
        int spacingWidth = Math.max(s(50), Math.round(innerWidth * 0.10F));
        int destinationWidth = innerWidth - distanceWidth - fontWidth - spacingWidth - gap * 2;

        this.line1Field = new EditBox(
                this.font,
                innerX,
                fieldY,
                destinationWidth,
                fieldHeight,
                Component.literal("Destination")
        );
        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);

        this.line1FontRect = new SignEditorUi.Rect(innerX + destinationWidth + gap, fieldY, fontWidth, fieldHeight);
        this.line1SpacingRect = new SignEditorUi.Rect(innerX + destinationWidth + line1FontRect.width() + gap*2, fieldY, spacingWidth, fieldHeight);

        this.distance1Field = new EditBox(
                this.font,
                this.line1SpacingRect.x() + this.line1SpacingRect.width() + gap,
                fieldY,
                distanceWidth,
                fieldHeight,
                Component.literal("Kilométrage")
        );
        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        int line2Y = fieldY + fieldHeight + (pagedUi() ? 8 : s(6));
        this.line2Field = new EditBox(
                this.font,
                innerX,
                line2Y,
                destinationWidth,
                fieldHeight,
                Component.literal("Destination ligne 2")
        );
        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);

        this.line2FontRect = new SignEditorUi.Rect(innerX + destinationWidth + gap, line2Y, fontWidth, fieldHeight);
        this.line2SpacingRect = new SignEditorUi.Rect(innerX + destinationWidth + line2FontRect.width() + gap*2, line2Y, spacingWidth, fieldHeight);

        this.distance2Field = new EditBox(
                this.font,
                this.line2SpacingRect.x() + this.line2SpacingRect.width() + gap,
                line2Y,
                distanceWidth,
                fieldHeight,
                Component.literal("Kilométrage ligne 2")
        );
        this.distance2Field.setMaxLength(8);
        this.addRenderableWidget(this.distance2Field);
    }

    private void initCustomControls() {
        int styleInnerX = this.styleRect.x() + s(10);
        int styleInnerWidth = this.styleRect.width() - s(20);
        int styleGap = pagedUi() ? 8 : s(8);
        int colorY = this.styleRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 48));
        int colorHeight = pagedUi() ? 20 : s(24);
        int colorWidth = (styleInnerWidth - styleGap * 2) / 3;

        this.whiteRect = new SignEditorUi.Rect(styleInnerX, colorY, colorWidth, colorHeight);
        this.greenRect = new SignEditorUi.Rect(styleInnerX + colorWidth + styleGap, colorY, colorWidth, colorHeight);
        this.blueRect = new SignEditorUi.Rect(styleInnerX + (colorWidth + styleGap) * 2, colorY, colorWidth, colorHeight);

        int logoY = colorY + colorHeight + (pagedUi() ? 8 : s(compactUi() ? 5 : 8));
        this.logoRect = new SignEditorUi.Rect(styleInnerX, logoY, styleInnerWidth, pagedUi() ? 24 : s(compactUi() ? 24 : 34));

        int directionY = logoY + (pagedUi() ? 30 : s(compactUi() ? 27 : 38));
        int directionWidth = Math.max(s(88), Math.round(styleInnerWidth * 0.30F));
        this.directionRightRect = new SignEditorUi.Rect(
                styleInnerX + styleInnerWidth - directionWidth,
                directionY,
                directionWidth,
                pagedUi() ? 20 : s(24)
        );
        this.directionLeftRect = new SignEditorUi.Rect(
                this.directionRightRect.x() - (pagedUi() ? 6 : s(6)) - directionWidth,
                directionY,
                directionWidth,
                pagedUi() ? 20 : s(24)
        );

        int cartoucheInnerX = this.cartoucheRect.x() + s(10);
        int cartoucheInnerWidth = this.cartoucheRect.width() - s(20);
        int cartoucheFieldY = this.cartoucheRect.y() + (pagedUi() ? 34 : s(compactUi() ? 28 : 44));
        int cartoucheGap = pagedUi() ? 8 : s(8);
        int typeWidth = Math.max(s(145), Math.round(cartoucheInnerWidth * 0.48F));
        this.cartoucheTypeRect = new SignEditorUi.Rect(
                cartoucheInnerX,
                cartoucheFieldY,
                typeWidth,
                pagedUi() ? 20 : s(24)
        );

        this.cartoucheTextField = new EditBox(
                this.font,
                cartoucheInnerX + typeWidth + cartoucheGap,
                cartoucheFieldY,
                cartoucheInnerWidth - typeWidth - cartoucheGap,
                pagedUi() ? 20 : s(24),
                Component.literal("Texte du cartouche")
        );
        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(this.cartoucheText);
        this.addRenderableWidget(this.cartoucheTextField);

        int structureInnerX = this.structureRect.x() + s(10);
        int structureInnerWidth = this.structureRect.width() - s(20);
        int formatY = this.structureRect.y() + (pagedUi() ? 34 : s(compactUi() ? 28 : 48));
        int formatWidth = Math.max(s(105), Math.round(structureInnerWidth * 0.30F));
        this.doubleRect = new SignEditorUi.Rect(
                structureInnerX + structureInnerWidth - formatWidth,
                formatY,
                formatWidth,
                pagedUi() ? 20 : s(24)
        );
        this.simpleRect = new SignEditorUi.Rect(
                this.doubleRect.x() - (pagedUi() ? 6 : s(6)) - formatWidth,
                formatY,
                formatWidth,
                pagedUi() ? 20 : s(24)
        );
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

        drawTabs(graphics, mouseX, mouseY);
        drawPreview(graphics);
        if (pagedUi()) {
            SignEditorUi.drawPageTabs(
                    graphics,
                    this.font,
                    this.settingsPageRects,
                    new String[]{"Texte", "Style", "Cart.", "Format"},
                    this.settingsPage,
                    mouseX,
                    mouseY
            );
            switch (this.settingsPage) {
                case 0 -> drawContentSection(graphics, mouseX, mouseY);
                case 1 -> drawStyleSection(graphics, mouseX, mouseY);
                case 2 -> drawCartoucheSection(graphics, mouseX, mouseY);
                default -> drawStructureSection(graphics, mouseX, mouseY);
            }
        } else {
            drawContentSection(graphics, mouseX, mouseY);
            drawStyleSection(graphics, mouseX, mouseY);
            drawCartoucheSection(graphics, mouseX, mouseY);
            drawStructureSection(graphics, mouseX, mouseY);
        }
        drawFooter(graphics, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < this.tabRects.length; i++) {
            D21APanelData panel = i == this.selectedPanelIndex ? currentPanelFromWidgets() : this.panels[i];
            String lineInfo = panel.doubleLine() ? "2 lignes" : "1 ligne";

            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.tabRects[i],
                    compactUi() ? "Panneau " + (i + 1) : "Panneau " + (i + 1) + "  •  " + lineInfo,
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
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.PreviewBox box = new SignEditorUi.PreviewBox(
                this.previewRect.x(),
                this.previewRect.y(),
                this.previewRect.width(),
                this.previewRect.height(),
                true
        );

        D21APanelData[] previewPanels = this.panels.clone();
        previewPanels[this.selectedPanelIndex] = currentPanelFromWidgets();

        SignEditorUi.drawD21StackPreview(
                graphics,
                this.font,
                box,
                previewPanels,
                this.selectedPanelIndex,
                this.cartoucheType,
                this.cartoucheTextField == null ? this.cartoucheText : this.cartoucheTextField.getValue()
        );
    }

    private void drawContentSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "1. CONTENU",
                pagedUi() ? "" : "Destination, police et kilométrage"
        );

        if (!compactUi()) {
            int labelY = this.contentRect.y() + s(32);
            SignEditorUi.drawFieldLabel(graphics, this.font, "Destination", this.line1Field.getX(), labelY);
            SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line1FontRect.x(), labelY);
            SignEditorUi.drawFieldLabel(graphics, this.font, "Espacement", this.line1SpacingRect.x(), labelY);
            SignEditorUi.drawFieldLabel(graphics, this.font, "Kilométrage (km)", this.distance1Field.getX(), labelY);
        }

        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.line1FontRect,
                SignEditorUi.fontLabel(this.line1Font),
                false,
                true,
                mouseX,
                mouseY
        );
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.line1SpacingRect,
                this.line1Spacing ? "a b" : "ab",
                false,
                true,
                mouseX,
                mouseY
        );

        if (this.doubleLine) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.line2FontRect,
                    SignEditorUi.fontLabel(this.line2Font),
                    false,
                    true,
                    mouseX,
                    mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.line2SpacingRect,
                    this.line2Spacing ? "a b" : "ab",
                    false,
                    true,
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawStyleSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.styleRect,
                "2. STYLE",
                pagedUi() ? "" : "Couleur, logo autoroute et direction"
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Couleur du panneau", this.whiteRect.x(), this.whiteRect.y() - s(12));
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc", this.selectedType == D21AType.WHITE, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert", this.selectedType == D21AType.GREEN, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu", this.selectedType == D21AType.BLUE, true, mouseX, mouseY);

        boolean logoAllowed = this.selectedType == D21AType.GREEN || this.selectedType == D21AType.BLUE;
        SignEditorUi.drawModernToggle(
                graphics,
                this.font,
                this.logoRect,
                "Logo autoroute",
                compactUi() ? "" : "Afficher le logo autoroute sur le panneau",
                this.autorouteLogo,
                logoAllowed,
                mouseX,
                mouseY
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Direction", this.directionLeftRect.x(), this.directionLeftRect.y() - s(12));
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.directionLeftRect, "← Gauche", !this.arrowRight, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.directionRightRect, "Droite →", this.arrowRight, true, mouseX, mouseY);
    }

    private void drawCartoucheSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.cartoucheRect,
                "3. CARTOUCHE",
                pagedUi() ? "" : "Type et texte affiché au-dessus du panneau"
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Type de cartouche", this.cartoucheTypeRect.x(), this.cartoucheTypeRect.y() - s(12));
        }
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
        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Texte du cartouche", this.cartoucheTextField.getX(), this.cartoucheTextField.getY() - s(12));
        }
    }

    private void drawStructureSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.structureRect,
                "4. STRUCTURE",
                pagedUi() ? "" : "Format du panneau sélectionné"
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Format du panneau", this.simpleRect.x(), this.simpleRect.y() - s(12));
        }
        SignEditorUi.drawModernButton(graphics, this.font, this.simpleRect, "Simple (1 ligne)", !this.doubleLine, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.doubleRect, "Double (2 lignes)", this.doubleLine, true, mouseX, mouseY);
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int tipX = this.windowX + s(18);
        int tipY = this.windowY + this.windowHeight - s(28);
        if (!compactUi()) {
            graphics.text(
                    this.font,
                    Component.literal(SignEditorUi.fitText(this.font, "Conseil : chaque Panneau possède son activation directement dans les onglets du haut.", this.windowWidth - Math.max(36, s(36)))),
                    tipX,
                    tipY,
                    SignEditorUi.MODERN_MUTED,
                    false
            );
        }

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            for (int i = 0; i < this.tabRects.length; i++) {
                if (this.tabToggleRects[i].contains(mouseX, mouseY)) {
                    togglePanelEnabled(i);
                    return true;
                }
                if (this.tabRects[i].contains(mouseX, mouseY)) {
                    selectPanel(i);
                    return true;
                }
            }

            if (pagedUi()) {
                for (int i = 0; i < this.settingsPageRects.length; i++) {
                    if (this.settingsPageRects[i] != null && this.settingsPageRects[i].contains(mouseX, mouseY)) {
                        this.settingsPage = i;
                        updatePagedVisibility();
                        return true;
                    }
                }
            }

            if ((!pagedUi() || this.settingsPage == 0) && this.line1FontRect.contains(mouseX, mouseY)) {
                if ((selectedType == D21AType.BLUE || selectedType == D21AType.GREEN)
                        && (this.line1Font == RoadTextFont.L1 ||  this.line1Font == RoadTextFont.L4))
                    this.line1Font = RoadTextFont.L2;
                else
                    this.line1Font = this.line1Font.next();
            }
            if ((!pagedUi() || this.settingsPage == 0) && this.line1SpacingRect.contains(mouseX, mouseY)) {
                this.line1Spacing = !line1Spacing;
            }
            if ((!pagedUi() || this.settingsPage == 0) && this.line2SpacingRect.contains(mouseX, mouseY)) {
                this.line2Spacing = !line2Spacing;
            }
            if ((!pagedUi() || this.settingsPage == 0) && this.doubleLine && this.line2FontRect.contains(mouseX, mouseY)) {
                if ((selectedType == D21AType.BLUE || selectedType == D21AType.GREEN)
                        && (this.line2Font == RoadTextFont.L1 ||  this.line2Font == RoadTextFont.L4))
                    this.line2Font = RoadTextFont.L2;
                else
                    this.line2Font = this.line2Font.next();
                return true;
            }
            if (!pagedUi() || this.settingsPage == 1) {
                if (this.whiteRect.contains(mouseX, mouseY)) {
                    selectType(D21AType.WHITE);
                    if (line1Font == RoadTextFont.L2) {
                        this.line1Font = RoadTextFont.L1;
                        this.line2Font = RoadTextFont.L1;
                    }

                    return true;
                }
                if (this.greenRect.contains(mouseX, mouseY)) {
                    selectType(D21AType.GREEN);
                    if (line1Font == RoadTextFont.L1) {
                        this.line1Font = RoadTextFont.L2;
                        this.line2Font = RoadTextFont.L2;
                    }
                    return true;
                }
                if (this.blueRect.contains(mouseX, mouseY)) {
                    selectType(D21AType.BLUE);
                    if (line1Font == RoadTextFont.L1) {
                        this.line1Font = RoadTextFont.L2;
                        this.line2Font = RoadTextFont.L2;
                    }
                    return true;
                }
                if (this.logoRect.contains(mouseX, mouseY)
                        && (this.selectedType == D21AType.GREEN || this.selectedType == D21AType.BLUE)) {
                    this.autorouteLogo = !this.autorouteLogo;
                    return true;
                }
                if (this.directionLeftRect.contains(mouseX, mouseY)) {
                    this.arrowRight = false;
                    return true;
                }
                if (this.directionRightRect.contains(mouseX, mouseY)) {
                    this.arrowRight = true;
                    return true;
                }
            }
            if ((!pagedUi() || this.settingsPage == 2) && this.cartoucheTypeRect.contains(mouseX, mouseY)) {
                this.cartoucheType = this.cartoucheType.next();
                updateCartoucheFieldState();
                return true;
            }
            if (!pagedUi() || this.settingsPage == 3) {
                if (this.simpleRect.contains(mouseX, mouseY)) {
                    this.doubleLine = false;
                    updateFieldVisibility();
                    return true;
                }
                if (this.doubleRect.contains(mouseX, mouseY)) {
                    this.doubleLine = true;
                    updateFieldVisibility();
                    return true;
                }
            }
            if (this.applyRect.contains(mouseX, mouseY)) {
                save();
                return true;
            }
            if (this.cancelRect.contains(mouseX, mouseY)) {
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
                panel.line2Font(),
                panel.line1Spacing(),
                panel.line2Spacing()
        );
    }

    private void selectPanel(int newIndex) {
        if (newIndex < 0 || newIndex >= D21ABlockEntity.MAX_PANELS || newIndex == this.selectedPanelIndex) {
            return;
        }

        storeSelectedPanelFromWidgets();
        this.selectedPanelIndex = newIndex;
        loadSelectedPanelIntoWidgets();
        this.setInitialFocus(this.line1Field);
    }

    private void selectType(D21AType type) {
        this.selectedType = type == null ? D21AType.WHITE : type;
        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }
    }

    private D21APanelData currentPanelFromWidgets() {
        if (this.line1Field == null || this.line2Field == null || this.distance1Field == null || this.distance2Field == null) {
            return this.panels[this.selectedPanelIndex];
        }

        return new D21APanelData(
                this.panelEnabled,
                this.line1Field.getValue(),
                this.line2Field.getValue(),
                this.distance1Field.getValue(),
                this.distance2Field.getValue(),
                this.selectedType,
                this.arrowRight,
                this.autorouteLogo,
                this.doubleLine,
                this.line1Font,
                this.line2Font,
                this.line1Spacing,
                this.line2Spacing
        );
    }

    private void storeSelectedPanelFromWidgets() {
        this.panels[this.selectedPanelIndex] = currentPanelFromWidgets();
    }

    private void loadSelectedPanelIntoWidgets() {
        D21APanelData panel = this.panels[this.selectedPanelIndex];
        this.panelEnabled = panel.enabled();
        this.doubleLine = panel.doubleLine();
        this.selectedType = panel.type();
        this.arrowRight = panel.arrowRight();
        this.autorouteLogo = panel.autorouteLogo();
        this.line1Font = panel.line1Font();
        this.line2Font = panel.line2Font();
        this.line1Spacing = panel.line1Spacing();
        this.line2Spacing = panel.line2Spacing();

        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }

        this.line1Field.setValue(panel.line1());
        this.line2Field.setValue(panel.line2());
        this.distance1Field.setValue(panel.distance1());
        this.distance2Field.setValue(panel.distance2());
        updateFieldVisibility();
        updateCartoucheFieldState();
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
            boolean visible = !pagedUi() || this.settingsPage == 2;
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

    private boolean pagedUi() {
        return SignEditorUi.pagedCompactMode(this.scale, this.windowHeight);
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
                        this.panels[3],
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );
        this.onClose();
    }
}
