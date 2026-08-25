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
import net.xelpy.moreroad.block.custom.DA31CArrowType;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.network.UpdateDA31CPayload;

/**
 * Éditeur DA31C V12 : 4 lignes avec police indépendante, 3 cartouches à taille normale
 * et 2 flèches sélectionnables.
 */
public class DA31CEditScreen extends Screen {

    private static final int MAX_LINE_LENGTH = 48;
    private static final int MAX_CARTOUCHE_LENGTH = 24;
    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );
    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    private final BlockPos blockPos;
    private final String[] currentLines = new String[4];
    private final RoadTextFont[] lineFonts = new RoadTextFont[4];

    private CartoucheType cartoucheTopType;
    private final String currentCartoucheTopText;
    private CartoucheType cartoucheLeftType;
    private final String currentCartoucheLeftText;
    private CartoucheType cartoucheRightType;
    private final String currentCartoucheRightText;

    private DA31CArrowType arrowLeftType;
    private DA31CArrowType arrowRightType;

    private final EditBox[] lineFields = new EditBox[4];
    private EditBox cartoucheTopTextField;
    private EditBox cartoucheLeftTextField;
    private EditBox cartoucheRightTextField;

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect cartoucheTopTypeRect;
    private SignEditorUi.Rect cartoucheLeftTypeRect;
    private SignEditorUi.Rect cartoucheRightTypeRect;
    private SignEditorUi.Rect arrowLeftTypeRect;
    private SignEditorUi.Rect arrowRightTypeRect;
    private final SignEditorUi.Rect[] lineFontRects = new SignEditorUi.Rect[4];
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public DA31CEditScreen(
            BlockPos blockPos,
            String line1,
            String line2,
            String line3,
            String line4,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            RoadTextFont line3Font,
            RoadTextFont line4Font,
            CartoucheType cartoucheTopType,
            String currentCartoucheTopText,
            CartoucheType cartoucheLeftType,
            String currentCartoucheLeftText,
            CartoucheType cartoucheRightType,
            String currentCartoucheRightText,
            DA31CArrowType arrowLeftType,
            DA31CArrowType arrowRightType
    ) {
        super(Component.literal("DA31C — Autoroute"));
        this.blockPos = blockPos.immutable();
        this.currentLines[0] = safe(line1);
        this.currentLines[1] = safe(line2);
        this.currentLines[2] = safe(line3);
        this.currentLines[3] = safe(line4);
        this.lineFonts[0] = safeFont(line1Font);
        this.lineFonts[1] = safeFont(line2Font);
        this.lineFonts[2] = safeFont(line3Font);
        this.lineFonts[3] = safeFont(line4Font);
        this.cartoucheTopType = safeType(cartoucheTopType);
        this.currentCartoucheTopText = safe(currentCartoucheTopText);
        this.cartoucheLeftType = safeType(cartoucheLeftType);
        this.currentCartoucheLeftText = safe(currentCartoucheLeftText);
        this.cartoucheRightType = safeType(cartoucheRightType);
        this.currentCartoucheRightText = safe(currentCartoucheRightText);
        this.arrowLeftType = safeArrow(arrowLeftType);
        this.arrowRightType = safeArrow(arrowRightType);
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(10, Math.min(36, this.width / 22));
        int marginY = Math.max(8, Math.min(26, this.height / 22));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(
                this.windowWidth,
                this.windowHeight,
                1180.0F,
                720.0F
        );

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(360), Math.round((this.windowWidth - pad * 2 - gap) * 0.52F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.contentRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.contentRect.x() + s(12);
        int innerW = this.contentRect.width() - s(24);
        int fieldH = pagedUi() ? 20 : s(22);
        int selectorH = SignEditorUi.safeControlHeight(this.font, s(22));
        int selectorW = Math.max(s(118), Math.round(innerW * 0.35F));
        int rowGap = s(8);
        int startY = this.contentRect.y() + (pagedUi() ? 28 : s(36));
        int cartoucheStep = Math.max(fieldH, selectorH) + s(15);

        this.cartoucheTopTypeRect = new SignEditorUi.Rect(innerX, startY, selectorW, selectorH);
        this.cartoucheTopTextField = createCartoucheField(
                innerX + selectorW + rowGap,
                startY,
                innerW - selectorW - rowGap,
                fieldH,
                "Cartouche haut",
                this.currentCartoucheTopText
        );

        int leftY = startY + cartoucheStep;
        this.cartoucheLeftTypeRect = new SignEditorUi.Rect(innerX, leftY, selectorW, selectorH);
        this.cartoucheLeftTextField = createCartoucheField(
                innerX + selectorW + rowGap,
                leftY,
                innerW - selectorW - rowGap,
                fieldH,
                "Cartouche bas gauche",
                this.currentCartoucheLeftText
        );

        int rightY = leftY + cartoucheStep;
        this.cartoucheRightTypeRect = new SignEditorUi.Rect(innerX, rightY, selectorW, selectorH);
        this.cartoucheRightTextField = createCartoucheField(
                innerX + selectorW + rowGap,
                rightY,
                innerW - selectorW - rowGap,
                fieldH,
                "Cartouche bas droite",
                this.currentCartoucheRightText
        );

        int arrowY = rightY + cartoucheStep;
        int arrowGap = s(8);
        int arrowW = (innerW - arrowGap) / 2;
        this.arrowLeftTypeRect = new SignEditorUi.Rect(innerX, arrowY, arrowW, selectorH);
        this.arrowRightTypeRect = new SignEditorUi.Rect(innerX + arrowW + arrowGap, arrowY, arrowW, selectorH);

        int linesStartY = arrowY + selectorH + s(19);
        int lineGap = s(8);
        int fontW = Math.max(s(96), Math.round(innerW * 0.26F));
        int lineFieldW = innerW - fontW - rowGap;
        for (int i = 0; i < this.lineFields.length; i++) {
            int lineY = linesStartY + i * (fieldH + lineGap);
            this.lineFields[i] = new EditBox(
                    this.font,
                    innerX,
                    lineY,
                    lineFieldW,
                    fieldH,
                    Component.literal("Ligne " + (i + 1))
            );
            this.lineFields[i].setMaxLength(MAX_LINE_LENGTH);
            this.lineFields[i].setValue(this.currentLines[i]);
            this.addRenderableWidget(this.lineFields[i]);
            this.lineFontRects[i] = new SignEditorUi.Rect(
                    innerX + lineFieldW + rowGap,
                    lineY,
                    fontW,
                    fieldH
            );
        }

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(
                74,
                Math.max(
                        s(145),
                        Math.max(this.font.width("✓  Appliquer"), this.font.width("×  Annuler")) + 18
                )
        );
        int actionGap = Math.max(4, s(10));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - actionW,
                actionY,
                actionW,
                actionH
        );
        this.applyRect = new SignEditorUi.Rect(
                this.cancelRect.x() - actionGap - actionW,
                actionY,
                actionW,
                actionH
        );

        updateFieldStates();
        this.setInitialFocus(this.lineFields[0]);
    }

    private EditBox createCartoucheField(
            int x,
            int y,
            int width,
            int height,
            String name,
            String value
    ) {
        EditBox field = new EditBox(this.font, x, y, width, height, Component.literal(name));
        field.setMaxLength(MAX_CARTOUCHE_LENGTH);
        field.setValue(value);
        this.addRenderableWidget(field);
        return field;
    }

    private void updateFieldStates() {
        this.cartoucheTopTextField.active = this.cartoucheTopType.isVisible();
        this.cartoucheLeftTextField.active = this.cartoucheLeftType.isVisible();
        this.cartoucheRightTextField.active = this.cartoucheRightType.isVisible();
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
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
                graphics,
                this.font,
                this.windowX,
                this.windowY,
                this.windowWidth,
                this.windowHeight,
                "DA31C",
                "Panneau autoroutier sur portique",
                compactUi() ? "" : "1 à 4 lignes • hauteur automatique • 3 cartouches • 2 flèches au choix"
        );

        drawPreview(graphics);

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "CONTENU",
                pagedUi() ? "" : "3 cartouches, 2 flèches et jusqu'à quatre destinations avec police indépendante"
        );

        drawTypeButton(graphics, this.cartoucheTopTypeRect, "Haut", this.cartoucheTopType, mouseX, mouseY);
        drawTypeButton(graphics, this.cartoucheLeftTypeRect, "Bas G", this.cartoucheLeftType, mouseX, mouseY);
        drawTypeButton(graphics, this.cartoucheRightTypeRect, "Bas D", this.cartoucheRightType, mouseX, mouseY);
        drawArrowTypeButton(graphics, this.arrowLeftTypeRect, "Flèche G", this.arrowLeftType, mouseX, mouseY);
        drawArrowTypeButton(graphics, this.arrowRightTypeRect, "Flèche D", this.arrowRightType, mouseX, mouseY);

        for (int i = 0; i < this.lineFields.length; i++) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.lineFontRects[i],
                    SignEditorUi.fontLabel(this.lineFonts[i]),
                    false,
                    true,
                    mouseX,
                    mouseY
            );
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(
                        graphics,
                        this.font,
                        "Ligne " + (i + 1),
                        this.lineFields[i].getX(),
                        this.lineFields[i].getY() - s(10)
                );
                SignEditorUi.drawFieldLabel(
                        graphics,
                        this.font,
                        "Police",
                        this.lineFontRects[i].x(),
                        this.lineFontRects[i].y() - s(10)
                );
            }
        }

        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.applyRect,
                "✓  Appliquer",
                true,
                true,
                mouseX,
                mouseY
        );
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.cancelRect,
                "×  Annuler",
                false,
                true,
                mouseX,
                mouseY
        );

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTypeButton(
            GuiGraphicsExtractor graphics,
            SignEditorUi.Rect rect,
            String slot,
            CartoucheType type,
            int mouseX,
            int mouseY
    ) {
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                rect,
                slot + " : " + shortTypeName(type),
                type.isVisible(),
                true,
                mouseX,
                mouseY
        );
    }

    private void drawArrowTypeButton(
            GuiGraphicsExtractor graphics,
            SignEditorUi.Rect rect,
            String slot,
            DA31CArrowType type,
            int mouseX,
            int mouseY
    ) {
        DA31CArrowType safe = safeArrow(type);
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                rect,
                slot + " : " + safe.getDisplayName(),
                safe != DA31CArrowType.NONE,
                true,
                mouseX,
                mouseY
        );
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.previewRect,
                "APERÇU",
                pagedUi() ? "" : "La hauteur de la plaque suit automatiquement la dernière ligne utilisée"
        );

        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + (pagedUi() ? 14 : s(12));
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(6);

        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        int count = getCurrentLineCount();
        int signW = Math.max(s(270), (int) (w * 0.78F));
        int baseSignH = Math.max(s(92), (int) (h * 0.25F));
        int signH = baseSignH + (count - 1) * s(21);
        signH = Math.min(signH, Math.max(s(150), h - s(120)));
        int signX = x + (w - signW) / 2;
        int signY = y + Math.max(s(92), (h - signH) / 2);

        int poleX = x + w / 2;
        int footBottom = y + h - s(8);
        int beamY = signY - s(22);
        int supportGrey = 0xFF2D2D2D;
        int supportGreyLight = 0xFF353535;

        graphics.fill(poleX - s(9), beamY, poleX + s(9), footBottom - s(12), supportGrey);
        graphics.fill(poleX - s(16), footBottom - s(12), poleX + s(16), footBottom, supportGreyLight);

        int beamLeft = signX - s(22);
        int beamRight = signX + signW + s(22);
        graphics.fill(beamLeft, beamY, beamRight, beamY + s(6), supportGrey);
        graphics.fill(beamLeft, beamY + s(15), beamRight, beamY + s(21), supportGrey);
        int braceStep = Math.max(s(28), (beamRight - beamLeft) / 6);
        for (int bx = beamLeft + s(6); bx < beamRight - s(6); bx += braceStep) {
            graphics.fill(bx, beamY + s(6), bx + s(3), beamY + s(15), supportGreyLight);
        }

        graphics.fill(signX + signW / 4 - s(3), beamY + s(8), signX + signW / 4 + s(3), signY + s(6), supportGrey);
        graphics.fill(signX + signW * 3 / 4 - s(3), beamY + s(8), signX + signW * 3 / 4 + s(3), signY + s(6), supportGrey);

        int lowerCartoucheY = signY - s(34);
        int topCartoucheY = lowerCartoucheY - s(34);
        int cartoucheW = s(74);
        int cartoucheH = s(29);

        drawPreviewCartouche(
                graphics,
                this.cartoucheTopType,
                this.cartoucheTopTextField.getValue(),
                signX + signW / 2 - cartoucheW / 2,
                topCartoucheY,
                cartoucheW,
                cartoucheH
        );
        drawPreviewCartouche(
                graphics,
                this.cartoucheLeftType,
                this.cartoucheLeftTextField.getValue(),
                signX + signW / 2 - cartoucheW - s(4),
                lowerCartoucheY,
                cartoucheW,
                cartoucheH
        );
        drawPreviewCartouche(
                graphics,
                this.cartoucheRightType,
                this.cartoucheRightTextField.getValue(),
                signX + signW / 2 + s(4),
                lowerCartoucheY,
                cartoucheW,
                cartoucheH
        );

        graphics.fill(signX, signY, signX + signW, signY + signH, 0xFF0000FF);
        graphics.fill(signX, signY, signX + signW, signY + s(3), 0xFFECECEC);
        graphics.fill(signX, signY + signH - s(3), signX + signW, signY + signH, 0xFFECECEC);
        graphics.fill(signX, signY, signX + s(3), signY + signH, 0xFFECECEC);
        graphics.fill(signX + signW - s(3), signY, signX + signW, signY + signH, 0xFFECECEC);

        int textTop = signY + s(count >= 4 ? 15 : count == 3 ? 17 : 19);
        int textStep = s(count >= 3 ? 24 : 20);
        for (int i = 0; i < count; i++) {
            drawCenteredPreviewText(
                    graphics,
                    this.lineFields[i].getValue(),
                    signX,
                    textTop + i * textStep,
                    signW,
                    0xFFFFFFFF,
                    this.lineFonts[i]
            );
        }

        int arrowPreviewY = signY + signH - s(29);
        drawSelectedArrow(graphics, signX + signW / 4, arrowPreviewY, s(36), this.arrowLeftType);
        drawSelectedArrow(graphics, signX + signW * 3 / 4, arrowPreviewY, s(36), this.arrowRightType);
    }

    private void drawPreviewCartouche(
            GuiGraphicsExtractor graphics,
            CartoucheType type,
            String text,
            int x,
            int y,
            int width,
            int height
    ) {
        if (type == null || !type.isVisible()) {
            return;
        }

        graphics.fill(x, y, x + width, y + height, cartoucheColor(type));
        String display = SignEditorUi.fitText(this.font, safe(text), width - s(8));
        int color = type == CartoucheType.E43 || type == CartoucheType.E44
                ? 0xFF000000
                : 0xFFFFFFFF;
        graphics.text(
                this.font,
                Component.literal(display),
                x + (width - this.font.width(display)) / 2,
                y + (height - this.font.lineHeight) / 2,
                color,
                false
        );
    }

    private void drawCenteredPreviewText(
            GuiGraphicsExtractor graphics,
            String text,
            int x,
            int y,
            int width,
            int color,
            RoadTextFont roadFont
    ) {
        String display = SignEditorUi.fitText(this.font, safe(text), width - s(22));
        Component component = Component.literal(display).withStyle(
                Style.EMPTY.withFont(
                        safeFont(roadFont) == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1
                )
        );
        int drawX = x + (width - this.font.width(component)) / 2;
        graphics.text(this.font, component, drawX, y, color, false);
    }

    private void drawSelectedArrow(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int size,
            DA31CArrowType type
    ) {
        DA31CArrowType safe = safeArrow(type);
        if (safe == DA31CArrowType.NONE) {
            return;
        }
        if (safe == DA31CArrowType.DOWN) {
            int stemW = Math.max(4, size / 5);
            int stemH = Math.max(10, size / 2);
            graphics.fill(
                    centerX - stemW / 2,
                    centerY - size / 2,
                    centerX + stemW / 2,
                    centerY - size / 2 + stemH,
                    0xFFFFFFFF
            );
            int tipY = centerY + size / 2;
            for (int row = 0; row < size / 3; row++) {
                int half = size / 3 - row;
                graphics.fill(centerX - half, tipY - row, centerX + half, tipY - row + 1, 0xFFFFFFFF);
            }
            return;
        }

        boolean left = safe == DA31CArrowType.LEFT;
        drawDiagonalArrow(graphics, centerX, centerY, size, left);
    }

    private void drawDiagonalArrow(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int size,
            boolean left
    ) {
        int direction = left ? -1 : 1;
        int steps = Math.max(8, size / 3);
        int thickness = Math.max(3, size / 9);
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            int px = centerX - direction * size / 4 + Math.round(direction * t * size / 2.0F);
            int py = centerY - size / 3 + Math.round(t * size / 2.0F);
            graphics.fill(
                    px - thickness / 2,
                    py - thickness / 2,
                    px + thickness / 2 + 1,
                    py + thickness / 2 + 1,
                    0xFFFFFFFF
            );
        }
        int tipX = centerX + direction * size / 4;
        int tipY = centerY + size / 6;
        for (int i = 0; i < size / 4; i++) {
            int spread = size / 4 - i;
            int rowY = tipY - i;
            int startX = left ? tipX : tipX - spread;
            int endX = left ? tipX + spread : tipX;
            graphics.fill(startX, rowY, endX + 1, rowY + 1, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (this.cartoucheTopTypeRect.contains(event.x(), event.y())) {
                this.cartoucheTopType = this.cartoucheTopType.next();
                updateFieldStates();
                return true;
            }
            if (this.cartoucheLeftTypeRect.contains(event.x(), event.y())) {
                this.cartoucheLeftType = this.cartoucheLeftType.next();
                updateFieldStates();
                return true;
            }
            if (this.cartoucheRightTypeRect.contains(event.x(), event.y())) {
                this.cartoucheRightType = this.cartoucheRightType.next();
                updateFieldStates();
                return true;
            }
            if (this.arrowLeftTypeRect.contains(event.x(), event.y())) {
                this.arrowLeftType = this.arrowLeftType.next();
                return true;
            }
            if (this.arrowRightTypeRect.contains(event.x(), event.y())) {
                this.arrowRightType = this.arrowRightType.next();
                return true;
            }
            for (int i = 0; i < this.lineFontRects.length; i++) {
                if (this.lineFontRects[i].contains(event.x(), event.y())) {
                    this.lineFonts[i] = this.lineFonts[i].next();
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

    private int getCurrentLineCount() {
        for (int i = this.lineFields.length - 1; i >= 0; i--) {
            EditBox field = this.lineFields[i];
            String value = field == null ? this.currentLines[i] : field.getValue();
            if (value != null && !value.isBlank()) {
                return i + 1;
            }
        }
        return 1;
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
        ClientPacketDistributor.sendToServer(
                new UpdateDA31CPayload(
                        this.blockPos,
                        this.lineFields[0].getValue(),
                        this.lineFields[1].getValue(),
                        this.lineFields[2].getValue(),
                        this.lineFields[3].getValue(),
                        this.lineFonts[0],
                        this.lineFonts[1],
                        this.lineFonts[2],
                        this.lineFonts[3],
                        this.cartoucheTopType.getSerializedName(),
                        this.cartoucheTopTextField.getValue(),
                        this.cartoucheLeftType.getSerializedName(),
                        this.cartoucheLeftTextField.getValue(),
                        this.cartoucheRightType.getSerializedName(),
                        this.cartoucheRightTextField.getValue(),
                        this.arrowLeftType.getSerializedName(),
                        this.arrowRightType.getSerializedName()
                )
        );
        this.onClose();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static CartoucheType safeType(CartoucheType type) {
        return type == null ? CartoucheType.NONE : type;
    }

    private static RoadTextFont safeFont(RoadTextFont font) {
        return font == null ? RoadTextFont.L1 : font;
    }

    private static DA31CArrowType safeArrow(DA31CArrowType type) {
        return type == null ? DA31CArrowType.DOWN : type;
    }

    private static String shortTypeName(CartoucheType type) {
        return switch (safeType(type)) {
            case NONE -> "Aucun";
            case E41_45 -> "Vert";
            case E42 -> "Rouge";
            case E43 -> "Jaune";
            case E44 -> "Blanc";
            case E47 -> "Bleu";
        };
    }

    private static int cartoucheColor(CartoucheType type) {
        return switch (safeType(type)) {
            case E41_45 -> 0xFF1A8F2B;
            case E42 -> 0xFFC40000;
            case E43 -> 0xFFF0D800;
            case E44 -> 0xFFF0F0F0;
            case E47 -> 0xFF295AC8;
            case NONE -> 0x00000000;
        };
    }
}
