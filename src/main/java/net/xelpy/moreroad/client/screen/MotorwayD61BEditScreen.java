package net.xelpy.moreroad.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.MotorwaySignGraphic;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.network.UpdateMotorwaySignPayload;

import java.util.ArrayList;
import java.util.List;

/** Éditeur D61b : une pile vierge de panneaux ville/kilométrage uniquement. */
public class MotorwayD61BEditScreen extends Screen {

    private static final FontDescription.Resource ROAD_FONT_L1 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
    );
    private static final FontDescription.Resource ROAD_FONT_L4 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
    );
    private static final FontDescription.Resource ROAD_FONT_L2 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l2")
    );

    private static FontDescription.Resource roadFontResource(RoadTextFont font) {
        return switch (font) {
            case L2 -> ROAD_FONT_L2;
            case L4 -> ROAD_FONT_L4;
            case L1, NORMAL -> ROAD_FONT_L1;
        };
    }

    /*
     * La police n'est pas indépendante de la couleur du fond : L1/L4
     * dessinent un texte sombre prévu pour un fond clair, L2 dessine un
     * vrai texte blanc prévu pour un fond foncé (bleu, vert...).
     */
    private static RoadTextFont forcedFontForColor(RoadTextFont font, MotorwaySignColor color) {
        return color.isLight()
                ? RoadTextFont.forceForLightBackground(font)
                : RoadTextFont.forceForDarkBackground(font);
    }

    private final BlockPos blockPos;
    private final MotorwaySignPreset preset;
    private final boolean freeform;
    private final MotorwaySignLineData[] originalLines =
            new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
    private final MotorwaySignPanelData[] panels =
            new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final EditBox[] cityFields = new EditBox[4];
    private final EditBox[] distanceFields = new EditBox[4];
    private final SignEditorUi.Rect[] fontRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] tabRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final SignEditorUi.Rect[] tabToggleRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final SignEditorUi.Rect[] pageRects = new SignEditorUi.Rect[3];
    private final SignEditorUi.Rect[] formatRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] backgroundRects = new SignEditorUi.Rect[3];
    private SignEditorUi.Rect graphicRect;

    private int selectedPanel;
    private int settingsPage;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";
    private EditBox cartoucheField;
    private SignEditorUi.Rect cartoucheTypeRect;
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect settingsRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;
    private SignEditorUi.Rect previousModelRect;
    private SignEditorUi.Rect nextModelRect;
    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public MotorwayD61BEditScreen(
            BlockPos blockPos,
            MotorwaySignLineData[] lines,
            MotorwaySignPanelData[] currentPanels
    ) {
        this(blockPos, MotorwaySignPreset.D61B, lines, currentPanels);
    }

    public MotorwayD61BEditScreen(
            BlockPos blockPos,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] lines,
            MotorwaySignPanelData[] currentPanels
    ) {
        super(Component.literal("Panneau autoroutier personnalisable"));
        this.blockPos = blockPos.immutable();
        this.preset = preset == MotorwaySignPreset.FREEFORM
                ? MotorwaySignPreset.FREEFORM
                : MotorwaySignPreset.D61B;
        this.freeform = this.preset == MotorwaySignPreset.FREEFORM;
        for (int index = 0; index < this.originalLines.length; index++) {
            this.originalLines[index] = lines != null && index < lines.length && lines[index] != null
                    ? lines[index]
                    : MotorwaySignLineData.empty();
        }

        boolean hasEnabledPanel = false;
        for (int index = 0; index < this.panels.length; index++) {
            MotorwaySignPanelData source = currentPanels != null && index < currentPanels.length
                    ? currentPanels[index]
                    : null;
            if (index == 0 && source != null) {
                this.cartoucheType = source.cartoucheType();
                this.cartoucheText = source.cartoucheText();
            }
            this.panels[index] = sanitize(source == null ? MotorwaySignPanelData.disabled() : source);
            hasEnabledPanel |= this.panels[index].enabled();
        }
        if (!hasEnabledPanel) {
            this.panels[0] = emptyPanel(true);
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
        this.scale = SignEditorUi.adaptiveEditorScale(
                this.windowWidth, this.windowHeight, 1240.0F, 860.0F
        );

        int pad = s(compactUi() ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsH = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int tabGap = s(8);
        int tabY = this.windowY + header;
        int tabsWidth = this.windowWidth - pad * 2;
        int tabWidth = (tabsWidth - tabGap * 3) / 4;
        for (int index = 0; index < this.tabRects.length; index++) {
            int x = this.windowX + pad + index * (tabWidth + tabGap);
            this.tabRects[index] = new SignEditorUi.Rect(
                    x, tabY, tabWidth, SignEditorUi.safeControlHeight(this.font, s(32))
            );
            this.tabToggleRects[index] = SignEditorUi.tabToggleRect(this.tabRects[index], this.font);
        }

        int bodyY = this.windowY + header + tabsH;
        int bodyH = this.windowHeight - header - tabsH - footer;
        int leftW = Math.max(s(310), Math.round((this.windowWidth - pad * 2 - gap) * 0.44F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;
        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, Math.max(s(160), bodyH - s(8)));

        int pageHeight = SignEditorUi.safeControlHeight(this.font, s(28));
        SignEditorUi.Rect pageBar = new SignEditorUi.Rect(rightX, bodyY, rightW, pageHeight);
        SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, 3, pageHeight, s(6));
        System.arraycopy(pages, 0, this.pageRects, 0, this.pageRects.length);
        this.settingsRect = new SignEditorUi.Rect(
                rightX, bodyY + pageHeight + s(8), rightW,
                Math.max(s(150), bodyH - pageHeight - s(16))
        );

        initTextControls();
        initFormatControls();
        initCartoucheControls();

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(82, s(145));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH
        );
        this.applyRect = new SignEditorUi.Rect(
                this.cancelRect.x() - s(10) - actionW, actionY, actionW, actionH
        );
        int modelW = Math.max(190, s(244));
        this.previousModelRect = new SignEditorUi.Rect(this.windowX + pad, actionY, modelW, actionH);
        this.nextModelRect = new SignEditorUi.Rect(0, 0, 0, 0);

        loadPanelIntoWidgets();
        updateVisibility();
        this.setInitialFocus(this.cityFields[0]);
    }

    private void initTextControls() {
        int innerX = this.settingsRect.x() + s(12);
        int innerW = this.settingsRect.width() - s(24);
        int rowGap = s(12);
        int fieldH = SignEditorUi.safeControlHeight(this.font, s(26));
        int fontW = Math.max(s(84), Math.round(innerW * 0.20F));
        int distanceW = Math.max(s(62), Math.round(innerW * 0.16F));
        int gap = s(7);
        int cityW = innerW - fontW - distanceW - gap * 2;
        int firstY = this.settingsRect.y() + s(54);
        for (int index = 0; index < 4; index++) {
            int y = firstY + index * (fieldH + rowGap);
            this.cityFields[index] = new EditBox(
                    this.font, innerX, y, cityW, fieldH,
                    Component.literal("Ville " + (index + 1))
            );
            this.cityFields[index].setMaxLength(64);
            this.cityFields[index].setHint(Component.literal("Ville " + (index + 1)));
            this.addRenderableWidget(this.cityFields[index]);
            this.fontRects[index] = new SignEditorUi.Rect(
                    innerX + cityW + gap, y, fontW, fieldH
            );
            this.distanceFields[index] = new EditBox(
                    this.font, this.fontRects[index].x() + fontW + gap, y,
                    distanceW, fieldH, Component.literal("Km " + (index + 1))
            );
            this.distanceFields[index].setMaxLength(8);
            this.distanceFields[index].setHint(Component.literal("Km"));
            this.addRenderableWidget(this.distanceFields[index]);
        }
    }

    private void initFormatControls() {
        int innerX = this.settingsRect.x() + s(12);
        int innerW = this.settingsRect.width() - s(24);
        int gap = s(8);
        int width = (innerW - gap * 3) / 4;
        int y = this.settingsRect.y() + s(58);
        int height = SignEditorUi.safeControlHeight(this.font, s(30));
        for (int index = 0; index < 4; index++) {
            this.formatRects[index] = new SignEditorUi.Rect(
                    innerX + index * (width + gap), y, width, height
            );
        }
        int styleY = y + height + s(24);
        int styleGap = s(7);
        int styleW = (innerW - styleGap * 2) / 3;
        for (int index = 0; index < this.backgroundRects.length; index++) {
            this.backgroundRects[index] = new SignEditorUi.Rect(
                    innerX + index * (styleW + styleGap), styleY, styleW, height
            );
        }
        this.graphicRect = new SignEditorUi.Rect(
                innerX, styleY + height + s(12), innerW, height
        );
    }

    private void initCartoucheControls() {
        int innerX = this.settingsRect.x() + s(12);
        int innerW = this.settingsRect.width() - s(24);
        int gap = s(8);
        int typeW = Math.max(s(145), Math.round(innerW * 0.42F));
        int y = this.settingsRect.y() + s(58);
        int height = SignEditorUi.safeControlHeight(this.font, s(28));
        this.cartoucheTypeRect = new SignEditorUi.Rect(innerX, y, typeW, height);
        this.cartoucheField = new EditBox(
                this.font, innerX + typeW + gap, y,
                innerW - typeW - gap, height, Component.literal("Texte du cartouche")
        );
        this.cartoucheField.setMaxLength(24);
        this.cartoucheField.setHint(Component.literal("Ex. A 10"));
        this.cartoucheField.setValue(this.cartoucheText);
        this.addRenderableWidget(this.cartoucheField);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SignEditorUi.COLOR_OVERLAY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        SignEditorUi.drawModernWindow(
                graphics, this.font,
                this.windowX, this.windowY, this.windowWidth, this.windowHeight,
                this.freeform ? "D/DA" : "D61b",
                this.freeform ? "Panneau autoroutier personnalisable" : "Éditeur de panneau D61b",
                compactUi() ? "" : (this.freeform
                        ? "Un seul bloc • registres libres • couleurs, cartouches et symboles"
                        : "Panneaux ville et kilométrage • largeur commune automatique")
        );

        MotorwaySignPanelData current = currentPanelFromWidgets();
        for (int index = 0; index < this.tabRects.length; index++) {
            MotorwaySignPanelData panel = index == this.selectedPanel ? current : this.panels[index];
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.tabRects[index],
                    compactUi()
                            ? "Registre " + (index + 1)
                            : "Registre " + (index + 1) + "  •  " + panel.lineCount() + " ligne(s)",
                    index == this.selectedPanel, true, mouseX, mouseY
            );
            SignEditorUi.drawModernToggle(
                    graphics, this.font, this.tabToggleRects[index],
                    "", "", panel.enabled(), true, mouseX, mouseY
            );
        }

        drawPreview(graphics);
        SignEditorUi.drawPageTabs(
                graphics, this.font, this.pageRects,
                new String[]{"Textes", "Format", "Cartouche"},
                this.settingsPage, mouseX, mouseY
        );
        if (this.settingsPage == 0) {
            drawTextPage(graphics, mouseX, mouseY);
        } else if (this.settingsPage == 1) {
            drawFormatPage(graphics, mouseX, mouseY);
        } else {
            drawCartouchePage(graphics, mouseX, mouseY);
        }

        SignEditorUi.drawModernButton(
                graphics, this.font, this.previousModelRect,
                "▦  Choisir le modèle", false, true, mouseX, mouseY
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.applyRect,
                "✓  Appliquer", true, true, mouseX, mouseY
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.cancelRect,
                "×  Annuler", false, true, mouseX, mouseY
        );
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTextPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.settingsRect,
                this.freeform ? "1. TEXTES ET DISTANCES" : "1. VILLES ET KILOMÉTRAGE",
                compactUi() ? "" : "Chaque ligne possède sa police L1 ou L4"
        );
        MotorwaySignPanelData panel = currentPanelFromWidgets();
        for (int index = 0; index < panel.lineCount(); index++) {
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(
                        graphics, this.font, "Ville " + (index + 1),
                        this.cityFields[index].getX(), this.cityFields[index].getY() - s(11)
                );
                SignEditorUi.drawFieldLabel(
                        graphics, this.font, "Police",
                        this.fontRects[index].x(), this.fontRects[index].y() - s(11)
                );
                SignEditorUi.drawFieldLabel(
                        graphics, this.font, "Km",
                        this.distanceFields[index].getX(), this.distanceFields[index].getY() - s(11)
                );
            }
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.fontRects[index],
                    SignEditorUi.fontLabel(panel.font(index)), false, true, mouseX, mouseY
            );
        }
    }

    private void drawFormatPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.settingsRect,
                "2. FORMAT DU REGISTRE",
                compactUi() ? "" : (this.freeform
                        ? "1 à 4 lignes • fond réglementaire • symbole optionnel"
                        : "Une même pancarte peut contenir de 1 à 4 villes")
        );
        int count = currentPanelFromWidgets().lineCount();
        for (int index = 0; index < this.formatRects.length; index++) {
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.formatRects[index],
                    Integer.toString(index + 1), count == index + 1,
                    true, mouseX, mouseY
            );
        }
        if (this.freeform) {
            MotorwaySignPanelData panel = currentPanelFromWidgets();
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.backgroundRects[0], "Blanc",
                    panel.background() == MotorwaySignColor.WHITE, true, mouseX, mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.backgroundRects[1], "Vert",
                    panel.background() == MotorwaySignColor.GREEN, true, mouseX, mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.backgroundRects[2], "Bleu",
                    panel.background() == MotorwaySignColor.BLUE, true, mouseX, mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.graphicRect,
                    "Symbole : " + graphicLabel(panel.graphic()),
                    panel.graphic() != MotorwaySignGraphic.NONE, true, mouseX, mouseY
            );
        }
    }

    private void drawCartouchePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.settingsRect,
                "3. CARTOUCHE ROUTIER",
                compactUi() ? "" : "E41 à E47 • couleur réglementaire et texte de la route"
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.cartoucheTypeRect,
                SignEditorUi.cartoucheLabel(this.cartoucheType),
                this.cartoucheType.isVisible(), true, mouseX, mouseY
        );
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.previewRect,
                "APERÇU EN DIRECT", compactUi() ? "" : (this.freeform ? "Construction libre • registres configurés" : "D61b • confirmation autoroutière")
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(16);
        int width = this.previewRect.width() - pad * 2;
        int height = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + width, y + height, 0xFFF0F3F6);

        List<MotorwaySignPanelData> enabled = new ArrayList<>();
        MotorwaySignPanelData[] preview = previewPanels();
        for (MotorwaySignPanelData panel : preview) {
            if (panel.enabled()) {
                enabled.add(panel);
            }
        }
        if (enabled.isEmpty()) {
            enabled.add(emptyPanel(true));
        }

        int gap = Math.max(3, s(5));
        int panelWidth = Math.min(width - s(32), Math.max(s(220), Math.round(width * 0.80F)));
        int totalHeight = gap * Math.max(0, enabled.size() - 1);
        for (MotorwaySignPanelData panel : enabled) {
            totalHeight += s(25 + panel.lineCount() * 19);
        }
        int cartoucheHeight = this.cartoucheType.isVisible() ? s(30) : 0;
        if (cartoucheHeight > 0) {
            totalHeight += cartoucheHeight + gap;
        }
        int centerX = x + width / 2;
        int cursorY = y + Math.max(s(18), (height - totalHeight) / 2);
        graphics.fill(
                centerX - s(4), cursorY + totalHeight - s(3),
                centerX + s(4), y + height - s(7), 0xFF2C2C2C
        );
        if (cartoucheHeight > 0) {
            int cartoucheWidth = Math.min(
                    panelWidth,
                    Math.max(s(82), this.font.width(this.cartoucheField.getValue()) + s(28))
            );
            int cartoucheX = centerX - cartoucheWidth / 2;
            graphics.fill(
                    cartoucheX, cursorY,
                    cartoucheX + cartoucheWidth, cursorY + cartoucheHeight,
                    0xFFD7D7D2
            );
            int border = Math.max(2, s(3));
            MotorwaySignColor color = cartouchePreviewColor(this.cartoucheType);
            graphics.fill(
                    cartoucheX + border, cursorY + border,
                    cartoucheX + cartoucheWidth - border, cursorY + cartoucheHeight - border,
                    color.getArgb()
            );
            Component text = Component.literal(this.cartoucheField.getValue()).withStyle(
                    Style.EMPTY.withFont(ROAD_FONT_L1)
            );
            graphics.text(
                    this.font, text,
                    cartoucheX + (cartoucheWidth - this.font.width(text)) / 2,
                    cursorY + (cartoucheHeight - this.font.lineHeight) / 2,
                    color.getTextArgb(), false
            );
            cursorY += cartoucheHeight + gap;
        }
        for (MotorwaySignPanelData panel : enabled) {
            int panelHeight = s(25 + panel.lineCount() * 19);
            int panelX = centerX - panelWidth / 2;
            graphics.fill(
                    panelX, cursorY, panelX + panelWidth, cursorY + panelHeight,
                    previewPanelBorder(panel.background())
            );
            int border = Math.max(2, s(3));
            graphics.fill(
                    panelX + border, cursorY + border,
                    panelX + panelWidth - border, cursorY + panelHeight - border,
                    panel.background().getArgb()
            );
            int lineHeight = panelHeight / panel.lineCount();
            for (int lineIndex = 0; lineIndex < panel.lineCount(); lineIndex++) {
                drawPreviewLine(
                        graphics, panel, lineIndex,
                        panelX + border, cursorY + lineIndex * lineHeight,
                        panelWidth - border * 2,
                        lineIndex == panel.lineCount() - 1
                                ? panelHeight - lineIndex * lineHeight
                                : lineHeight
                );
            }
            cursorY += panelHeight + gap;
        }
    }

    private static int previewPanelBorder(MotorwaySignColor background) {
        return background == MotorwaySignColor.WHITE
                ? MotorwaySignColor.BLACK.getArgb()
                : MotorwaySignColor.WHITE.getArgb();
    }

    private static MotorwaySignColor cartouchePreviewColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E44 -> MotorwaySignColor.WHITE;
            case E47 -> MotorwaySignColor.METROPOLITAN_BLUE;
            default -> MotorwaySignColor.BLUE;
        };
    }

    private void drawPreviewLine(
            GuiGraphicsExtractor graphics,
            MotorwaySignPanelData panel,
            int index,
            int x,
            int y,
            int width,
            int height
    ) {
        FontDescription.Resource roadFont = roadFontResource(panel.font(index));
        Component distance = Component.literal(panel.distance(index)).withStyle(
                Style.EMPTY.withFont(roadFont)
        );
        int distanceWidth = panel.distance(index).isBlank() ? 0 : this.font.width(distance) + s(10);
        Component city = Component.literal(
                SignEditorUi.fitText(
                        this.font, panel.line(index), Math.max(10, width - distanceWidth - s(18))
                )
        ).withStyle(Style.EMPTY.withFont(roadFont));
        int textY = y + (height - this.font.lineHeight) / 2;
        graphics.text(
                this.font, city,
                x + s(10),
                textY, panel.background().getTextArgb(), false
        );
        if (distanceWidth > 0) {
            graphics.text(
                    this.font, distance,
                    x + width - this.font.width(distance) - s(7),
                    textY, panel.background().getTextArgb(), false
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();
            for (int index = 0; index < this.tabRects.length; index++) {
                if (this.tabToggleRects[index].contains(x, y)) {
                    if (index == this.selectedPanel) {
                        MotorwaySignPanelData current = currentPanelFromWidgets();
                        this.panels[index] = withEnabled(current, !current.enabled());
                        loadPanelIntoWidgets();
                    } else {
                        this.panels[index] = withEnabled(
                                this.panels[index], !this.panels[index].enabled()
                        );
                    }
                    return true;
                }
                if (this.tabRects[index].contains(x, y)) {
                    storeSelectedPanel();
                    this.selectedPanel = index;
                    loadPanelIntoWidgets();
                    updateVisibility();
                    this.setInitialFocus(this.cityFields[0]);
                    return true;
                }
            }
            for (int index = 0; index < this.pageRects.length; index++) {
                if (this.pageRects[index].contains(x, y)) {
                    this.settingsPage = index;
                    updateVisibility();
                    return true;
                }
            }
            MotorwaySignPanelData current = currentPanelFromWidgets();
            if (this.settingsPage == 0) {
                for (int index = 0; index < current.lineCount(); index++) {
                    if (this.fontRects[index].contains(x, y)) {
                        this.panels[this.selectedPanel] = withFont(
                                current, index,
                                RoadTextFont.nextForBackground(current.font(index), !current.background().isLight())
                        );
                        loadPanelIntoWidgets();
                        return true;
                    }
                }
            } else if (this.settingsPage == 1) {
                for (int index = 0; index < this.formatRects.length; index++) {
                    if (this.formatRects[index].contains(x, y)) {
                        this.panels[this.selectedPanel] = withLineCount(current, index + 1);
                        loadPanelIntoWidgets();
                        updateVisibility();
                        return true;
                    }
                }
                if (this.freeform) {
                    if (this.backgroundRects[0].contains(x, y)) {
                        this.panels[this.selectedPanel] = withBackground(current, MotorwaySignColor.WHITE);
                        loadPanelIntoWidgets();
                        return true;
                    }
                    if (this.backgroundRects[1].contains(x, y)) {
                        this.panels[this.selectedPanel] = withBackground(current, MotorwaySignColor.GREEN);
                        loadPanelIntoWidgets();
                        return true;
                    }
                    if (this.backgroundRects[2].contains(x, y)) {
                        this.panels[this.selectedPanel] = withBackground(current, MotorwaySignColor.BLUE);
                        loadPanelIntoWidgets();
                        return true;
                    }
                    if (this.graphicRect.contains(x, y)) {
                        MotorwaySignGraphic[] values = MotorwaySignGraphic.values();
                        MotorwaySignGraphic next = values[(current.graphic().ordinal() + 1) % values.length];
                        this.panels[this.selectedPanel] = withGraphic(current, next);
                        loadPanelIntoWidgets();
                        return true;
                    }
                }
            } else if (this.cartoucheTypeRect.contains(x, y)) {
                this.cartoucheType = this.cartoucheType.next();
                updateVisibility();
                return true;
            }
            if (this.previousModelRect.contains(x, y)) {
                storeSelectedPanel();
                this.panels[0] = withCartouche(
                        this.panels[0], this.cartoucheType, this.cartoucheField.getValue()
                );
                Minecraft.getInstance().gui.setScreen(
                        new MotorwayPresetGalleryScreen(
                                this,
                                this.blockPos,
                                this.preset,
                                this.panels
                        )
                );
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

    private void updateVisibility() {
        MotorwaySignPanelData panel = currentPanelFromWidgets();
        for (int index = 0; index < 4; index++) {
            boolean visible = this.settingsPage == 0 && index < panel.lineCount();
            this.cityFields[index].visible = visible;
            this.cityFields[index].active = visible;
            this.distanceFields[index].visible = visible;
            this.distanceFields[index].active = visible;
        }
        if (this.cartoucheField != null) {
            this.cartoucheField.visible = this.settingsPage == 2;
            this.cartoucheField.active = this.settingsPage == 2 && this.cartoucheType.isVisible();
        }
    }

    private void storeSelectedPanel() {
        if (this.cityFields[0] != null) {
            this.panels[this.selectedPanel] = currentPanelFromWidgets();
        }
    }

    private void loadPanelIntoWidgets() {
        if (this.cityFields[0] == null) {
            return;
        }
        MotorwaySignPanelData panel = this.panels[this.selectedPanel];
        for (int index = 0; index < 4; index++) {
            this.cityFields[index].setValue(panel.line(index));
            this.distanceFields[index].setValue(panel.distance(index));
        }
    }

    private MotorwaySignPanelData currentPanelFromWidgets() {
        MotorwaySignPanelData stored = this.panels[this.selectedPanel];
        if (this.cityFields[0] == null) {
            return stored;
        }
        return new MotorwaySignPanelData(
                stored.enabled(), stored.lineCount(),
                this.cityFields[0].getValue(), this.cityFields[1].getValue(),
                this.cityFields[2].getValue(), this.cityFields[3].getValue(),
                this.distanceFields[0].getValue(), this.distanceFields[1].getValue(),
                this.distanceFields[2].getValue(), this.distanceFields[3].getValue(),
                stored.line1Font(), stored.line2Font(), stored.line3Font(), stored.line4Font(),
                this.freeform ? stored.background() : MotorwaySignColor.BLUE,
                CartoucheType.NONE, "",
                this.freeform ? stored.graphic() : MotorwaySignGraphic.NONE
        );
    }

    private MotorwaySignPanelData[] previewPanels() {
        MotorwaySignPanelData[] result = this.panels.clone();
        result[this.selectedPanel] = currentPanelFromWidgets();
        return result;
    }

    private void openOtherModel(MotorwaySignPreset target) {
        MotorwaySignLineData[] defaults = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
        for (int index = 0; index < defaults.length; index++) {
            defaults[index] = index < target.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(target.getSlot(index))
                    : MotorwaySignLineData.empty();
        }
        MotorwaySignPanelData[] noAdditions = new MotorwaySignPanelData[
                MotorwaySignBlockEntity.MAX_CUSTOM_PANELS
        ];
        for (int index = 0; index < noAdditions.length; index++) {
            noAdditions[index] = MotorwaySignPanelData.disabled();
        }
        Minecraft.getInstance().gui.setScreen(
                new MotorwaySignEditScreen(
                        this.blockPos, target, defaults, false, noAdditions, MotorwaySignServiceIcon.defaults()
                )
        );
    }

    private void save() {
        storeSelectedPanel();
        this.panels[0] = withCartouche(
                this.panels[0], this.cartoucheType, this.cartoucheField.getValue()
        );
        ClientPacketDistributor.sendToServer(new UpdateMotorwaySignPayload(
                this.blockPos,
                this.preset.getSerializedName(),
                this.originalLines[0], this.originalLines[1], this.originalLines[2],
                this.originalLines[3], this.originalLines[4], this.originalLines[5],
                true,
                this.panels[0], this.panels[1], this.panels[2], this.panels[3],
                MotorwaySignServiceIcon.NONE, MotorwaySignServiceIcon.NONE, MotorwaySignServiceIcon.NONE,
                MotorwaySignServiceIcon.NONE, MotorwaySignServiceIcon.NONE, MotorwaySignServiceIcon.NONE
        ));
        this.onClose();
    }

    private MotorwaySignPanelData sanitize(MotorwaySignPanelData source) {
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                this.freeform ? sanitizeBackground(source.background()) : MotorwaySignColor.BLUE,
                CartoucheType.NONE, "",
                this.freeform ? source.graphic() : MotorwaySignGraphic.NONE
        );
    }

    private static MotorwaySignPanelData emptyPanel(boolean enabled) {
        return new MotorwaySignPanelData(
                enabled, 1,
                "", "", "", "", "", "", "", "",
                RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1,
                MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
        );
    }

    private static MotorwaySignPanelData withEnabled(MotorwaySignPanelData source, boolean enabled) {
        return new MotorwaySignPanelData(
                enabled, source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), CartoucheType.NONE, "", source.graphic()
        );
    }

    private static MotorwaySignPanelData withLineCount(MotorwaySignPanelData source, int count) {
        return new MotorwaySignPanelData(
                source.enabled(), count,
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), CartoucheType.NONE, "", source.graphic()
        );
    }

    private static MotorwaySignPanelData withFont(
            MotorwaySignPanelData source,
            int index,
            RoadTextFont font
    ) {
        RoadTextFont[] fonts = {
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font()
        };
        fonts[Math.max(0, Math.min(3, index))] = font;
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                fonts[0], fonts[1], fonts[2], fonts[3],
                source.background(), CartoucheType.NONE, "", source.graphic()
        );
    }

    private static MotorwaySignPanelData withCartouche(
            MotorwaySignPanelData source,
            CartoucheType type,
            String text
    ) {
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(),
                type == null ? CartoucheType.NONE : type,
                text == null ? "" : text,
                source.graphic()
        );
    }

    private static MotorwaySignColor sanitizeBackground(MotorwaySignColor color) {
        return color == MotorwaySignColor.WHITE || color == MotorwaySignColor.GREEN
                ? color
                : MotorwaySignColor.BLUE;
    }

    private static MotorwaySignPanelData withBackground(
            MotorwaySignPanelData source,
            MotorwaySignColor background
    ) {
        MotorwaySignColor sanitized = sanitizeBackground(background);
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                forcedFontForColor(source.line1Font(), sanitized),
                forcedFontForColor(source.line2Font(), sanitized),
                forcedFontForColor(source.line3Font(), sanitized),
                forcedFontForColor(source.line4Font(), sanitized),
                sanitized, source.cartoucheType(), source.cartoucheText(), source.graphic()
        );
    }

    private static MotorwaySignPanelData withGraphic(
            MotorwaySignPanelData source,
            MotorwaySignGraphic graphic
    ) {
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), source.cartoucheType(), source.cartoucheText(),
                graphic == null ? MotorwaySignGraphic.NONE : graphic
        );
    }

    private static String graphicLabel(MotorwaySignGraphic graphic) {
        return switch (graphic == null ? MotorwaySignGraphic.NONE : graphic) {
            case NONE -> "Aucun";
            case DIAGONAL_RIGHT -> "Flèche ↗";
            case DIAGONAL_LEFT -> "Flèche ↖";
            case DOWN -> "Flèche ↓";
            case DOWN_DOUBLE -> "Deux flèches ↓";
            case EXIT -> "Sortie";
            case EXIT_LIST -> "Liste de sorties";
            case SCHEMATIC_RIGHT -> "Schéma droite";
            case SCHEMATIC_LEFT -> "Schéma gauche";
            case SERVICES -> "Services";
            case MOTORWAY -> "Autoroute";
            case JUNCTION -> "Bifurcation";
        };
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }
}
