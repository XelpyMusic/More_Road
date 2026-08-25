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
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.MotorwaySignRole;
import net.xelpy.moreroad.block.custom.MotorwaySignSlot;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.network.UpdateMotorwaySignPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Éditeur commun aux 62 modèles autoroutiers issus de l'archive SVG. */
public class MotorwaySignEditScreen extends Screen {

    private static final int MAX_TEXT_LENGTH = 64;
    private static final FontDescription.Resource ROAD_FONT_L1 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
    );
    private static final FontDescription.Resource ROAD_FONT_L4 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
    );

    private final BlockPos blockPos;
    private MotorwaySignPreset preset;
    private final String[] initialTexts = new String[MotorwaySignBlockEntity.MAX_SLOTS];
    private final RoadTextFont[] lineFonts = new RoadTextFont[MotorwaySignBlockEntity.MAX_SLOTS];
    private final MotorwaySignColor[] lineColors = new MotorwaySignColor[MotorwaySignBlockEntity.MAX_SLOTS];
    private final EditBox[] fields = new EditBox[MotorwaySignBlockEntity.MAX_SLOTS];
    private final SignEditorUi.Rect[] fontRects = new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_SLOTS];
    private final SignEditorUi.Rect[] colorRects = new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_SLOTS];
    /** Onglet de réglage affiché : contenu du modèle ou l'une de ses pancartes. */
    private boolean customMode;
    private final MotorwaySignPanelData[] customPanels =
            new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private int selectedCustomPanel;
    private EditBox customLine1Field;
    private EditBox customLine2Field;
    private EditBox customLine3Field;
    private EditBox customLine4Field;
    private EditBox customDistance1Field;
    private EditBox customDistance2Field;
    private EditBox customDistance3Field;
    private EditBox customDistance4Field;
    private EditBox customCartoucheField;
    private EditBox contentCartoucheField;
    private final SignEditorUi.Rect[] customTabRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final SignEditorUi.Rect[] customEnabledRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final SignEditorUi.Rect[] customPageRects = new SignEditorUi.Rect[4];
    private int customSettingsPage;
    private SignEditorUi.Rect modeRect;
    private SignEditorUi.Rect customDoubleLineRect;
    private SignEditorUi.Rect customLine1FontRect;
    private SignEditorUi.Rect customLine2FontRect;
    private SignEditorUi.Rect customLine3FontRect;
    private SignEditorUi.Rect customLine4FontRect;
    private SignEditorUi.Rect customWhiteRect;
    private SignEditorUi.Rect customGreenRect;
    private SignEditorUi.Rect customBlueRect;
    private SignEditorUi.Rect customCartoucheRect;
    private SignEditorUi.Rect contentCartoucheRect;
    private SignEditorUi.Rect customGraphicRect;

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect previousPresetRect;
    private SignEditorUi.Rect presetRect;
    private SignEditorUi.Rect nextPresetRect;
    private SignEditorUi.Rect resetRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public MotorwaySignEditScreen(
            BlockPos blockPos,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean customMode,
            MotorwaySignPanelData[] customPanels
    ) {
        super(Component.literal("Panneaux autoroutiers modulables"));
        this.blockPos = blockPos.immutable();
        this.preset = preset == null ? MotorwaySignPreset.D31B_EX1 : preset;
        /* L'ancien mode est volontairement ignoré : l'éditeur est désormais unifié. */
        this.customMode = false;
        for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
            MotorwaySignLineData fallback = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.fromSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            MotorwaySignLineData data = values != null && i < values.length && values[i] != null
                    ? values[i]
                    : fallback;
            this.initialTexts[i] = data.text();
            this.lineFonts[i] = data.font();
            this.lineColors[i] = data.color();
        }
        for (int index = 0; index < this.customPanels.length; index++) {
            MotorwaySignPanelData panel = customPanels != null && index < customPanels.length
                    ? customPanels[index]
                    : null;
            MotorwaySignPanelData normalized = panel != null
                    ? withAllowedCustomBackground(panel)
                    : MotorwaySignPanelData.disabled();
            this.customPanels[index] = index == 0
                    ? normalized
                    : withoutCartouche(normalized);
        }
    }

    @Override
    protected void init() {
        super.init();
        int marginX = Math.max(10, Math.min(34, this.width / 24));
        int marginY = Math.max(8, Math.min(24, this.height / 24));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1220.0F, 720.0F);

        int pad = s(12);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;
        int leftW = Math.max(s(340), Math.round((this.windowWidth - pad * 2 - gap) * 0.45F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.contentRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.contentRect.x() + s(10);
        int innerW = this.contentRect.width() - s(20);
        int selectorY = this.contentRect.y() + s(34);
        int selectorH = SignEditorUi.safeControlHeight(this.font, s(25));
        int resetW = Math.max(70, s(98));
        int selectorGap = Math.max(3, s(6));
        int presetW = innerW - resetW - selectorGap;

        this.previousPresetRect = new SignEditorUi.Rect(0, 0, 0, 0);
        this.presetRect = new SignEditorUi.Rect(innerX, selectorY, presetW, selectorH);
        this.nextPresetRect = new SignEditorUi.Rect(0, 0, 0, 0);
        this.resetRect = new SignEditorUi.Rect(this.presetRect.x() + presetW + selectorGap, selectorY, resetW, selectorH);

        int editorTabsY = selectorY + selectorH + s(6);
        int editorTabGap = Math.max(2, s(4));
        int editorTabW = (innerW - editorTabGap * 4) / 5;
        this.modeRect = new SignEditorUi.Rect(
                innerX, editorTabsY, editorTabW, selectorH
        );
        int enabledW = Math.max(18, s(22));
        for (int index = 0; index < this.customTabRects.length; index++) {
            int x = innerX + (index + 1) * (editorTabW + editorTabGap);
            int width = index == this.customTabRects.length - 1
                    ? innerX + innerW - x
                    : editorTabW;
            this.customTabRects[index] = new SignEditorUi.Rect(
                    x, editorTabsY, width, selectorH
            );
            this.customEnabledRects[index] = new SignEditorUi.Rect(
                    x + width - enabledW, editorTabsY, enabledW, selectorH
            );
        }
        int rowsTop = editorTabsY + selectorH + s(13);
        int availableRowsH = this.contentRect.y() + this.contentRect.height() - rowsTop - s(9);
        int rowGap = Math.max(3, s(7));
        int rowH = Math.max(18, (availableRowsH - rowGap * 7) / 8);
        int fieldH = Math.min(rowH, Math.max(20, s(25)));
        int colorW = Math.max(62, s(90));
        int fontW = Math.max(56, s(78));
        int controlGap = Math.max(3, s(5));
        int fieldW = innerW - colorW - fontW - controlGap * 2;

        for (int i = 0; i < this.fields.length; i++) {
            int y = rowsTop + i * (rowH + rowGap);
            this.fields[i] = new EditBox(
                    this.font,
                    innerX,
                    y,
                    fieldW,
                    fieldH,
                    Component.literal("Champ " + (i + 1))
            );
            this.fields[i].setMaxLength(MAX_TEXT_LENGTH);
            this.fields[i].setValue(this.initialTexts[i]);
            this.addRenderableWidget(this.fields[i]);
            this.fontRects[i] = new SignEditorUi.Rect(innerX + fieldW + controlGap, y, fontW, fieldH);
            this.colorRects[i] = new SignEditorUi.Rect(this.fontRects[i].x() + fontW + controlGap, y, colorW, fieldH);
        }

        int contentCartoucheY = rowsTop + 4 * (rowH + rowGap);
        this.contentCartoucheRect = new SignEditorUi.Rect(
                innerX, contentCartoucheY, innerW, fieldH
        );
        this.contentCartoucheField = new EditBox(
                this.font, innerX, contentCartoucheY + fieldH + rowGap,
                innerW, fieldH, Component.literal("Texte du cartouche du panneau 1")
        );
        this.contentCartoucheField.setMaxLength(24);
        this.contentCartoucheField.setHint(Component.literal("Texte du cartouche (ex. M 337)"));
        this.addRenderableWidget(this.contentCartoucheField);

        int customPageGap = Math.max(2, s(4));
        int customPageW = (innerW - customPageGap * 3) / 4;
        for (int index = 0; index < this.customPageRects.length; index++) {
            int x = innerX + index * (customPageW + customPageGap);
            int width = index == this.customPageRects.length - 1
                    ? innerX + innerW - x
                    : customPageW;
            this.customPageRects[index] = new SignEditorUi.Rect(x, rowsTop, width, selectorH);
        }
        int customControlsTop = rowsTop + selectorH + s(10);
        int customY = customControlsTop;
        int distanceW = Math.max(48, s(64));
        int customFieldW = Math.max(90, innerW - fontW - distanceW - controlGap * 2);
        this.customLine1Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 1")
        );
        this.customLine1Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine1Field.setHint(Component.literal("Destination 1"));
        this.addRenderableWidget(this.customLine1Field);
        this.customDistance1Field = new EditBox(
                this.font,
                innerX + customFieldW + controlGap,
                customY,
                distanceW,
                fieldH,
                Component.literal("Distance 1")
        );
        this.customDistance1Field.setMaxLength(8);
        this.customDistance1Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance1Field);
        this.customLine1FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine2Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 2")
        );
        this.customLine2Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine2Field.setHint(Component.literal("Destination 2"));
        this.addRenderableWidget(this.customLine2Field);
        this.customDistance2Field = new EditBox(
                this.font,
                innerX + customFieldW + controlGap,
                customY,
                distanceW,
                fieldH,
                Component.literal("Distance 2")
        );
        this.customDistance2Field.setMaxLength(8);
        this.customDistance2Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance2Field);
        this.customLine2FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine3Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 3")
        );
        this.customLine3Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine3Field.setHint(Component.literal("Destination 3"));
        this.addRenderableWidget(this.customLine3Field);
        this.customDistance3Field = new EditBox(
                this.font, innerX + customFieldW + controlGap, customY,
                distanceW, fieldH, Component.literal("Distance 3")
        );
        this.customDistance3Field.setMaxLength(8);
        this.customDistance3Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance3Field);
        this.customLine3FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine4Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 4")
        );
        this.customLine4Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine4Field.setHint(Component.literal("Destination 4"));
        this.addRenderableWidget(this.customLine4Field);
        this.customDistance4Field = new EditBox(
                this.font, innerX + customFieldW + controlGap, customY,
                distanceW, fieldH, Component.literal("Distance 4")
        );
        this.customDistance4Field.setMaxLength(8);
        this.customDistance4Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance4Field);
        this.customLine4FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        int colorGap = Math.max(3, s(6));
        int colorButtonW = (innerW - colorGap * 2) / 3;
        this.customWhiteRect = new SignEditorUi.Rect(
                innerX, customControlsTop, colorButtonW, fieldH
        );
        this.customGreenRect = new SignEditorUi.Rect(
                innerX + colorButtonW + colorGap, customControlsTop, colorButtonW, fieldH
        );
        this.customBlueRect = new SignEditorUi.Rect(
                this.customGreenRect.x() + colorButtonW + colorGap,
                customControlsTop,
                innerX + innerW - (this.customGreenRect.x() + colorButtonW + colorGap),
                fieldH
        );
        this.customGraphicRect = new SignEditorUi.Rect(
                innerX, customControlsTop + fieldH + rowGap, innerW, fieldH
        );

        this.customCartoucheRect = new SignEditorUi.Rect(
                innerX, customControlsTop, innerW, fieldH
        );
        this.customCartoucheField = new EditBox(
                this.font, innerX, customControlsTop + fieldH + rowGap,
                innerW, fieldH, Component.literal("Texte du cartouche")
        );
        this.customCartoucheField.setMaxLength(24);
        this.customCartoucheField.setHint(Component.literal("Texte du cartouche"));
        this.addRenderableWidget(this.customCartoucheField);

        this.customDoubleLineRect = new SignEditorUi.Rect(
                innerX, customControlsTop, innerW, fieldH
        );

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(82, Math.max(s(145), this.font.width("✓  Appliquer") + 18));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH);
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, actionH);

        updateVisibleFields();
        loadCustomPanelIntoWidgets();
        loadContentCartoucheIntoWidgets();
        this.setInitialFocus(this.customMode ? this.customLine1Field : this.fields[0]);
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
                "D/DA",
                "Panneaux autoroutiers modulables",
                compactUi() ? "" : "62 préréglages • dimensions réelles adaptatives • plaques séparées conservées"
        );
        drawPreview(graphics);
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "MODÈLE ET CONTENU",
                compactUi() ? "" : "Chaque champ conserve sa police et la couleur de sa plaque"
        );

        SignEditorUi.drawModernButton(graphics, this.font, this.presetRect,
                fitText("▦  " + this.preset.getDisplayName() + " — choisir le modèle", this.presetRect.width() - 8),
                true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.resetRect, "Réinitialiser", false, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(
                graphics, this.font, this.modeRect,
                "Contenu", !this.customMode, true, mouseX, mouseY
        );
        MotorwaySignPanelData currentPanel = currentCustomPanelFromWidgets();
        for (int index = 0; index < this.customTabRects.length; index++) {
            MotorwaySignPanelData panel = this.customMode && index == this.selectedCustomPanel
                    ? currentPanel
                    : this.customPanels[index];
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.customTabRects[index],
                    "Ajout " + (index + 1),
                    this.customMode && index == this.selectedCustomPanel,
                    true, mouseX, mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.customEnabledRects[index],
                    panel.enabled() ? "✓" : "×", panel.enabled(),
                    true, mouseX, mouseY
            );
        }

        if (this.customMode) {
            drawCustomControls(graphics, mouseX, mouseY);
        } else {
            for (int i = 0; i < this.preset.getSlotCount(); i++) {
                MotorwaySignSlot slot = this.preset.getSlot(i);
                if (!compactUi()) {
                    SignEditorUi.drawFieldLabel(
                            graphics, this.font, fitText(slotEditorLabel(slot), this.fields[i].getWidth()),
                            this.fields[i].getX(), this.fields[i].getY() - s(10)
                    );
                }
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.fontRects[i], SignEditorUi.fontLabel(this.lineFonts[i]),
                        false, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.colorRects[i], this.lineColors[i].getDisplayName(),
                        false, true, mouseX, mouseY
                );
            }
            if (this.preset == MotorwaySignPreset.D63C) {
                MotorwaySignPanelData cartouche = contentCartouchePanelFromWidgets();
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.contentCartoucheRect,
                        "Cartouche du panneau 1 : " + cartouche.cartoucheType().getDisplayName(),
                        cartouche.cartoucheType().isVisible(), true, mouseX, mouseY
                );
            }
        }

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        if (this.customMode) {
            drawCustomPreview(graphics);
            return;
        }
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.previewRect,
                "APERÇU",
                compactUi() ? "" : this.preset.getDisplayName() + " • " + this.preset.getSupport().name().toLowerCase()
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(15);
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        TreeMap<Integer, List<Integer>> groups = new TreeMap<>();
        List<Integer> routes = new ArrayList<>();
        int distance = -1;
        for (int i = 0; i < this.preset.getSlotCount(); i++) {
            MotorwaySignSlot slot = this.preset.getSlot(i);
            if (slot.role() == MotorwaySignRole.ROUTE) {
                routes.add(i);
            } else if (slot.role() == MotorwaySignRole.DISTANCE) {
                distance = i;
            } else {
                groups.computeIfAbsent(Math.max(0, slot.panelGroup()), ignored -> new ArrayList<>()).add(i);
            }
        }

        int signW = Math.min(w - s(32), Math.max(s(220), (int) (w * 0.76F)));
        int plateGap = Math.max(3, s(5));
        int lineStep = Math.max(13, s(17));
        int totalLines = groups.values().stream().mapToInt(List::size).sum();
        int mainHeight = Math.max(s(52), totalLines * lineStep + groups.size() * s(18));
        int extraHeight = (routes.isEmpty() ? 0 : s(34)) + (distance < 0 ? 0 : s(29));
        int signBottom = y + Math.min(h - s(45), Math.max(s(145), (h + mainHeight + extraHeight) / 2));
        int supportTop = signBottom - mainHeight - extraHeight;
        int poleColor = 0xFF2C2C2C;
        int centerX = x + w / 2;
        graphics.fill(centerX - s(5), supportTop, centerX + s(5), y + h - s(8), poleColor);
        graphics.fill(centerX - s(14), y + h - s(12), centerX + s(14), y + h - s(4), poleColor);

        int cursorY = signBottom;
        if (distance >= 0) {
            int ph = s(25);
            int pw = Math.min(signW / 2, Math.max(s(90), this.font.width(this.fields[distance].getValue()) + s(22)));
            cursorY -= ph;
            drawPreviewPlate(graphics, centerX - pw / 2, cursorY, pw, ph, this.lineColors[distance]);
            drawPreviewText(graphics, distance, centerX - pw / 2, cursorY, pw, ph);
            cursorY -= plateGap;
        }

        for (List<Integer> indices : groups.descendingMap().values()) {
            int ph = Math.max(s(39), indices.size() * lineStep + s(16));
            cursorY -= ph;
            MotorwaySignColor color = this.lineColors[indices.getFirst()];
            drawPreviewPlate(graphics, centerX - signW / 2, cursorY, signW, ph, color);
            int textY = cursorY + (ph - indices.size() * lineStep) / 2;
            for (int index : indices) {
                drawPreviewText(graphics, index, centerX - signW / 2, textY, signW, lineStep);
                textY += lineStep;
            }
            cursorY -= plateGap;
        }

        if (groups.isEmpty()) {
            int ph = s(48);
            cursorY -= ph;
            drawPreviewPlate(graphics, centerX - signW / 2, cursorY, signW, ph, MotorwaySignColor.WHITE);
            cursorY -= plateGap;
        }

        if (!routes.isEmpty()) {
            int ph = s(28);
            int pw = Math.max(s(68), (signW - plateGap * (routes.size() - 1)) / Math.max(2, routes.size()));
            int total = pw * routes.size() + plateGap * (routes.size() - 1);
            int routeX = centerX - total / 2;
            cursorY -= ph;
            for (int index : routes) {
                drawPreviewPlate(graphics, routeX, cursorY, pw, ph, this.lineColors[index]);
                drawPreviewText(graphics, index, routeX, cursorY, pw, ph);
                routeX += pw + plateGap;
            }
        }
        if (this.preset == MotorwaySignPreset.D63C) {
            MotorwaySignPanelData cartouche = contentCartouchePanelFromWidgets();
            if (cartouche.cartoucheType().isVisible()) {
                int cartH = s(25);
                int cartW = Math.min(signW, Math.max(
                        s(70), this.font.width(cartouche.cartoucheText()) + s(24)
                ));
                int cartY = cursorY - plateGap - cartH;
                MotorwaySignColor color = cartouchePreviewColor(cartouche.cartoucheType());
                drawPreviewPlate(graphics, centerX - cartW / 2, cartY, cartW, cartH, color);
                drawCustomPreviewText(
                        graphics, cartouche.cartoucheText(), RoadTextFont.L1, color,
                        centerX - cartW / 2, cartY, cartW, cartH
                );
            }
        }
    }

    private void drawCustomControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MotorwaySignPanelData current = currentCustomPanelFromWidgets();
        SignEditorUi.drawPageTabs(
                graphics, this.font, this.customPageRects,
                new String[]{"Texte", "Style", "Cart.", "Format"},
                this.customSettingsPage, mouseX, mouseY
        );
        switch (this.customSettingsPage) {
            case 0 -> {
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine1FontRect,
                        SignEditorUi.fontLabel(current.line1Font()), false, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine2FontRect,
                        SignEditorUi.fontLabel(current.line2Font()), false,
                        current.lineCount() >= 2, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine3FontRect,
                        SignEditorUi.fontLabel(current.line3Font()), false,
                        current.lineCount() >= 3, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine4FontRect,
                        SignEditorUi.fontLabel(current.line4Font()), false,
                        current.lineCount() >= 4, mouseX, mouseY
                );
            }
            case 1 -> {
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customWhiteRect, "Blanc",
                        current.background() == MotorwaySignColor.WHITE, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customGreenRect, "Vert",
                        current.background() == MotorwaySignColor.GREEN, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customBlueRect, "Bleu",
                        current.background() == MotorwaySignColor.BLUE, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customGraphicRect,
                        "Symbole : " + graphicLabel(current.graphic()),
                        current.graphic() != MotorwaySignGraphic.NONE, true, mouseX, mouseY
                );
            }
            case 2 -> {
                boolean cartoucheAllowed = this.selectedCustomPanel == 0;
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customCartoucheRect,
                        cartoucheAllowed
                                ? "Cartouche : " + current.cartoucheType().getDisplayName()
                                : "Cartouche réservé au panneau 1",
                        cartoucheAllowed && current.cartoucheType().isVisible(),
                        cartoucheAllowed, mouseX, mouseY
                );
            }
            default -> SignEditorUi.drawModernButton(
                    graphics, this.font, this.customDoubleLineRect,
                    "Villes sur la pancarte : " + current.lineCount(),
                    current.lineCount() > 1, true, mouseX, mouseY
            );
        }
    }

    private void drawCustomPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.previewRect, "APERÇU",
                compactUi() ? "" : "Ensemble complet • " + this.preset.getDisplayName()
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(15);
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        MotorwaySignPanelData[] panels = previewCustomPanels();
        List<MotorwaySignPanelData> enabled = new ArrayList<>();
        for (MotorwaySignPanelData panel : panels) {
            if (panel.enabled()
                    && !(panel.cartoucheType().isVisible() && !panel.hasPanelContent())) {
                enabled.add(panel);
            }
        }
        int gap = Math.max(3, s(5));
        int panelW = Math.min(w - s(34), Math.max(s(210), Math.round(w * 0.78F)));
        int originalH = s(52);
        int totalH = originalH;
        for (MotorwaySignPanelData panel : enabled) {
            totalH += gap + s(25 + panel.lineCount() * 19);
        }
        MotorwaySignPanelData firstPanel = panels.length > 0 ? panels[0] : null;
        boolean showTopCartouche = firstPanel != null && firstPanel.cartoucheType().isVisible();
        if (showTopCartouche) {
            totalH += s(29) + gap;
        }
        int cursorY = y + Math.max(s(18), (h - totalH) / 2);
        int centerX = x + w / 2;
        int poleTop = cursorY + totalH;
        graphics.fill(centerX - s(4), poleTop - s(4), centerX + s(4), y + h - s(7), 0xFF2C2C2C);

        if (showTopCartouche) {
            int cartH = s(29);
            int cartW = Math.min(panelW, Math.max(
                    s(70), this.font.width(firstPanel.cartoucheText()) + s(24)
            ));
            MotorwaySignColor color = cartouchePreviewColor(firstPanel.cartoucheType());
            drawPreviewPlate(graphics, centerX - cartW / 2, cursorY, cartW, cartH, color);
            drawCustomPreviewText(
                    graphics, firstPanel.cartoucheText(), RoadTextFont.L1, color,
                    centerX - cartW / 2, cursorY, cartW, cartH
            );
            cursorY += cartH + gap;
        }

        drawPreviewPlate(
                graphics, centerX - panelW / 2, cursorY,
                panelW, originalH, MotorwaySignColor.BLUE
        );
        drawCustomPreviewText(
                graphics, this.preset.getDisplayName(),
                RoadTextFont.L1, MotorwaySignColor.BLUE,
                centerX - panelW / 2, cursorY, panelW, originalH
        );
        cursorY += originalH;

        for (MotorwaySignPanelData panel : enabled) {
            cursorY += gap;
            int panelH = s(25 + panel.lineCount() * 19);
            drawPreviewPlate(
                    graphics, centerX - panelW / 2, cursorY, panelW, panelH, panel.background()
            );
            int lineH = panelH / panel.lineCount();
            for (int lineIndex = 0; lineIndex < panel.lineCount(); lineIndex++) {
                drawCustomPreviewLine(
                        graphics,
                        panel.line(lineIndex), panel.distance(lineIndex), panel.font(lineIndex), panel.background(),
                        centerX - panelW / 2, cursorY + lineIndex * lineH,
                        panelW, lineIndex == panel.lineCount() - 1
                                ? panelH - lineIndex * lineH
                                : lineH
                );
            }
            cursorY += panelH;
        }
    }

    private void drawCustomPreviewText(
            GuiGraphicsExtractor graphics,
            String value,
            RoadTextFont fontType,
            MotorwaySignColor color,
            int x,
            int y,
            int width,
            int height
    ) {
        Component component = Component.literal(fitText(value, width - s(14))).withStyle(
                Style.EMPTY.withFont(fontType == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1)
        );
        graphics.text(
                this.font, component,
                x + (width - this.font.width(component)) / 2,
                y + (height - this.font.lineHeight) / 2,
                color.getTextArgb(), false
        );
    }

    private void drawCustomPreviewLine(
            GuiGraphicsExtractor graphics,
            String value,
            String distance,
            RoadTextFont fontType,
            MotorwaySignColor color,
            int x,
            int y,
            int width,
            int height
    ) {
        Component distanceComponent = Component.literal(distance == null ? "" : distance).withStyle(
                Style.EMPTY.withFont(fontType == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1)
        );
        int distanceWidth = distance == null || distance.isBlank()
                ? 0
                : this.font.width(distanceComponent) + s(10);
        Component lineComponent = Component.literal(
                fitText(value, width - distanceWidth - s(18))
        ).withStyle(Style.EMPTY.withFont(fontType == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1));
        int drawY = y + (height - this.font.lineHeight) / 2;
        int contentWidth = width - distanceWidth;
        graphics.text(
                this.font, lineComponent,
                x + s(9),
                drawY, color.getTextArgb(), false
        );
        if (distanceWidth > 0) {
            graphics.text(
                    this.font, distanceComponent,
                    x + width - this.font.width(distanceComponent) - s(7),
                    drawY, color.getTextArgb(), false
            );
        }
    }

    private void drawPreviewPlate(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            MotorwaySignColor color
    ) {
        graphics.fill(x, y, x + width, y + height, previewBorderColor(color));
        int border = Math.max(2, s(3));
        graphics.fill(x + border, y + border, x + width - border, y + height - border, color.getArgb());
    }

    private void drawPreviewText(
            GuiGraphicsExtractor graphics,
            int index,
            int x,
            int y,
            int width,
            int height
    ) {
        String text = fitText(this.fields[index].getValue(), width - s(12));
        Component component = Component.literal(text).withStyle(
                Style.EMPTY.withFont(this.lineFonts[index] == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1)
        );
        int drawX = this.preset == MotorwaySignPreset.D63C && (index == 2 || index == 3)
                ? x + s(9)
                : x + (width - this.font.width(component)) / 2;
        int drawY = y + (height - this.font.lineHeight) / 2;
        if (this.preset == MotorwaySignPreset.D63C) {
            drawY += index == 3 ? s(2) : index == 2 ? s(1) : 0;
        }
        graphics.text(this.font, component, drawX, drawY, this.lineColors[index].getTextArgb(), false);
    }

    private static String slotEditorLabel(MotorwaySignSlot slot) {
        if (slot == null) {
            return "Champ";
        }
        return switch (slot.role()) {
            case ROUTE, DISTANCE -> "Base • " + slot.label();
            case DESTINATION, INFO -> slot.panelGroup() >= 0
                    ? "Panneau " + (slot.panelGroup() + 1) + " • " + slot.label()
                    : "Base • " + slot.label();
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (this.modeRect.contains(event.x(), event.y())) {
                if (this.customMode) {
                    storeSelectedCustomPanel();
                }
                this.customMode = false;
                loadContentCartoucheIntoWidgets();
                updateVisibleFields();
                this.setInitialFocus(this.fields[0]);
                return true;
            }
            for (int index = 0; index < this.customTabRects.length; index++) {
                if (this.customEnabledRects[index].contains(event.x(), event.y())) {
                    if (this.customMode && index == this.selectedCustomPanel) {
                        MotorwaySignPanelData current = currentCustomPanelFromWidgets();
                        setCurrentCustomPanel(copyPanel(
                                current, !current.enabled(), null, null, null,
                                null, null, null
                        ));
                    } else {
                        MotorwaySignPanelData panel = this.customPanels[index];
                        this.customPanels[index] = copyPanel(
                                panel, !panel.enabled(), null, null, null,
                                null, null, null
                        );
                    }
                    return true;
                }
                if (this.customTabRects[index].contains(event.x(), event.y())) {
                    if (this.customMode) {
                        storeSelectedCustomPanel();
                    } else {
                        storeContentCartouche();
                    }
                    this.selectedCustomPanel = index;
                    this.customMode = true;
                    loadCustomPanelIntoWidgets();
                    updateVisibleFields();
                    this.setInitialFocus(this.customLine1Field);
                    return true;
                }
            }
            if (this.customMode) {
                for (int index = 0; index < this.customPageRects.length; index++) {
                    if (this.customPageRects[index].contains(event.x(), event.y())) {
                        storeSelectedCustomPanel();
                        this.customSettingsPage = index;
                        updateVisibleFields();
                        return true;
                    }
                }
            }
            if (this.customMode && handleCustomClick(event.x(), event.y())) {
                return true;
            }
            if (!this.customMode && this.preset == MotorwaySignPreset.D63C
                    && this.contentCartoucheRect.contains(event.x(), event.y())) {
                storeContentCartouche();
                MotorwaySignPanelData panel = this.customPanels[0];
                this.customPanels[0] = withCartouche(
                        panel, panel.cartoucheType().next(), panel.cartoucheText()
                );
                loadContentCartoucheIntoWidgets();
                updateVisibleFields();
                return true;
            }
            if (this.presetRect.contains(event.x(), event.y())) {
                openPresetGallery();
                return true;
            }
            if (this.resetRect.contains(event.x(), event.y())) {
                applyPresetDefaults();
                return true;
            }
            for (int i = 0; !this.customMode && i < this.preset.getSlotCount(); i++) {
                if (this.fontRects[i].contains(event.x(), event.y())) {
                    this.lineFonts[i] = this.lineFonts[i].next();
                    return true;
                }
                if (this.colorRects[i].contains(event.x(), event.y())) {
                    setGroupColor(i, this.lineColors[i].next());
                    return true;
                }
            }
            if (this.applyRect.contains(event.x(), event.y())) {
                save();
                return true;
            }
            if (this.cancelRect.contains(event.x(), event.y())) {
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void openPresetGallery() {
        if (this.customMode) {
            storeSelectedCustomPanel();
        } else {
            storeContentCartouche();
        }
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                new MotorwayPresetGalleryScreen(
                        this,
                        this.blockPos,
                        this.preset,
                        this.customPanels
                )
        );
    }

    private void changePreset(MotorwaySignPreset newPreset) {
        if (newPreset == MotorwaySignPreset.D61B) {
            MotorwaySignLineData[] defaults = new MotorwaySignLineData[
                    MotorwaySignBlockEntity.MAX_SLOTS
            ];
            for (int index = 0; index < defaults.length; index++) {
                defaults[index] = index < newPreset.getSlotCount()
                        ? MotorwaySignLineData.fromSlot(newPreset.getSlot(index))
                        : MotorwaySignLineData.empty();
            }
            MotorwaySignPanelData[] emptyPanels = new MotorwaySignPanelData[
                    MotorwaySignBlockEntity.MAX_CUSTOM_PANELS
            ];
            for (int index = 0; index < emptyPanels.length; index++) {
                emptyPanels[index] = MotorwaySignPanelData.disabled();
            }
            net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                    new MotorwayD61BEditScreen(this.blockPos, defaults, emptyPanels)
            );
            return;
        }
        this.preset = newPreset;
        applyPresetDefaults();
        loadContentCartoucheIntoWidgets();
        updateVisibleFields();
    }

    private void applyPresetDefaults() {
        for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
            MotorwaySignLineData data = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.fromSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            this.fields[i].setValue(data.text());
            this.lineFonts[i] = data.font();
            this.lineColors[i] = data.color();
        }
    }

    private void updateVisibleFields() {
        for (int i = 0; i < this.fields.length; i++) {
            boolean visible = !this.customMode && i < this.preset.getSlotCount();
            this.fields[i].visible = visible;
            this.fields[i].active = visible;
        }
        if (this.customLine1Field != null) {
            MotorwaySignPanelData panel = currentCustomPanelFromWidgets();
            boolean textPage = this.customMode && this.customSettingsPage == 0;
            this.customLine1Field.visible = textPage;
            this.customLine1Field.active = textPage;
            this.customDistance1Field.visible = textPage;
            this.customDistance1Field.active = textPage;
            this.customLine2Field.visible = textPage && panel.doubleLine();
            this.customLine2Field.active = textPage && panel.doubleLine();
            this.customDistance2Field.visible = textPage && panel.doubleLine();
            this.customDistance2Field.active = textPage && panel.doubleLine();
            this.customLine3Field.visible = textPage && panel.lineCount() >= 3;
            this.customLine3Field.active = textPage && panel.lineCount() >= 3;
            this.customDistance3Field.visible = textPage && panel.lineCount() >= 3;
            this.customDistance3Field.active = textPage && panel.lineCount() >= 3;
            this.customLine4Field.visible = textPage && panel.lineCount() >= 4;
            this.customLine4Field.active = textPage && panel.lineCount() >= 4;
            this.customDistance4Field.visible = textPage && panel.lineCount() >= 4;
            this.customDistance4Field.active = textPage && panel.lineCount() >= 4;
            boolean cartouchePage = this.customMode && this.customSettingsPage == 2
                    && this.selectedCustomPanel == 0;
            this.customCartoucheField.visible = cartouchePage;
            this.customCartoucheField.active = cartouchePage && panel.cartoucheType().isVisible();
        }
        if (this.contentCartoucheField != null) {
            boolean visible = !this.customMode && this.preset == MotorwaySignPreset.D63C;
            this.contentCartoucheField.visible = visible;
            this.contentCartoucheField.active = visible
                    && this.customPanels[0].cartoucheType().isVisible();
        }
    }

    private boolean handleCustomClick(double x, double y) {
        MotorwaySignPanelData current = currentCustomPanelFromWidgets();
        if (this.customSettingsPage == 0 && this.customLine1FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(current, null, null, current.line1Font().next(), null, null, null, null));
            return true;
        }
        if (this.customSettingsPage == 0 && current.doubleLine() && this.customLine2FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(current, null, null, null, current.line2Font().next(), null, null, null));
            return true;
        }
        if (this.customSettingsPage == 0 && current.lineCount() >= 3 && this.customLine3FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithFont(current, 2, current.line3Font().next()));
            return true;
        }
        if (this.customSettingsPage == 0 && current.lineCount() >= 4 && this.customLine4FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithFont(current, 3, current.line4Font().next()));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customWhiteRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.WHITE, null, null));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customGreenRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.GREEN, null, null));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customBlueRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.BLUE, null, null));
            return true;
        }
        if (this.customSettingsPage == 3 && this.customDoubleLineRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithLineCount(current, current.lineCount() % 4 + 1));
            updateVisibleFields();
            return true;
        }
        if (this.customSettingsPage == 2 && this.selectedCustomPanel == 0
                && this.customCartoucheRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(
                    current, null, null, null, null, null, current.cartoucheType().next(), null
            ));
            updateVisibleFields();
            return true;
        }
        if (this.customSettingsPage == 1 && this.customGraphicRect.contains(x, y)) {
            MotorwaySignGraphic[] graphics = MotorwaySignGraphic.values();
            MotorwaySignGraphic next = graphics[(current.graphic().ordinal() + 1) % graphics.length];
            setCurrentCustomPanel(copyPanel(current, null, null, null, null, null, null, next));
            return true;
        }
        return false;
    }

    private MotorwaySignPanelData copyPanel(
            MotorwaySignPanelData source,
            Boolean enabled,
            Boolean doubleLine,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            MotorwaySignColor background,
            CartoucheType cartoucheType,
            MotorwaySignGraphic graphic
    ) {
        return new MotorwaySignPanelData(
                enabled == null ? source.enabled() : enabled,
                doubleLine == null ? source.lineCount() : doubleLine ? 2 : 1,
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                line1Font == null ? source.line1Font() : line1Font,
                line2Font == null ? source.line2Font() : line2Font,
                source.line3Font(), source.line4Font(),
                background == null ? source.background() : background,
                cartoucheType == null ? source.cartoucheType() : cartoucheType,
                source.cartoucheText(),
                graphic == null ? source.graphic() : graphic
        );
    }

    private MotorwaySignPanelData copyPanelWithLineCount(MotorwaySignPanelData source, int lineCount) {
        return new MotorwaySignPanelData(
                source.enabled(), lineCount,
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), source.cartoucheType(), source.cartoucheText(), source.graphic()
        );
    }

    private MotorwaySignPanelData copyPanelWithFont(
            MotorwaySignPanelData source,
            int lineIndex,
            RoadTextFont font
    ) {
        RoadTextFont[] fonts = {
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font()
        };
        fonts[Math.max(0, Math.min(3, lineIndex))] = font;
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                fonts[0], fonts[1], fonts[2], fonts[3],
                source.background(), source.cartoucheType(), source.cartoucheText(), source.graphic()
        );
    }

    private MotorwaySignPanelData currentCustomPanelFromWidgets() {
        MotorwaySignPanelData stored = this.customPanels[this.selectedCustomPanel];
        if (this.customLine1Field == null) {
            return stored;
        }
        return new MotorwaySignPanelData(
                stored.enabled(), stored.lineCount(),
                this.customLine1Field.getValue(), this.customLine2Field.getValue(),
                this.customLine3Field.getValue(), this.customLine4Field.getValue(),
                this.customDistance1Field.getValue(), this.customDistance2Field.getValue(),
                this.customDistance3Field.getValue(), this.customDistance4Field.getValue(),
                stored.line1Font(), stored.line2Font(), stored.line3Font(), stored.line4Font(),
                stored.background(),
                stored.cartoucheType(), this.customCartoucheField.getValue(), stored.graphic()
        );
    }

    private void setCurrentCustomPanel(MotorwaySignPanelData panel) {
        MotorwaySignPanelData normalized = withAllowedCustomBackground(panel);
        this.customPanels[this.selectedCustomPanel] = this.selectedCustomPanel == 0
                ? normalized
                : withoutCartouche(normalized);
        loadCustomPanelIntoWidgets();
    }

    private void storeSelectedCustomPanel() {
        if (this.customLine1Field != null) {
            this.customPanels[this.selectedCustomPanel] = currentCustomPanelFromWidgets();
        }
    }

    private MotorwaySignPanelData contentCartouchePanelFromWidgets() {
        MotorwaySignPanelData panel = this.customPanels[0];
        String text = this.contentCartoucheField == null
                ? panel.cartoucheText()
                : this.contentCartoucheField.getValue();
        return withCartouche(panel, panel.cartoucheType(), text);
    }

    private void storeContentCartouche() {
        if (this.contentCartoucheField != null && this.preset == MotorwaySignPreset.D63C) {
            this.customPanels[0] = contentCartouchePanelFromWidgets();
        }
    }

    private void loadContentCartoucheIntoWidgets() {
        if (this.contentCartoucheField != null) {
            this.contentCartoucheField.setValue(this.customPanels[0].cartoucheText());
        }
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
                source.background(), type, text, source.graphic()
        );
    }

    private void loadCustomPanelIntoWidgets() {
        if (this.customLine1Field == null) {
            return;
        }
        MotorwaySignPanelData panel = this.customPanels[this.selectedCustomPanel];
        this.customLine1Field.setValue(panel.line1());
        this.customLine2Field.setValue(panel.line2());
        this.customLine3Field.setValue(panel.line3());
        this.customLine4Field.setValue(panel.line4());
        this.customDistance1Field.setValue(panel.distance1());
        this.customDistance2Field.setValue(panel.distance2());
        this.customDistance3Field.setValue(panel.distance3());
        this.customDistance4Field.setValue(panel.distance4());
        this.customCartoucheField.setValue(panel.cartoucheText());
        updateVisibleFields();
    }

    private MotorwaySignPanelData[] previewCustomPanels() {
        MotorwaySignPanelData[] result = this.customPanels.clone();
        result[this.selectedCustomPanel] = currentCustomPanelFromWidgets();
        return result;
    }

    private static MotorwaySignColor cartouchePreviewColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E44 -> MotorwaySignColor.WHITE;
            default -> MotorwaySignColor.BLUE;
        };
    }

    private static MotorwaySignColor allowedCustomBackground(MotorwaySignColor background) {
        return background == MotorwaySignColor.GREEN || background == MotorwaySignColor.WHITE
                ? background
                : MotorwaySignColor.BLUE;
    }

    private static int previewBorderColor(MotorwaySignColor color) {
        return switch (color) {
            case BLUE, GREEN -> MotorwaySignColor.WHITE.getArgb();
            case WHITE -> MotorwaySignColor.BLACK.getArgb();
            default -> 0xFFD7D7D2;
        };
    }

    private static MotorwaySignPanelData withAllowedCustomBackground(MotorwaySignPanelData panel) {
        MotorwaySignColor background = allowedCustomBackground(panel.background());
        if (background == panel.background()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                background, panel.cartoucheType(), panel.cartoucheText(), panel.graphic()
        );
    }

    private static MotorwaySignPanelData withoutCartouche(MotorwaySignPanelData panel) {
        if (!panel.cartoucheType().isVisible() && panel.cartoucheText().isBlank()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                panel.background(), CartoucheType.NONE, "", panel.graphic()
        );
    }

    private static String graphicLabel(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case NONE -> "Aucun";
            case DIAGONAL_LEFT -> "Flèche gauche";
            case DIAGONAL_RIGHT -> "Flèche droite";
            case DOWN -> "Flèche basse";
            case DOWN_DOUBLE -> "Deux flèches";
            case EXIT -> "Sortie";
            case EXIT_LIST -> "Liste sorties";
            case SCHEMATIC_LEFT -> "Schéma gauche";
            case SCHEMATIC_RIGHT -> "Schéma droit";
            case JUNCTION -> "Bifurcation";
            case MOTORWAY -> "Autoroute";
            case SERVICES -> "Services";
        };
    }

    private void setGroupColor(int index, MotorwaySignColor color) {
        MotorwaySignSlot clicked = this.preset.getSlot(index);
        this.lineColors[index] = color;
        if (clicked.role() == MotorwaySignRole.ROUTE || clicked.role() == MotorwaySignRole.DISTANCE) {
            return;
        }
        for (int i = 0; i < this.preset.getSlotCount(); i++) {
            MotorwaySignSlot candidate = this.preset.getSlot(i);
            if (candidate.role() != MotorwaySignRole.ROUTE
                    && candidate.role() != MotorwaySignRole.DISTANCE
                    && candidate.panelGroup() == clicked.panelGroup()) {
                this.lineColors[i] = color;
            }
        }
    }

    private void save() {
        if (this.customMode) {
            storeSelectedCustomPanel();
        } else {
            storeContentCartouche();
        }
        MotorwaySignLineData[] data = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
        for (int i = 0; i < data.length; i++) {
            data[i] = new MotorwaySignLineData(this.fields[i].getValue(), this.lineFonts[i], this.lineColors[i]);
        }
        ClientPacketDistributor.sendToServer(new UpdateMotorwaySignPayload(
                this.blockPos,
                this.preset.getSerializedName(),
                data[0], data[1], data[2], data[3], data[4], data[5],
                false,
                this.customPanels[0], this.customPanels[1],
                this.customPanels[2], this.customPanels[3]
        ));
        this.onClose();
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private String fitText(String value, int maxWidth) {
        return SignEditorUi.fitText(this.font, value == null ? "" : value, Math.max(8, maxWidth));
    }
}
