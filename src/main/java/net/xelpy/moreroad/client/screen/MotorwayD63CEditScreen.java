package net.xelpy.moreroad.client.screen;

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

/** Éditeur D63c organisé comme les éditeurs modulaires D21/D61. */
public final class MotorwayD63CEditScreen extends Screen {

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
    private final MotorwaySignLineData[] baseLines =
            new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
    private final MotorwaySignPanelData[] panels =
            new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];

    private final EditBox[] baseFields = new EditBox[4];
    private final SignEditorUi.Rect[] baseFontRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] baseColorRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] baseStyleRects = new SignEditorUi.Rect[4];
    private final EditBox[] cityFields = new EditBox[4];
    private final EditBox[] distanceFields = new EditBox[4];
    private final SignEditorUi.Rect[] cityFontRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] panelTabRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] panelToggleRects = new SignEditorUi.Rect[4];
    private final SignEditorUi.Rect[] pageRects = new SignEditorUi.Rect[5];
    private final SignEditorUi.Rect[] formatRects = new SignEditorUi.Rect[4];

    private int selectedPanel;
    private int settingsPage;
    private boolean baseMode;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private CartoucheType secondCartoucheType = CartoucheType.NONE;

    private EditBox cartoucheField;
    private EditBox secondCartoucheField;
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect settingsRect;
    private SignEditorUi.Rect baseModeRect;
    private SignEditorUi.Rect whiteRect;
    private SignEditorUi.Rect greenRect;
    private SignEditorUi.Rect blueRect;
    private SignEditorUi.Rect graphicRect;
    private SignEditorUi.Rect cartoucheTypeRect;
    private SignEditorUi.Rect secondCartoucheTypeRect;
    private SignEditorUi.Rect modelGalleryRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public MotorwayD63CEditScreen(
            BlockPos blockPos,
            MotorwaySignLineData[] lines,
            MotorwaySignPanelData[] currentPanels
    ) {
        super(Component.literal("Éditeur de panneau D63c"));
        this.blockPos = blockPos.immutable();
        for (int index = 0; index < this.baseLines.length; index++) {
            MotorwaySignLineData fallback = index < MotorwaySignPreset.D63C.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(MotorwaySignPreset.D63C.getSlot(index))
                    : MotorwaySignLineData.empty();
            this.baseLines[index] = lines != null && index < lines.length && lines[index] != null
                    ? lines[index]
                    : fallback;
        }
        for (int index = 0; index < this.panels.length; index++) {
            MotorwaySignPanelData source = currentPanels != null && index < currentPanels.length
                    ? currentPanels[index]
                    : null;
            if (source == null) {
                source = MotorwaySignPanelData.disabled();
            }
            if (index == 0) {
                this.cartoucheType = source.cartoucheType();
            } else if (index == 1) {
                this.secondCartoucheType = source.cartoucheType();
            }
            this.panels[index] = sanitizePanel(source, index <= 1);
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

        int pad = s(16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsH = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int tabGap = s(8);
        int tabY = this.windowY + header;
        int tabsWidth = this.windowWidth - pad * 2;
        int tabWidth = (tabsWidth - tabGap * 3) / 4;
        for (int index = 0; index < this.panelTabRects.length; index++) {
            int x = this.windowX + pad + index * (tabWidth + tabGap);
            this.panelTabRects[index] = new SignEditorUi.Rect(
                    x, tabY, tabWidth, SignEditorUi.safeControlHeight(this.font, s(32))
            );
            this.panelToggleRects[index] = SignEditorUi.tabToggleRect(
                    this.panelTabRects[index], this.font
            );
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
        SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, 5, pageHeight, s(6));
        System.arraycopy(pages, 0, this.pageRects, 0, this.pageRects.length);
        this.settingsRect = new SignEditorUi.Rect(
                rightX, bodyY + pageHeight + s(8), rightW,
                Math.max(s(150), bodyH - pageHeight - s(16))
        );

        initTextControls();
        initPageControls();

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(82, s(145));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH
        );
        this.applyRect = new SignEditorUi.Rect(
                this.cancelRect.x() - s(10) - actionW, actionY, actionW, actionH
        );
        int headerButtonH = SignEditorUi.safeControlHeight(this.font, s(26));
        int galleryW = Math.max(s(190), this.font.width("Choisir le modèle") + s(30));
        int baseW = Math.max(s(225), this.font.width("Base D63c • sortie + distance") + s(30));
        int headerButtonY = this.windowY + s(10);
        this.modelGalleryRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - galleryW,
                headerButtonY, galleryW, headerButtonH
        );
        this.baseModeRect = new SignEditorUi.Rect(
                this.modelGalleryRect.x() - s(8) - baseW,
                headerButtonY, baseW, headerButtonH
        );

        loadPanelIntoWidgets();
        updateVisibility();
        this.setInitialFocus(this.baseFields[2]);
    }

    private void initTextControls() {
        int innerX = this.settingsRect.x() + s(12);
        int innerW = this.settingsRect.width() - s(24);
        int fieldH = SignEditorUi.safeControlHeight(this.font, s(26));
        int rowGap = s(12);
        int fontW = Math.max(s(84), Math.round(innerW * 0.18F));
        int colorW = Math.max(s(76), Math.round(innerW * 0.16F));
        int distanceW = Math.max(s(62), Math.round(innerW * 0.15F));
        int controlGap = s(7);
        int firstY = this.settingsRect.y() + s(54);

        int baseTextW = innerW - fontW - colorW - controlGap * 2;
        for (int index = 0; index < 4; index++) {
            int row = index < 2 ? index : 0;
            int y = firstY + row * (fieldH + rowGap);
            this.baseFields[index] = new EditBox(
                    this.font, innerX, y, baseTextW, fieldH,
                    Component.literal("Champ D63c " + (index + 1))
            );
            this.baseFields[index].setMaxLength(64);
            this.baseFields[index].setValue(this.baseLines[index].text());
            this.addRenderableWidget(this.baseFields[index]);
            this.baseFontRects[index] = new SignEditorUi.Rect(
                    innerX + baseTextW + controlGap, y, fontW, fieldH
            );
            this.baseColorRects[index] = new SignEditorUi.Rect(
                    this.baseFontRects[index].x() + fontW + controlGap, y, colorW, fieldH
            );
        }

        int cityW = innerW - fontW - distanceW - controlGap * 2;
        for (int index = 0; index < 4; index++) {
            int y = firstY + index * (fieldH + rowGap);
            this.cityFields[index] = new EditBox(
                    this.font, innerX, y, cityW, fieldH,
                    Component.literal("Ville " + (index + 1))
            );
            this.cityFields[index].setMaxLength(64);
            this.cityFields[index].setHint(Component.literal("Ville " + (index + 1)));
            this.addRenderableWidget(this.cityFields[index]);
            this.cityFontRects[index] = new SignEditorUi.Rect(
                    innerX + cityW + controlGap, y, fontW, fieldH
            );
            this.distanceFields[index] = new EditBox(
                    this.font, this.cityFontRects[index].x() + fontW + controlGap,
                    y, distanceW, fieldH, Component.literal("Km " + (index + 1))
            );
            this.distanceFields[index].setMaxLength(8);
            this.distanceFields[index].setHint(Component.literal("Km"));
            this.addRenderableWidget(this.distanceFields[index]);
        }
    }

    private void initPageControls() {
        int innerX = this.settingsRect.x() + s(12);
        int innerW = this.settingsRect.width() - s(24);
        int firstY = this.settingsRect.y() + s(58);
        int controlH = SignEditorUi.safeControlHeight(this.font, s(28));
        int gap = s(8);
        int third = (innerW - gap * 2) / 3;
        this.whiteRect = new SignEditorUi.Rect(innerX, firstY, third, controlH);
        this.greenRect = new SignEditorUi.Rect(innerX + third + gap, firstY, third, controlH);
        this.blueRect = new SignEditorUi.Rect(
                this.greenRect.x() + third + gap, firstY,
                innerX + innerW - (this.greenRect.x() + third + gap), controlH
        );
        this.graphicRect = new SignEditorUi.Rect(innerX, firstY, innerW, controlH);

        int baseStyleGap = s(10);
        int baseStyleH = SignEditorUi.safeControlHeight(this.font, s(30));
        for (int index = 0; index < this.baseStyleRects.length; index++) {
            this.baseStyleRects[index] = new SignEditorUi.Rect(
                    innerX, firstY + index * (baseStyleH + baseStyleGap), innerW, baseStyleH
            );
        }

        int typeW = Math.max(s(150), Math.round(innerW * 0.42F));
        this.cartoucheTypeRect = new SignEditorUi.Rect(innerX, firstY, typeW, controlH);
        this.cartoucheField = new EditBox(
                this.font, innerX + typeW + gap, firstY,
                innerW - typeW - gap, controlH, Component.literal("Texte du cartouche")
        );
        this.cartoucheField.setMaxLength(24);
        this.cartoucheField.setHint(Component.literal("Ex. M 337"));
        this.cartoucheField.setValue(this.panels[0].cartoucheText());
        this.addRenderableWidget(this.cartoucheField);

        int secondY = firstY + controlH + s(12);
        this.secondCartoucheTypeRect = new SignEditorUi.Rect(innerX, secondY, typeW, controlH);
        this.secondCartoucheField = new EditBox(
                this.font, innerX + typeW + gap, secondY,
                innerW - typeW - gap, controlH, Component.literal("Texte du second cartouche")
        );
        this.secondCartoucheField.setMaxLength(24);
        this.secondCartoucheField.setHint(Component.literal("Ex. E 60"));
        this.secondCartoucheField.setValue(this.panels[1].cartoucheText());
        this.addRenderableWidget(this.secondCartoucheField);

        int formatW = (innerW - gap * 3) / 4;
        for (int index = 0; index < this.formatRects.length; index++) {
            this.formatRects[index] = new SignEditorUi.Rect(
                    innerX + index * (formatW + gap), firstY, formatW, controlH
            );
        }
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
                "D63c", "Éditeur de panneau D63c",
                compactUi() ? "" : "Configuration en direct"
        );
        for (int index = 0; index < this.panelTabRects.length; index++) {
            boolean originalDestination = index < 2;
            MotorwaySignPanelData panel = originalDestination
                    ? null
                    : (!this.baseMode && index == this.selectedPanel
                    ? currentPanelFromWidgets()
                    : this.panels[index - 2]);
            String detail = switch (index) {
                case 0 -> this.baseFields[2].getValue().isBlank() ? "BLOIS" : this.baseFields[2].getValue();
                case 1 -> this.baseFields[3].getValue().isBlank() ? "SAUMUR" : this.baseFields[3].getValue();
                case 2 -> panel.lineCount() + " destination(s)";
                default -> "optionnel • " + panel.lineCount() + " ligne(s)";
            };
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.panelTabRects[index],
                    compactUi()
                            ? "Panneau " + (index + 1)
                            : "Panneau " + (index + 1) + "  •  " + detail,
                    !this.baseMode && index == this.selectedPanel,
                    true, mouseX, mouseY
            );
            SignEditorUi.drawModernToggle(
                    graphics, this.font, this.panelToggleRects[index],
                    "", "", originalDestination || panel.enabled(), !originalDestination, mouseX, mouseY
            );
        }

        drawPreview(graphics);
        SignEditorUi.drawPageTabs(
                graphics, this.font, this.pageRects,
                new String[]{"Texte", "Style", "Flèche", "Cart.", "Format"},
                this.settingsPage, mouseX, mouseY
        );
        if (this.baseMode) {
            drawBasePage(graphics, mouseX, mouseY);
        } else {
            drawPanelPage(graphics, mouseX, mouseY);
        }

        SignEditorUi.drawModernButton(
                graphics, this.font, this.baseModeRect,
                "Base D63c • sortie + distance", this.baseMode, true, mouseX, mouseY
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.modelGalleryRect,
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

    private void drawBasePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        switch (this.settingsPage) {
            case 0 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "1. SORTIE ET DISTANCE", compactUi() ? "" : "Registre blanc supérieur du D63c"
                );
                for (int index = 0; index < 2; index++) {
                    SignEditorUi.drawModernButton(
                            graphics, this.font, this.baseFontRects[index],
                            SignEditorUi.fontLabel(this.baseLines[index].font()),
                            false, true, mouseX, mouseY
                    );
                    SignEditorUi.drawModernButton(
                            graphics, this.font, this.baseColorRects[index],
                            this.baseLines[index].color().getDisplayName(),
                            false, true, mouseX, mouseY
                    );
                }
            }
            case 1 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "2. STYLE DU REGISTRE", compactUi() ? "" : "Fond blanc réglementaire"
                );
                for (int index = 0; index < 2; index++) {
                    SignEditorUi.drawModernButton(
                            graphics, this.font, this.baseStyleRects[index],
                            MotorwaySignPreset.D63C.getSlot(index).label() + " : "
                                    + this.baseLines[index].color().getDisplayName(),
                            true, true, mouseX, mouseY
                    );
                }
            }
            case 2 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "3. FLÈCHE ET SYMBOLE", compactUi() ? "" : "Symbole réglementaire du modèle"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.graphicRect,
                        "Symbole de sortie SE2b — modèle D63c", true, false, mouseX, mouseY
                );
            }
            case 3 -> drawCartouchePage(graphics, mouseX, mouseY);
            default -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "5. FORMAT", compactUi() ? "" : "Structure réglementaire du modèle sélectionné"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.graphicRect,
                        "Structure D63c", true, false, mouseX, mouseY
                );
            }
        }
    }

    private void drawPanelPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.selectedPanel < 2) {
            drawOriginalDestinationPage(graphics, mouseX, mouseY);
            return;
        }
        MotorwaySignPanelData panel = currentPanelFromWidgets();
        switch (this.settingsPage) {
            case 0 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "1. CONTENU", compactUi() ? "" : "Villes, police et kilométrage"
                );
                for (int index = 0; index < panel.lineCount(); index++) {
                    SignEditorUi.drawModernButton(
                            graphics, this.font, this.cityFontRects[index],
                            SignEditorUi.fontLabel(panel.font(index)), false, true, mouseX, mouseY
                    );
                }
            }
            case 1 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "2. STYLE", compactUi() ? "" : "Fond blanc, vert ou bleu"
                );
                SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc",
                        panel.background() == MotorwaySignColor.WHITE, true, mouseX, mouseY);
                SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert",
                        panel.background() == MotorwaySignColor.GREEN, true, mouseX, mouseY);
                SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu",
                        panel.background() == MotorwaySignColor.BLUE, true, mouseX, mouseY);
            }
            case 2 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "3. FLÈCHE", compactUi() ? "" : "Symbole de la pancarte sélectionnée"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.graphicRect,
                        "Symbole : " + graphicLabel(panel.graphic()),
                        panel.graphic() != MotorwaySignGraphic.NONE, true, mouseX, mouseY
                );
            }
            case 3 -> drawCartouchePage(graphics, mouseX, mouseY);
            default -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "5. FORMAT", compactUi() ? "" : "De 1 à 4 villes sur la même pancarte"
                );
                for (int index = 0; index < this.formatRects.length; index++) {
                    SignEditorUi.drawModernButton(
                            graphics, this.font, this.formatRects[index],
                            Integer.toString(index + 1), panel.lineCount() == index + 1,
                            true, mouseX, mouseY
                    );
                }
            }
        }
    }

    private void drawOriginalDestinationPage(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY
    ) {
        int lineIndex = this.selectedPanel + 2;
        String name = this.selectedPanel == 0 ? "BLOIS" : "SAUMUR";
        switch (this.settingsPage) {
            case 0 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "1. PANNEAU " + (this.selectedPanel + 1),
                        compactUi() ? "" : "Destination " + name + " du modèle D63c"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.baseFontRects[lineIndex],
                        SignEditorUi.fontLabel(this.baseLines[lineIndex].font()),
                        false, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.baseColorRects[lineIndex],
                        this.baseLines[lineIndex].color().getDisplayName(),
                        false, true, mouseX, mouseY
                );
            }
            case 1 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "2. STYLE", compactUi() ? "" : "Fond blanc, vert ou bleu"
                );
                MotorwaySignColor color = allowedBaseColor(this.baseLines[lineIndex].color());
                SignEditorUi.drawModernButton(graphics, this.font, this.whiteRect, "Blanc",
                        color == MotorwaySignColor.WHITE, true, mouseX, mouseY);
                SignEditorUi.drawModernButton(graphics, this.font, this.greenRect, "Vert",
                        color == MotorwaySignColor.GREEN, true, mouseX, mouseY);
                SignEditorUi.drawModernButton(graphics, this.font, this.blueRect, "Bleu",
                        color == MotorwaySignColor.BLUE, true, mouseX, mouseY);
            }
            case 2 -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "3. FLÈCHE", compactUi() ? "" : "Aucun symbole sur ce registre"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.graphicRect,
                        "Aucun symbole", false, false, mouseX, mouseY
                );
            }
            case 3 -> drawCartouchePage(graphics, mouseX, mouseY);
            default -> {
                SignEditorUi.drawModernSection(
                        graphics, this.font, this.settingsRect,
                        "5. FORMAT", compactUi() ? "" : "Une destination sur ce panneau"
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.graphicRect,
                        "1 ville", true, false, mouseX, mouseY
                );
            }
        }
    }

    private void drawCartouchePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.settingsRect,
                "4. CARTOUCHES SUPÉRIEURS", compactUi() ? "" : "Jusqu'à deux grands cartouches"
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.cartoucheTypeRect,
                "Cartouche 1 : " + SignEditorUi.cartoucheLabel(this.cartoucheType),
                this.cartoucheType.isVisible(), true, mouseX, mouseY
        );
        SignEditorUi.drawModernButton(
                graphics, this.font, this.secondCartoucheTypeRect,
                "Cartouche 2 : " + SignEditorUi.cartoucheLabel(this.secondCartoucheType),
                this.secondCartoucheType.isVisible(), true, mouseX, mouseY
        );
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.previewRect,
                "APERÇU EN DIRECT", compactUi() ? "" : "D63c • ensemble configuré"
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(16);
        int width = this.previewRect.width() - pad * 2;
        int height = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + width, y + height, 0xFFF0F3F6);

        MotorwaySignPanelData[] previewPanels = previewPanels();
        List<MotorwaySignPanelData> enabled = new ArrayList<>();
        for (MotorwaySignPanelData panel : previewPanels) {
            if (panel.enabled() && panel.hasPanelContent()) {
                enabled.add(panel);
            }
        }
        int gap = Math.max(3, s(5));
        int panelWidth = Math.min(width - s(30), Math.max(s(220), Math.round(width * 0.80F)));
        boolean firstCartouche = this.cartoucheType.isVisible();
        boolean secondCartouche = this.secondCartoucheType.isVisible();
        int cartH = firstCartouche || secondCartouche ? s(42) : 0;
        int totalH = s(48 + 42 + 42) + gap * 2;
        if (cartH > 0) {
            totalH += cartH + gap;
        }
        for (MotorwaySignPanelData panel : enabled) {
            totalH += gap + (panel.lineCount() >= 3
                    ? s(38 + panel.lineCount() * 31)
                    : s(25 + panel.lineCount() * 18));
        }
        int centerX = x + width / 2;
        int cursorY = y + Math.max(s(16), (height - totalH) / 2);
        graphics.fill(centerX - s(4), cursorY + totalH - s(4),
                centerX + s(4), y + height - s(7), 0xFF2C2C2C);

        if (cartH > 0) {
            int visibleCount = (firstCartouche ? 1 : 0) + (secondCartouche ? 1 : 0);
            int cartGap = visibleCount == 2 ? gap : 0;
            int cartW = visibleCount == 2
                    ? Math.min((panelWidth - cartGap) / 2, s(180))
                    : Math.min(panelWidth, s(220));
            int cartX = centerX - (visibleCount * cartW + cartGap) / 2;
            if (firstCartouche) {
                drawPlate(graphics, cartX, cursorY, cartW, cartH, cartoucheColor(this.cartoucheType));
                drawCenteredText(graphics, this.cartoucheField.getValue(), RoadTextFont.L1,
                        cartoucheColor(this.cartoucheType), cartX, cursorY, cartW, cartH);
                cartX += cartW + cartGap;
            }
            if (secondCartouche) {
                drawPlate(graphics, cartX, cursorY, cartW, cartH, cartoucheColor(this.secondCartoucheType));
                drawCenteredText(graphics, this.secondCartoucheField.getValue(), RoadTextFont.L1,
                        cartoucheColor(this.secondCartoucheType), cartX, cursorY, cartW, cartH);
            }
            cursorY += cartH + gap;
        }

        int topH = s(48);
        drawPlate(graphics, centerX - panelWidth / 2, cursorY, panelWidth, topH, MotorwaySignColor.WHITE);
        drawLeftText(graphics, this.baseFields[0].getValue(), this.baseLines[0].font(), MotorwaySignColor.WHITE,
                centerX - panelWidth / 2, cursorY, panelWidth / 2, topH);
        drawRightText(graphics, this.baseFields[1].getValue(), this.baseLines[1].font(), MotorwaySignColor.WHITE,
                centerX, cursorY, panelWidth / 2, topH);
        cursorY += topH + gap;

        int destinationH = s(42);
        for (int index = 2; index <= 3; index++) {
            MotorwaySignColor color = allowedBaseColor(this.baseLines[index].color());
            drawPlate(graphics, centerX - panelWidth / 2, cursorY, panelWidth, destinationH, color);
            drawLeftText(graphics, this.baseFields[index].getValue(), this.baseLines[index].font(), color,
                    centerX - panelWidth / 2, cursorY, panelWidth, destinationH);
            cursorY += destinationH + (index == 2 ? gap : 0);
        }

        for (MotorwaySignPanelData panel : enabled) {
            cursorY += gap;
            int panelH = panel.lineCount() >= 3
                    ? s(38 + panel.lineCount() * 31)
                    : s(25 + panel.lineCount() * 18);
            drawPlate(graphics, centerX - panelWidth / 2, cursorY, panelWidth, panelH, panel.background());
            int lineH = panelH / panel.lineCount();
            for (int line = 0; line < panel.lineCount(); line++) {
                drawPanelLine(graphics, panel, line, centerX - panelWidth / 2,
                        cursorY + line * lineH, panelWidth,
                        line == panel.lineCount() - 1 ? panelH - line * lineH : lineH);
            }
            cursorY += panelH;
        }
    }

    private void drawPlate(GuiGraphicsExtractor graphics, int x, int y, int width, int height, MotorwaySignColor color) {
        graphics.fill(x, y, x + width, y + height, 0xFFD7D7D2);
        int border = Math.max(2, s(3));
        graphics.fill(x + border, y + border, x + width - border, y + height - border, color.getArgb());
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String value, RoadTextFont roadFont,
                                  MotorwaySignColor color, int x, int y, int width, int height) {
        Component text = roadText(value, roadFont, width - s(12));
        graphics.text(this.font, text, x + (width - this.font.width(text)) / 2,
                y + (height - this.font.lineHeight) / 2, color.getTextArgb(), false);
    }

    private void drawLeftText(GuiGraphicsExtractor graphics, String value, RoadTextFont roadFont,
                              MotorwaySignColor color, int x, int y, int width, int height) {
        Component text = roadText(value, roadFont, width - s(20));
        graphics.text(this.font, text, x + s(10), y + (height - this.font.lineHeight) / 2,
                color.getTextArgb(), false);
    }

    private void drawRightText(GuiGraphicsExtractor graphics, String value, RoadTextFont roadFont,
                               MotorwaySignColor color, int x, int y, int width, int height) {
        Component text = roadText(value, roadFont, width - s(20));
        graphics.text(this.font, text, x + width - this.font.width(text) - s(18),
                y + (height - this.font.lineHeight) / 2, color.getTextArgb(), false);
    }

    private void drawPanelLine(GuiGraphicsExtractor graphics, MotorwaySignPanelData panel,
                               int index, int x, int y, int width, int height) {
        Component distance = roadText(panel.distance(index), panel.font(index), width / 3);
        int reserve = panel.distance(index).isBlank() ? 0 : this.font.width(distance) + s(20);
        Component city = roadText(panel.line(index), panel.font(index), width - reserve - s(22));
        int textY = y + (height - this.font.lineHeight) / 2;
        graphics.text(this.font, city, x + s(11), textY, panel.background().getTextArgb(), false);
        if (!panel.distance(index).isBlank()) {
            graphics.text(this.font, distance, x + width - this.font.width(distance) - s(10),
                    textY, panel.background().getTextArgb(), false);
        }
    }

    private Component roadText(String value, RoadTextFont roadFont, int maxWidth) {
        String fitted = SignEditorUi.fitText(this.font, value == null ? "" : value, Math.max(8, maxWidth));
        return Component.literal(fitted).withStyle(
                Style.EMPTY.withFont(roadFontResource(roadFont))
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();
            if (this.modelGalleryRect.contains(x, y)) {
                storePanel();
                syncBaseLinesFromFields();
                net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                        new MotorwayPresetGalleryScreen(
                                this,
                                this.blockPos,
                                MotorwaySignPreset.D63C,
                                previewPanels()
                        )
                );
                return true;
            }
            if (this.baseModeRect.contains(x, y)) {
                storePanel();
                this.baseMode = true;
                updateVisibility();
                this.setInitialFocus(this.baseFields[0]);
                return true;
            }
            for (int index = 0; index < this.panelTabRects.length; index++) {
                if (index >= 2 && this.panelToggleRects[index].contains(x, y)) {
                    int customIndex = index - 2;
                    if (!this.baseMode && index == this.selectedPanel) {
                        MotorwaySignPanelData current = currentPanelFromWidgets();
                        this.panels[customIndex] = withEnabled(current, !current.enabled());
                        loadPanelIntoWidgets();
                    } else {
                        this.panels[customIndex] = withEnabled(
                                this.panels[customIndex], !this.panels[customIndex].enabled()
                        );
                    }
                    return true;
                }
                if (this.panelTabRects[index].contains(x, y)) {
                    storePanel();
                    this.selectedPanel = index;
                    this.baseMode = false;
                    loadPanelIntoWidgets();
                    updateVisibility();
                    if (index < 2) {
                        this.setInitialFocus(this.baseFields[index + 2]);
                    } else {
                        this.setInitialFocus(this.cityFields[0]);
                    }
                    return true;
                }
            }
            for (int index = 0; index < this.pageRects.length; index++) {
                if (this.pageRects[index].contains(x, y)) {
                    storePanel();
                    this.settingsPage = index;
                    updateVisibility();
                    return true;
                }
            }

            if (this.baseMode) {
                if (this.settingsPage == 0) {
                    for (int index = 0; index < 4; index++) {
                        if (this.baseFontRects[index].contains(x, y)) {
                            this.baseLines[index] = withFont(this.baseLines[index], RoadTextFont.nextForBackground(
                                    this.baseLines[index].font(), !this.baseLines[index].color().isLight()
                            ));
                            return true;
                        }
                        if (this.baseColorRects[index].contains(x, y)) {
                            this.baseLines[index] = withColor(this.baseLines[index], nextBaseColor(this.baseLines[index].color()));
                            return true;
                        }
                    }
                } else if (this.settingsPage == 1) {
                    for (int index = 0; index < 4; index++) {
                        if (this.baseStyleRects[index].contains(x, y)) {
                            this.baseLines[index] = withColor(this.baseLines[index], nextBaseColor(this.baseLines[index].color()));
                            return true;
                        }
                    }
                }
            } else if (this.selectedPanel < 2) {
                int lineIndex = this.selectedPanel + 2;
                if (this.settingsPage == 0) {
                    if (this.baseFontRects[lineIndex].contains(x, y)) {
                        this.baseLines[lineIndex] = withFont(
                                this.baseLines[lineIndex], RoadTextFont.nextForBackground(
                                        this.baseLines[lineIndex].font(), !this.baseLines[lineIndex].color().isLight()
                                )
                        );
                        return true;
                    }
                    if (this.baseColorRects[lineIndex].contains(x, y)) {
                        this.baseLines[lineIndex] = withColor(
                                this.baseLines[lineIndex], nextBaseColor(this.baseLines[lineIndex].color())
                        );
                        return true;
                    }
                } else if (this.settingsPage == 1) {
                    if (this.whiteRect.contains(x, y)) {
                        this.baseLines[lineIndex] = withColor(this.baseLines[lineIndex], MotorwaySignColor.WHITE);
                        return true;
                    }
                    if (this.greenRect.contains(x, y)) {
                        this.baseLines[lineIndex] = withColor(this.baseLines[lineIndex], MotorwaySignColor.GREEN);
                        return true;
                    }
                    if (this.blueRect.contains(x, y)) {
                        this.baseLines[lineIndex] = withColor(this.baseLines[lineIndex], MotorwaySignColor.BLUE);
                        return true;
                    }
                }
            } else {
                MotorwaySignPanelData current = currentPanelFromWidgets();
                if (this.settingsPage == 0) {
                    for (int index = 0; index < current.lineCount(); index++) {
                        if (this.cityFontRects[index].contains(x, y)) {
                            this.panels[selectedCustomPanelIndex()] = withPanelFont(current, index,
                                    RoadTextFont.nextForBackground(current.font(index), !current.background().isLight()));
                            loadPanelIntoWidgets();
                            return true;
                        }
                    }
                } else if (this.settingsPage == 1) {
                    if (this.whiteRect.contains(x, y)) {
                        this.panels[selectedCustomPanelIndex()] = withBackground(current, MotorwaySignColor.WHITE);
                        loadPanelIntoWidgets();
                        return true;
                    }
                    if (this.greenRect.contains(x, y)) {
                        this.panels[selectedCustomPanelIndex()] = withBackground(current, MotorwaySignColor.GREEN);
                        loadPanelIntoWidgets();
                        return true;
                    }
                    if (this.blueRect.contains(x, y)) {
                        this.panels[selectedCustomPanelIndex()] = withBackground(current, MotorwaySignColor.BLUE);
                        loadPanelIntoWidgets();
                        return true;
                    }
                } else if (this.settingsPage == 2 && this.graphicRect.contains(x, y)) {
                    MotorwaySignGraphic[] values = MotorwaySignGraphic.values();
                    this.panels[selectedCustomPanelIndex()] = withGraphic(
                            current, values[(current.graphic().ordinal() + 1) % values.length]
                    );
                    loadPanelIntoWidgets();
                    return true;
                } else if (this.settingsPage == 4) {
                    for (int index = 0; index < 4; index++) {
                        if (this.formatRects[index].contains(x, y)) {
                            this.panels[selectedCustomPanelIndex()] = withLineCount(current, index + 1);
                            loadPanelIntoWidgets();
                            updateVisibility();
                            return true;
                        }
                    }
                }
            }
            if (this.settingsPage == 3 && this.cartoucheTypeRect.contains(x, y)) {
                this.cartoucheType = this.cartoucheType.next();
                updateVisibility();
                return true;
            }
            if (this.settingsPage == 3 && this.secondCartoucheTypeRect.contains(x, y)) {
                this.secondCartoucheType = this.secondCartoucheType.next();
                updateVisibility();
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
        for (int index = 0; index < 4; index++) {
            boolean fullBaseVisible = this.baseMode && this.settingsPage == 0;
            boolean originalDestinationVisible = !this.baseMode && this.settingsPage == 0
                    && this.selectedPanel < 2 && index == this.selectedPanel + 2;
            boolean baseVisible = fullBaseVisible || originalDestinationVisible;
            this.baseFields[index].visible = baseVisible;
            this.baseFields[index].active = baseVisible;

            boolean panelVisible = !this.baseMode && this.selectedPanel >= 2
                    && this.settingsPage == 0
                    && index < currentPanelFromWidgets().lineCount();
            this.cityFields[index].visible = panelVisible;
            this.cityFields[index].active = panelVisible;
            this.distanceFields[index].visible = panelVisible;
            this.distanceFields[index].active = panelVisible;
        }
        boolean cartVisible = this.settingsPage == 3;
        this.cartoucheField.visible = cartVisible;
        this.cartoucheField.active = cartVisible && this.cartoucheType.isVisible();
        this.secondCartoucheField.visible = cartVisible;
        this.secondCartoucheField.active = cartVisible && this.secondCartoucheType.isVisible();
    }

    private void storePanel() {
        if (!this.baseMode && this.selectedPanel >= 2 && this.cityFields[0] != null) {
            this.panels[selectedCustomPanelIndex()] = currentPanelFromWidgets();
        }
    }

    private void loadPanelIntoWidgets() {
        if (this.cityFields[0] == null) {
            return;
        }
        if (this.selectedPanel < 2) {
            return;
        }
        MotorwaySignPanelData panel = this.panels[selectedCustomPanelIndex()];
        for (int index = 0; index < 4; index++) {
            this.cityFields[index].setValue(panel.line(index));
            this.distanceFields[index].setValue(panel.distance(index));
        }
    }

    private MotorwaySignPanelData currentPanelFromWidgets() {
        MotorwaySignPanelData stored = this.panels[selectedCustomPanelIndex()];
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
                stored.background(), stored.cartoucheType(), stored.cartoucheText(), stored.graphic()
        );
    }

    private MotorwaySignPanelData[] previewPanels() {
        MotorwaySignPanelData[] result = this.panels.clone();
        if (!this.baseMode && this.selectedPanel >= 2) {
            result[selectedCustomPanelIndex()] = currentPanelFromWidgets();
        }
        return result;
    }

    private void save() {
        storePanel();
        syncBaseLinesFromFields();
        this.panels[0] = withCartouche(
                this.panels[0], this.cartoucheType, this.cartoucheField.getValue()
        );
        this.panels[1] = withCartouche(
                this.panels[1], this.secondCartoucheType, this.secondCartoucheField.getValue()
        );
        MotorwaySignServiceIcon[] noServices = new MotorwaySignServiceIcon[MotorwaySignServiceIcon.MAX_SLOTS];
        java.util.Arrays.fill(noServices, MotorwaySignServiceIcon.NONE);
        ClientPacketDistributor.sendToServer(new UpdateMotorwaySignPayload(
                this.blockPos, MotorwaySignPreset.D63C.getSerializedName(),
                this.baseLines,
                false,
                this.panels,
                noServices
        ));
        this.onClose();
    }

    private int selectedCustomPanelIndex() {
        return Math.max(0, Math.min(1, this.selectedPanel - 2));
    }

    private void syncBaseLinesFromFields() {
        for (int index = 0; index < 4; index++) {
            this.baseLines[index] = new MotorwaySignLineData(
                    this.baseFields[index].getValue(),
                    this.baseLines[index].font(), this.baseLines[index].color()
            );
        }
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private static MotorwaySignPanelData sanitizePanel(MotorwaySignPanelData panel, boolean keepCartouche) {
        MotorwaySignColor background = allowedPanelColor(panel.background());
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                background,
                keepCartouche ? panel.cartoucheType() : CartoucheType.NONE,
                keepCartouche ? panel.cartoucheText() : "",
                panel.graphic()
        );
    }

    private static MotorwaySignPanelData withEnabled(MotorwaySignPanelData panel, boolean enabled) {
        return copyPanel(panel, enabled, panel.lineCount(), panel.background(), panel.graphic(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font());
    }

    private static MotorwaySignPanelData withLineCount(MotorwaySignPanelData panel, int count) {
        return copyPanel(panel, panel.enabled(), count, panel.background(), panel.graphic(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font());
    }

    private static MotorwaySignPanelData withBackground(MotorwaySignPanelData panel, MotorwaySignColor color) {
        MotorwaySignColor allowed = allowedPanelColor(color);
        return copyPanel(panel, panel.enabled(), panel.lineCount(), allowed, panel.graphic(),
                forcedFontForColor(panel.line1Font(), allowed),
                forcedFontForColor(panel.line2Font(), allowed),
                forcedFontForColor(panel.line3Font(), allowed),
                forcedFontForColor(panel.line4Font(), allowed));
    }

    private static MotorwaySignPanelData withGraphic(MotorwaySignPanelData panel, MotorwaySignGraphic graphic) {
        return copyPanel(panel, panel.enabled(), panel.lineCount(), panel.background(), graphic,
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font());
    }

    private static MotorwaySignPanelData withPanelFont(
            MotorwaySignPanelData panel, int index, RoadTextFont font
    ) {
        RoadTextFont[] fonts = {
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font()
        };
        fonts[Math.max(0, Math.min(3, index))] = font;
        return copyPanel(panel, panel.enabled(), panel.lineCount(), panel.background(), panel.graphic(),
                fonts[0], fonts[1], fonts[2], fonts[3]);
    }

    private static MotorwaySignPanelData copyPanel(
            MotorwaySignPanelData panel, boolean enabled, int count,
            MotorwaySignColor background, MotorwaySignGraphic graphic,
            RoadTextFont font1, RoadTextFont font2, RoadTextFont font3, RoadTextFont font4
    ) {
        return new MotorwaySignPanelData(
                enabled, count,
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                font1, font2, font3, font4,
                background, panel.cartoucheType(), panel.cartoucheText(), graphic
        );
    }

    private static MotorwaySignPanelData withCartouche(
            MotorwaySignPanelData panel, CartoucheType type, String text
    ) {
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                panel.background(), type, text, panel.graphic()
        );
    }

    private static MotorwaySignLineData withFont(MotorwaySignLineData line, RoadTextFont font) {
        return new MotorwaySignLineData(line.text(), font, line.color());
    }

    private static MotorwaySignLineData withColor(MotorwaySignLineData line, MotorwaySignColor color) {
        return new MotorwaySignLineData(line.text(), forcedFontForColor(line.font(), color), color);
    }

    private static MotorwaySignColor nextBaseColor(MotorwaySignColor current) {
        MotorwaySignColor allowed = allowedBaseColor(current);
        return switch (allowed) {
            case WHITE -> MotorwaySignColor.GREEN;
            case GREEN -> MotorwaySignColor.BLUE;
            default -> MotorwaySignColor.WHITE;
        };
    }

    private static MotorwaySignColor allowedBaseColor(MotorwaySignColor color) {
        return color == MotorwaySignColor.GREEN || color == MotorwaySignColor.BLUE
                ? color
                : MotorwaySignColor.WHITE;
    }

    private static MotorwaySignColor allowedPanelColor(MotorwaySignColor color) {
        return color == MotorwaySignColor.GREEN || color == MotorwaySignColor.WHITE
                ? color
                : MotorwaySignColor.BLUE;
    }

    private static MotorwaySignColor cartoucheColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E44 -> MotorwaySignColor.WHITE;
            case E47 -> MotorwaySignColor.METROPOLITAN_BLUE;
            default -> MotorwaySignColor.BLUE;
        };
    }

    private static String graphicLabel(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case NONE -> "Aucun";
            case DIAGONAL_LEFT -> "Flèche gauche";
            case DIAGONAL_RIGHT -> "Flèche droite";
            case DOWN -> "Flèche basse";
            case DOWN_DOUBLE -> "Double flèche";
            case EXIT -> "Sortie";
            case EXIT_LIST -> "Liste de sortie";
            case SCHEMATIC_LEFT -> "Schéma gauche";
            case SCHEMATIC_RIGHT -> "Schéma droite";
            case JUNCTION -> "Bifurcation";
            case MOTORWAY -> "Autoroute";
            case SERVICES -> "Services";
        };
    }
}
