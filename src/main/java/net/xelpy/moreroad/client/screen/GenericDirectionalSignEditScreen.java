package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.GenericArrowShape;
import net.xelpy.moreroad.block.custom.GenericDestinationRow;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;
import net.xelpy.moreroad.block.custom.GenericRouteCartoucheData;
import net.xelpy.moreroad.block.custom.GenericSignAlignment;
import net.xelpy.moreroad.block.custom.GenericSignHeader;
import net.xelpy.moreroad.block.custom.GenericSignSymbol;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.network.UpdateGenericDirectionalSignPayload;

/**
 * Éditeur du panneau directionnel modulable générique.
 *
 * Construit sur le même modèle que {@code D61AEditScreen} (onglets +
 * sections empilées, boîte à outils {@link SignEditorUi} partagée) avec deux
 * jeux d'onglets indépendants (destinations jusqu'à 6, cartouches jusqu'à 3)
 * plus deux réglages globaux : le fond du panneau et l'en-tête optionnel —
 * distinct des cartouches ET des destinations (voir GenericSignHeader).
 */
public class GenericDirectionalSignEditScreen extends Screen {

    private static final int ROWS = GenericDirectionalSignData.MAX_ROWS;
    private static final int CARTOUCHES = GenericDirectionalSignData.MAX_CARTOUCHES;

    private final BlockPos blockPos;
    private final GenericDestinationRow[] rows = new GenericDestinationRow[ROWS];
    private final GenericRouteCartoucheData[] cartouches = new GenericRouteCartoucheData[CARTOUCHES];
    private MotorwaySignColor background = MotorwaySignColor.BLUE;

    private int selectedRow;
    private int selectedCartouche;

    /* Widgets courants pour l'en-tête. */
    private boolean headerEnabled;
    private boolean headerSameAsPanel = true;
    private MotorwaySignColor headerColor = MotorwaySignColor.BLUE;
    private GenericSignAlignment headerAlignment = GenericSignAlignment.CENTER;
    private String headerInitialText = "";
    private EditBox headerTextField;

    /* Widgets courants pour la destination sélectionnée. */
    private boolean rowEnabled;
    private GenericSignAlignment alignment = GenericSignAlignment.CENTER;
    private boolean arrowEnabled;
    private GenericArrowShape arrowShape = GenericArrowShape.DIAGONAL_ROUNDED;
    private boolean arrowMirrored;
    private D61AArrowPosition arrowPosition = D61AArrowPosition.RIGHT;
    private boolean symbolEnabled;
    private GenericSignSymbol symbol = GenericSignSymbol.NONE;
    private D61AArrowPosition symbolPosition = D61AArrowPosition.LEFT;
    private EditBox rowTextField;

    /* Widgets courants pour le cartouche sélectionné. */
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private EditBox cartoucheTextField;

    private final SignEditorUi.Rect[] rowTabRects = new SignEditorUi.Rect[ROWS];
    private final SignEditorUi.Rect[] rowToggleRects = new SignEditorUi.Rect[ROWS];
    private final SignEditorUi.Rect[] cartoucheTabRects = new SignEditorUi.Rect[CARTOUCHES];

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect backgroundRect;
    private SignEditorUi.Rect headerRect;
    private SignEditorUi.Rect destinationRect;
    private SignEditorUi.Rect cartoucheRect;

    private SignEditorUi.Rect whiteRect;
    private SignEditorUi.Rect greenRect;
    private SignEditorUi.Rect blueRect;

    private SignEditorUi.Rect headerEnabledRect;
    private SignEditorUi.Rect headerSameAsPanelRect;
    private SignEditorUi.Rect headerWhiteRect;
    private SignEditorUi.Rect headerGreenRect;
    private SignEditorUi.Rect headerBlueRect;
    private SignEditorUi.Rect headerAlignLeftRect;
    private SignEditorUi.Rect headerAlignCenterRect;
    private SignEditorUi.Rect headerAlignRightRect;

    private SignEditorUi.Rect alignLeftRect;
    private SignEditorUi.Rect alignCenterRect;
    private SignEditorUi.Rect alignRightRect;
    private SignEditorUi.Rect arrowEnabledRect;
    private SignEditorUi.Rect arrowLeftRect;
    private SignEditorUi.Rect arrowRightRect;
    private SignEditorUi.Rect arrowShapeRect;
    private SignEditorUi.Rect arrowMirroredRect;
    private SignEditorUi.Rect symbolEnabledRect;
    private SignEditorUi.Rect symbolCycleRect;
    private SignEditorUi.Rect symbolLeftRect;
    private SignEditorUi.Rect symbolRightRect;

    private SignEditorUi.Rect cartoucheRedRect;
    private SignEditorUi.Rect cartoucheYellowRect;
    private SignEditorUi.Rect cartoucheGreenRect;
    private SignEditorUi.Rect cartoucheBlueRect;
    private SignEditorUi.Rect cartoucheNoneRect;

    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public GenericDirectionalSignEditScreen(BlockPos blockPos, GenericDirectionalSignData data) {
        super(Component.literal("Éditeur de panneau directionnel"));
        this.blockPos = blockPos.immutable();
        GenericDirectionalSignData safe = data == null ? GenericDirectionalSignData.blank() : data;
        this.background = safe.background();
        for (int i = 0; i < ROWS; i++) {
            this.rows[i] = safe.rows()[i];
        }
        for (int i = 0; i < CARTOUCHES; i++) {
            this.cartouches[i] = safe.cartouches()[i];
        }
        GenericSignHeader header = safe.header();
        this.headerEnabled = header.enabled();
        this.headerSameAsPanel = header.sameAsPanel();
        this.headerColor = header.color();
        this.headerAlignment = header.alignment();
        this.headerInitialText = header.text();
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
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1280.0F, 940.0F);

        boolean compact = compactUi();
        int pad = s(compact ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsH = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int sectionGap = s(7);

        int tabsGap = s(6);
        int rowTabsW = this.windowWidth - pad * 2;
        int rowTabW = (rowTabsW - tabsGap * (ROWS - 1)) / ROWS;
        int rowTabY = this.windowY + header;
        int rowTabH = SignEditorUi.safeControlHeight(this.font, s(26));
        for (int i = 0; i < ROWS; i++) {
            int tx = this.windowX + pad + i * (rowTabW + tabsGap);
            this.rowTabRects[i] = new SignEditorUi.Rect(tx, rowTabY, rowTabW, rowTabH);
            this.rowToggleRects[i] = SignEditorUi.tabToggleRect(this.rowTabRects[i], this.font);
        }

        int cartoucheTabsY = rowTabY + rowTabH + s(6);
        int cartoucheTabW = (rowTabsW - tabsGap * (CARTOUCHES - 1)) / CARTOUCHES;
        int cartoucheTabH = rowTabH;
        for (int i = 0; i < CARTOUCHES; i++) {
            int tx = this.windowX + pad + i * (cartoucheTabW + tabsGap);
            this.cartoucheTabRects[i] = new SignEditorUi.Rect(tx, cartoucheTabsY, cartoucheTabW, cartoucheTabH);
        }

        int bodyY = cartoucheTabsY + cartoucheTabH + tabsH;
        int bodyH = this.windowHeight - (bodyY - this.windowY) - footer;

        int leftW = Math.max(s(280), Math.round((this.windowWidth - pad * 2 - gap) * 0.38F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, Math.max(s(160), bodyH));

        int[] sectionHeights = SignEditorUi.fitSections(
                Math.max(200, bodyH),
                sectionGap,
                new float[]{0.11F, 0.17F, 0.46F, 0.26F},
                new int[]{s(compact ? 48 : 56), s(compact ? 60 : 72), s(compact ? 190 : 230), s(compact ? 90 : 108)}
        );
        int backgroundH = sectionHeights[0];
        int headerH = sectionHeights[1];
        int destinationH = sectionHeights[2];
        int cartoucheH = sectionHeights[3];

        int y = bodyY;
        this.backgroundRect = new SignEditorUi.Rect(rightX, y, rightW, backgroundH);
        y += backgroundH + sectionGap;
        this.headerRect = new SignEditorUi.Rect(rightX, y, rightW, headerH);
        y += headerH + sectionGap;
        this.destinationRect = new SignEditorUi.Rect(rightX, y, rightW, destinationH);
        y += destinationH + sectionGap;
        this.cartoucheRect = new SignEditorUi.Rect(rightX, y, rightW, cartoucheH);

        initBackgroundControls();
        initHeaderControls();
        initDestinationControls();
        initCartoucheControls();

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(74, Math.max(s(145), Math.max(this.font.width("✓  Appliquer"), this.font.width("×  Annuler")) + 18));
        int actionGap = Math.max(4, s(10));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH);
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - actionGap - actionW, actionY, actionW, actionH);

        loadSelectedRowIntoWidgets();
        loadSelectedCartoucheIntoWidgets();
        this.headerTextField.setValue(this.headerInitialText);
        this.setInitialFocus(this.rowTextField);
    }

    private void initBackgroundControls() {
        int innerX = this.backgroundRect.x() + s(10);
        int innerW = this.backgroundRect.width() - s(20);
        int gap = s(8);
        int controlH = s(20);
        int colorY = this.backgroundRect.y() + s(compactUi() ? 26 : 32);
        int colorW = (innerW - gap * 2) / 3;
        this.whiteRect = new SignEditorUi.Rect(innerX, colorY, colorW, controlH);
        this.greenRect = new SignEditorUi.Rect(innerX + colorW + gap, colorY, colorW, controlH);
        this.blueRect = new SignEditorUi.Rect(innerX + (colorW + gap) * 2, colorY, colorW, controlH);
    }

    private void initHeaderControls() {
        int innerX = this.headerRect.x() + s(10);
        int innerW = this.headerRect.width() - s(20);
        int gap = s(6);
        int y = this.headerRect.y() + s(compactUi() ? 26 : 32);

        int toggleW = Math.max(s(90), Math.round(innerW * 0.30F));
        int controlH = s(20);
        this.headerEnabledRect = new SignEditorUi.Rect(innerX, y, toggleW, controlH);
        this.headerTextField = new EditBox(
                this.font, innerX + toggleW + gap, y, innerW - toggleW - gap, controlH, Component.literal("En-tête")
        );
        this.headerTextField.setMaxLength(32);
        this.addRenderableWidget(this.headerTextField);
        y += controlH + s(6);

        int sameW = Math.max(s(110), Math.round(innerW * 0.34F));
        this.headerSameAsPanelRect = new SignEditorUi.Rect(innerX, y, sameW, controlH);
        int colorW = (innerW - sameW - gap - gap * 2) / 3;
        int colorX = innerX + sameW + gap;
        this.headerWhiteRect = new SignEditorUi.Rect(colorX, y, colorW, controlH);
        this.headerGreenRect = new SignEditorUi.Rect(colorX + colorW + gap, y, colorW, controlH);
        this.headerBlueRect = new SignEditorUi.Rect(colorX + (colorW + gap) * 2, y, colorW, controlH);
        y += controlH + s(6);

        int alignW = (innerW - gap * 2) / 3;
        this.headerAlignLeftRect = new SignEditorUi.Rect(innerX, y, alignW, controlH);
        this.headerAlignCenterRect = new SignEditorUi.Rect(innerX + alignW + gap, y, alignW, controlH);
        this.headerAlignRightRect = new SignEditorUi.Rect(innerX + (alignW + gap) * 2, y, alignW, controlH);
    }

    private void initDestinationControls() {
        int innerX = this.destinationRect.x() + s(10);
        int innerW = this.destinationRect.width() - s(20);
        int gap = s(7);
        int fieldH = s(22);
        int y = this.destinationRect.y() + s(compactUi() ? 28 : 36);

        this.rowTextField = new EditBox(this.font, innerX, y, innerW, fieldH, Component.literal("Destination"));
        this.rowTextField.setMaxLength(48);
        this.addRenderableWidget(this.rowTextField);
        y += fieldH + s(12);

        int alignW = (innerW - gap * 2) / 3;
        int alignH = s(21);
        this.alignLeftRect = new SignEditorUi.Rect(innerX, y, alignW, alignH);
        this.alignCenterRect = new SignEditorUi.Rect(innerX + alignW + gap, y, alignW, alignH);
        this.alignRightRect = new SignEditorUi.Rect(innerX + (alignW + gap) * 2, y, alignW, alignH);
        y += alignH + s(9);

        int toggleH = s(22);
        int sideW = Math.max(s(78), Math.round(innerW * 0.22F));
        int sideGap = s(6);
        this.arrowEnabledRect = new SignEditorUi.Rect(innerX, y, innerW - (sideW + sideGap) * 2 - s(6), toggleH);
        this.arrowLeftRect = new SignEditorUi.Rect(this.arrowEnabledRect.x() + this.arrowEnabledRect.width() + s(6), y, sideW, toggleH);
        this.arrowRightRect = new SignEditorUi.Rect(this.arrowLeftRect.x() + sideW + sideGap, y, sideW, toggleH);
        y += toggleH + s(6);

        int mirrorW = Math.max(s(90), Math.round(innerW * 0.28F));
        this.arrowShapeRect = new SignEditorUi.Rect(innerX, y, innerW - mirrorW - gap, s(21));
        this.arrowMirroredRect = new SignEditorUi.Rect(innerX + innerW - mirrorW, y, mirrorW, s(21));
        y += s(21) + s(10);

        this.symbolEnabledRect = new SignEditorUi.Rect(innerX, y, innerW - (sideW + sideGap) * 2 - s(6), toggleH);
        this.symbolLeftRect = new SignEditorUi.Rect(this.symbolEnabledRect.x() + this.symbolEnabledRect.width() + s(6), y, sideW, toggleH);
        this.symbolRightRect = new SignEditorUi.Rect(this.symbolLeftRect.x() + sideW + sideGap, y, sideW, toggleH);
        y += toggleH + s(6);

        this.symbolCycleRect = new SignEditorUi.Rect(innerX, y, innerW, s(21));
    }

    private void initCartoucheControls() {
        int innerX = this.cartoucheRect.x() + s(10);
        int innerW = this.cartoucheRect.width() - s(20);
        int gap = s(7);
        int y = this.cartoucheRect.y() + s(compactUi() ? 28 : 36);

        int colorW = (innerW - gap * 3) / 4;
        int colorH = s(22);
        this.cartoucheRedRect = new SignEditorUi.Rect(innerX, y, colorW, colorH);
        this.cartoucheYellowRect = new SignEditorUi.Rect(innerX + colorW + gap, y, colorW, colorH);
        this.cartoucheGreenRect = new SignEditorUi.Rect(innerX + (colorW + gap) * 2, y, colorW, colorH);
        this.cartoucheBlueRect = new SignEditorUi.Rect(innerX + (colorW + gap) * 3, y, colorW, colorH);
        y += colorH + s(8);

        int noneW = Math.max(s(90), Math.round(innerW * 0.28F));
        this.cartoucheNoneRect = new SignEditorUi.Rect(innerX, y, noneW, s(21));
        this.cartoucheTextField = new EditBox(
                this.font, innerX + noneW + gap, y, innerW - noneW - gap, s(22), Component.literal("Numéro")
        );
        this.cartoucheTextField.setMaxLength(16);
        this.addRenderableWidget(this.cartoucheTextField);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SignEditorUi.COLOR_OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SignEditorUi.drawModernWindow(
                graphics, this.font, this.windowX, this.windowY, this.windowWidth, this.windowHeight,
                "PANNEAU", "Éditeur de panneau directionnel",
                compactUi() ? "" : "Configuration en direct"
        );

        for (int i = 0; i < ROWS; i++) {
            GenericDestinationRow row = i == this.selectedRow ? currentRowFromWidgets() : this.rows[i];
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.rowTabRects[i],
                    "D" + (i + 1), i == this.selectedRow, true, mouseX, mouseY
            );
            SignEditorUi.drawModernToggle(
                    graphics, this.font, this.rowToggleRects[i], "", "", row.enabled(), true, mouseX, mouseY
            );
        }
        for (int i = 0; i < CARTOUCHES; i++) {
            GenericRouteCartoucheData cartouche = i == this.selectedCartouche ? currentCartoucheFromWidgets() : this.cartouches[i];
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.cartoucheTabRects[i],
                    "Cart. " + (i + 1), i == this.selectedCartouche, cartouche.isVisible() || i == this.selectedCartouche, mouseX, mouseY
            );
        }

        drawPreview(graphics);

        drawBackgroundSection(graphics, mouseX, mouseY);
        drawHeaderSection(graphics, mouseX, mouseY);
        drawDestinationSection(graphics, mouseX, mouseY);
        drawCartoucheSection(graphics, mouseX, mouseY);

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBackgroundSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.backgroundRect, "1. FOND DU PANNEAU", compactUi() ? "" : "Bleu autoroutier, vert ou blanc");
        SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc", this.background == MotorwaySignColor.WHITE, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert", this.background == MotorwaySignColor.GREEN, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu", this.background == MotorwaySignColor.BLUE, true, mouseX, mouseY);
    }

    private void drawHeaderSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(graphics, this.font, this.headerRect, "2. EN-TÊTE", compactUi() ? "" : "Optionnel, distinct des cartouches");
        SignEditorUi.drawModernToggle(graphics, this.font, this.headerEnabledRect, "Activer", "", this.headerEnabled, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerSameAsPanelRect, "Fond du panneau", this.headerSameAsPanel, this.headerEnabled, mouseX, mouseY);
        boolean colorButtonsActive = this.headerEnabled && !this.headerSameAsPanel;
        SignEditorUi.drawModernButton(graphics, this.font, this.headerWhiteRect, "Blanc", this.headerColor == MotorwaySignColor.WHITE, colorButtonsActive, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerGreenRect, "Vert", this.headerColor == MotorwaySignColor.GREEN, colorButtonsActive, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerBlueRect, "Bleu", this.headerColor == MotorwaySignColor.BLUE, colorButtonsActive, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerAlignLeftRect, "Gauche", this.headerAlignment == GenericSignAlignment.LEFT, this.headerEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerAlignCenterRect, "Centre", this.headerAlignment == GenericSignAlignment.CENTER, this.headerEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.headerAlignRightRect, "Droite", this.headerAlignment == GenericSignAlignment.RIGHT, this.headerEnabled, mouseX, mouseY);
    }

    private void drawDestinationSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.destinationRect,
                "3. DESTINATION " + (this.selectedRow + 1),
                compactUi() ? "" : "Texte, alignement, flèche et symbole"
        );
        SignEditorUi.drawModernButton(graphics, this.font, this.alignLeftRect, "Gauche", this.alignment == GenericSignAlignment.LEFT, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.alignCenterRect, "Centre", this.alignment == GenericSignAlignment.CENTER, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.alignRightRect, "Droite", this.alignment == GenericSignAlignment.RIGHT, true, mouseX, mouseY);

        SignEditorUi.drawModernToggle(graphics, this.font, this.arrowEnabledRect, "Flèche", "", this.arrowEnabled, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.arrowLeftRect, "Gauche", this.arrowPosition == D61AArrowPosition.LEFT, this.arrowEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.arrowRightRect, "Droite", this.arrowPosition == D61AArrowPosition.RIGHT, this.arrowEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.arrowShapeRect, this.arrowShape.getDisplayName(), this.arrowEnabled, this.arrowEnabled, mouseX, mouseY);
        SignEditorUi.drawModernToggle(graphics, this.font, this.arrowMirroredRect, "Miroir", "", this.arrowMirrored, this.arrowEnabled, mouseX, mouseY);

        SignEditorUi.drawModernToggle(graphics, this.font, this.symbolEnabledRect, "Symbole", "", this.symbolEnabled, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.symbolLeftRect, "Gauche", this.symbolPosition == D61AArrowPosition.LEFT, this.symbolEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.symbolRightRect, "Droite", this.symbolPosition == D61AArrowPosition.RIGHT, this.symbolEnabled, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.symbolCycleRect, this.symbol.getDisplayName(), this.symbolEnabled, this.symbolEnabled, mouseX, mouseY);
    }

    private void drawCartoucheSection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.cartoucheRect,
                "4. CARTOUCHE " + (this.selectedCartouche + 1),
                compactUi() ? "" : "Couleur et numéro de route"
        );
        SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheRedRect, "Rouge", this.cartoucheType == CartoucheType.E42, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheYellowRect, "Jaune", this.cartoucheType == CartoucheType.E43, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheGreenRect, "Vert", this.cartoucheType == CartoucheType.E41_45, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheBlueRect, "Bleu", this.cartoucheType == CartoucheType.E47, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheNoneRect, "Aucun", this.cartoucheType == CartoucheType.NONE, true, mouseX, mouseY);
    }

    /** Aperçu schématique simple (pas le rendu 3D complet) : suffisant pour vérifier la composition en direct. */
    private void drawPreview(GuiGraphicsExtractor graphics) {
        int x = this.previewRect.x();
        int y = this.previewRect.y();
        int w = this.previewRect.width();
        int h = this.previewRect.height();
        graphics.fill(x, y, x + w, y + h, 0xFF1A1A1E);

        MotorwaySignColor bg = this.background;
        int bgColor = bg.getArgb();
        int textColor = bg.getTextArgb();

        GenericRouteCartoucheData[] previewCartouches = this.cartouches.clone();
        previewCartouches[this.selectedCartouche] = currentCartoucheFromWidgets();
        int visibleCartouches = 0;
        for (GenericRouteCartoucheData cartouche : previewCartouches) {
            if (cartouche.isVisible()) {
                visibleCartouches++;
            }
        }

        int pad = 10;
        int cartoucheH = visibleCartouches > 0 ? 18 : 0;
        int panelTop = y + pad + cartoucheH + (cartoucheH > 0 ? 6 : 0);

        if (visibleCartouches > 0) {
            int chipW = Math.min(48, (w - pad * 2 - (visibleCartouches - 1) * 4) / Math.max(1, visibleCartouches));
            int cx = x + w / 2 - (chipW * visibleCartouches + 4 * (visibleCartouches - 1)) / 2;
            for (GenericRouteCartoucheData cartouche : previewCartouches) {
                if (!cartouche.isVisible()) {
                    continue;
                }
                MotorwaySignColor color = cartouche.backgroundColor();
                graphics.fill(cx, y + pad, cx + chipW, y + pad + cartoucheH, color.getArgb());
                graphics.text(
                        this.font, Component.literal(SignEditorUi.fitText(this.font, cartouche.text(), chipW - 4)),
                        cx + chipW / 2 - this.font.width(cartouche.text()) / 2, y + pad + 5, color.getTextArgb(), false
                );
                cx += chipW + 4;
            }
        }

        int panelBottom = y + h - pad;

        int headerH = 0;
        if (this.headerEnabled) {
            headerH = 16;
            MotorwaySignColor headerColorEffective = this.headerSameAsPanel ? bg : this.headerColor;
            graphics.fill(x + pad, panelTop, x + w - pad, panelTop + headerH, headerColorEffective.getArgb());
            String headerText = this.headerTextField == null ? "" : this.headerTextField.getValue();
            if (!headerText.isBlank()) {
                int textWidth = this.font.width(headerText);
                int innerLeft = x + pad + 4;
                int innerRight = x + w - pad - 4;
                int textX = switch (this.headerAlignment) {
                    case LEFT -> innerLeft;
                    case RIGHT -> innerRight - textWidth;
                    case CENTER -> innerLeft + (innerRight - innerLeft) / 2 - textWidth / 2;
                };
                graphics.text(this.font, Component.literal(headerText), textX, panelTop + 4, headerColorEffective.getTextArgb(), false);
            }
            panelTop += headerH + 3;
        }

        graphics.fill(x + pad, panelTop, x + w - pad, Math.max(panelTop + 20, panelBottom), bgColor);

        GenericDestinationRow[] previewRows = this.rows.clone();
        previewRows[this.selectedRow] = currentRowFromWidgets();
        int lineY = panelTop + 6;
        int innerLeft = x + pad + 8;
        int innerRight = x + w - pad - 8;
        for (GenericDestinationRow row : previewRows) {
            if (!row.enabled() || !row.hasContent()) {
                continue;
            }
            if (lineY > panelBottom - 10) {
                break;
            }
            String text = row.text().isBlank() ? "…" : row.text();
            int textWidth = this.font.width(text);
            int textX = switch (row.alignment()) {
                case LEFT -> innerLeft;
                case RIGHT -> innerRight - textWidth;
                case CENTER -> innerLeft + (innerRight - innerLeft) / 2 - textWidth / 2;
            };
            graphics.text(this.font, Component.literal(text), textX, lineY, textColor, false);
            if (row.arrowEnabled()) {
                int arrowX = row.arrowPosition() == D61AArrowPosition.LEFT ? innerLeft - 10 : innerRight - 8;
                graphics.text(this.font, Component.literal(row.arrowMirrored() ? "<" : ">"), arrowX, lineY, textColor, false);
            }
            lineY += 12;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();

            for (int i = 0; i < ROWS; i++) {
                if (this.rowToggleRects[i].contains(x, y)) {
                    toggleRowEnabled(i);
                    return true;
                }
                if (this.rowTabRects[i].contains(x, y)) {
                    selectRow(i);
                    return true;
                }
            }
            for (int i = 0; i < CARTOUCHES; i++) {
                if (this.cartoucheTabRects[i].contains(x, y)) {
                    selectCartouche(i);
                    return true;
                }
            }

            if (this.whiteRect.contains(x, y)) {
                selectBackground(MotorwaySignColor.WHITE);
                return true;
            }
            if (this.greenRect.contains(x, y)) {
                selectBackground(MotorwaySignColor.GREEN);
                return true;
            }
            if (this.blueRect.contains(x, y)) {
                selectBackground(MotorwaySignColor.BLUE);
                return true;
            }

            if (this.headerEnabledRect.contains(x, y)) {
                this.headerEnabled = !this.headerEnabled;
                return true;
            }
            if (this.headerEnabled && this.headerSameAsPanelRect.contains(x, y)) {
                this.headerSameAsPanel = !this.headerSameAsPanel;
                return true;
            }
            if (this.headerEnabled && !this.headerSameAsPanel && this.headerWhiteRect.contains(x, y)) {
                this.headerColor = MotorwaySignColor.WHITE;
                return true;
            }
            if (this.headerEnabled && !this.headerSameAsPanel && this.headerGreenRect.contains(x, y)) {
                this.headerColor = MotorwaySignColor.GREEN;
                return true;
            }
            if (this.headerEnabled && !this.headerSameAsPanel && this.headerBlueRect.contains(x, y)) {
                this.headerColor = MotorwaySignColor.BLUE;
                return true;
            }
            if (this.headerEnabled && this.headerAlignLeftRect.contains(x, y)) {
                this.headerAlignment = GenericSignAlignment.LEFT;
                return true;
            }
            if (this.headerEnabled && this.headerAlignCenterRect.contains(x, y)) {
                this.headerAlignment = GenericSignAlignment.CENTER;
                return true;
            }
            if (this.headerEnabled && this.headerAlignRightRect.contains(x, y)) {
                this.headerAlignment = GenericSignAlignment.RIGHT;
                return true;
            }

            if (this.alignLeftRect.contains(x, y)) {
                this.alignment = GenericSignAlignment.LEFT;
                return true;
            }
            if (this.alignCenterRect.contains(x, y)) {
                this.alignment = GenericSignAlignment.CENTER;
                return true;
            }
            if (this.alignRightRect.contains(x, y)) {
                this.alignment = GenericSignAlignment.RIGHT;
                return true;
            }
            if (this.arrowEnabledRect.contains(x, y)) {
                this.arrowEnabled = !this.arrowEnabled;
                if (this.arrowEnabled && this.arrowShape == GenericArrowShape.NONE) {
                    this.arrowShape = GenericArrowShape.DIAGONAL_ROUNDED;
                }
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
            if (this.arrowEnabled && this.arrowShapeRect.contains(x, y)) {
                this.arrowShape = this.arrowShape.next();
                if (this.arrowShape == GenericArrowShape.NONE) {
                    this.arrowShape = this.arrowShape.next();
                }
                return true;
            }
            if (this.arrowEnabled && this.arrowMirroredRect.contains(x, y)) {
                this.arrowMirrored = !this.arrowMirrored;
                return true;
            }
            if (this.symbolEnabledRect.contains(x, y)) {
                this.symbolEnabled = !this.symbolEnabled;
                if (this.symbolEnabled && this.symbol == GenericSignSymbol.NONE) {
                    this.symbol = GenericSignSymbol.AUTOROUTE;
                }
                return true;
            }
            if (this.symbolEnabled && this.symbolLeftRect.contains(x, y)) {
                this.symbolPosition = D61AArrowPosition.LEFT;
                return true;
            }
            if (this.symbolEnabled && this.symbolRightRect.contains(x, y)) {
                this.symbolPosition = D61AArrowPosition.RIGHT;
                return true;
            }
            if (this.symbolEnabled && this.symbolCycleRect.contains(x, y)) {
                this.symbol = this.symbol.next();
                if (this.symbol == GenericSignSymbol.NONE) {
                    this.symbol = this.symbol.next();
                }
                return true;
            }

            if (this.cartoucheRedRect.contains(x, y)) {
                this.cartoucheType = CartoucheType.E42;
                return true;
            }
            if (this.cartoucheYellowRect.contains(x, y)) {
                this.cartoucheType = CartoucheType.E43;
                return true;
            }
            if (this.cartoucheGreenRect.contains(x, y)) {
                this.cartoucheType = CartoucheType.E41_45;
                return true;
            }
            if (this.cartoucheBlueRect.contains(x, y)) {
                this.cartoucheType = CartoucheType.E47;
                return true;
            }
            if (this.cartoucheNoneRect.contains(x, y)) {
                this.cartoucheType = CartoucheType.NONE;
                return true;
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

    private GenericDestinationRow currentRowFromWidgets() {
        return new GenericDestinationRow(
                this.rowEnabled,
                this.rowTextField == null ? "" : this.rowTextField.getValue(),
                this.alignment,
                roadTextFontForBackground(),
                this.arrowEnabled,
                this.arrowShape,
                this.arrowMirrored,
                this.arrowPosition,
                this.symbolEnabled,
                this.symbol,
                this.symbolPosition
        );
    }

    private RoadTextFont roadTextFontForBackground() {
        return this.background.isLight() ? RoadTextFont.L1 : RoadTextFont.L2;
    }

    private GenericRouteCartoucheData currentCartoucheFromWidgets() {
        return new GenericRouteCartoucheData(
                this.cartoucheType,
                this.cartoucheTextField == null ? "" : this.cartoucheTextField.getValue()
        );
    }

    private GenericSignHeader currentHeaderFromWidgets() {
        MotorwaySignColor effectiveBackground = this.headerSameAsPanel ? this.background : this.headerColor;
        RoadTextFont font = effectiveBackground.isLight() ? RoadTextFont.L1 : RoadTextFont.L2;
        return new GenericSignHeader(
                this.headerEnabled,
                this.headerTextField == null ? "" : this.headerTextField.getValue(),
                this.headerSameAsPanel,
                this.headerColor,
                this.headerAlignment,
                font
        );
    }

    private void toggleRowEnabled(int index) {
        if (index == this.selectedRow) {
            this.rowEnabled = !this.rowEnabled;
            return;
        }
        GenericDestinationRow row = this.rows[index];
        this.rows[index] = new GenericDestinationRow(
                !row.enabled(), row.text(), row.alignment(), row.font(),
                row.arrowEnabled(), row.arrowShape(), row.arrowMirrored(), row.arrowPosition(),
                row.symbolEnabled(), row.symbol(), row.symbolPosition()
        );
    }

    private void selectRow(int newIndex) {
        if (newIndex == this.selectedRow) {
            return;
        }
        this.rows[this.selectedRow] = currentRowFromWidgets();
        this.selectedRow = newIndex;
        loadSelectedRowIntoWidgets();
        this.setInitialFocus(this.rowTextField);
    }

    private void selectCartouche(int newIndex) {
        if (newIndex == this.selectedCartouche) {
            return;
        }
        this.cartouches[this.selectedCartouche] = currentCartoucheFromWidgets();
        this.selectedCartouche = newIndex;
        loadSelectedCartoucheIntoWidgets();
    }

    private void selectBackground(MotorwaySignColor color) {
        this.background = color;
    }

    private void loadSelectedRowIntoWidgets() {
        GenericDestinationRow row = this.rows[this.selectedRow];
        this.rowEnabled = row.enabled();
        this.alignment = row.alignment();
        this.arrowEnabled = row.arrowEnabled();
        this.arrowShape = row.arrowShape() == GenericArrowShape.NONE ? GenericArrowShape.DIAGONAL_ROUNDED : row.arrowShape();
        this.arrowMirrored = row.arrowMirrored();
        this.arrowPosition = row.arrowPosition();
        this.symbolEnabled = row.symbolEnabled();
        this.symbol = row.symbol();
        this.symbolPosition = row.symbolPosition();
        this.rowTextField.setValue(row.text());
    }

    private void loadSelectedCartoucheIntoWidgets() {
        GenericRouteCartoucheData cartouche = this.cartouches[this.selectedCartouche];
        this.cartoucheType = cartouche.type();
        this.cartoucheTextField.setValue(cartouche.text());
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        this.rows[this.selectedRow] = currentRowFromWidgets();
        this.cartouches[this.selectedCartouche] = currentCartoucheFromWidgets();
        GenericSignHeader header = currentHeaderFromWidgets();
        GenericDirectionalSignData data = new GenericDirectionalSignData(this.background, header, this.rows, this.cartouches);
        ClientPacketDistributor.sendToServer(new UpdateGenericDirectionalSignPayload(this.blockPos, data));
        this.onClose();
    }
}
