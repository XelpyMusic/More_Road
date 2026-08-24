package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.network.UpdateDA31CPayload;

/**
 * Éditeur DA31C V6.
 *
 * Les deux cartouches fonctionnent maintenant comme sur les autres panneaux
 * modifiables : choix du type/couleur, puis saisie du texte. "Aucun" masque
 * totalement le cartouche dans le monde.
 */
public class DA31CEditScreen extends Screen {

    private static final int MAX_LINE_LENGTH = 48;
    private static final int MAX_CARTOUCHE_LENGTH = 24;

    private final BlockPos blockPos;
    private final String currentLine1;
    private final String currentLine2;

    private CartoucheType cartoucheLeftType;
    private final String currentCartoucheLeftText;

    private CartoucheType cartoucheRightType;
    private final String currentCartoucheRightText;

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox cartoucheLeftTextField;
    private EditBox cartoucheRightTextField;

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect cartoucheLeftTypeRect;
    private SignEditorUi.Rect cartoucheRightTypeRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public DA31CEditScreen(
            BlockPos blockPos,
            String currentLine1,
            String currentLine2,
            CartoucheType cartoucheLeftType,
            String currentCartoucheLeftText,
            CartoucheType cartoucheRightType,
            String currentCartoucheRightText
    ) {
        super(Component.literal("DA31C — Autoroute"));
        this.blockPos = blockPos.immutable();
        this.currentLine1 = currentLine1 == null ? "" : currentLine1;
        this.currentLine2 = currentLine2 == null ? "" : currentLine2;
        this.cartoucheLeftType = cartoucheLeftType == null ? CartoucheType.NONE : cartoucheLeftType;
        this.currentCartoucheLeftText = currentCartoucheLeftText == null ? "" : currentCartoucheLeftText;
        this.cartoucheRightType = cartoucheRightType == null ? CartoucheType.NONE : cartoucheRightType;
        this.currentCartoucheRightText = currentCartoucheRightText == null ? "" : currentCartoucheRightText;
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
                1080.0F,
                660.0F
        );

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(340), Math.round((this.windowWidth - pad * 2 - gap) * 0.52F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.contentRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.contentRect.x() + s(12);
        int innerW = this.contentRect.width() - s(24);
        int fieldH = pagedUi() ? 20 : s(24);
        int selectorH = SignEditorUi.safeControlHeight(this.font, s(24));
        int selectorW = Math.max(s(126), Math.round(innerW * 0.38F));
        int rowGap = s(8);
        int startY = this.contentRect.y() + (pagedUi() ? 30 : s(42));

        this.cartoucheLeftTypeRect = new SignEditorUi.Rect(
                innerX,
                startY,
                selectorW,
                selectorH
        );
        this.cartoucheLeftTextField = new EditBox(
                this.font,
                innerX + selectorW + rowGap,
                startY,
                innerW - selectorW - rowGap,
                fieldH,
                Component.literal("Texte cartouche gauche")
        );
        this.cartoucheLeftTextField.setMaxLength(MAX_CARTOUCHE_LENGTH);
        this.cartoucheLeftTextField.setValue(this.currentCartoucheLeftText);
        this.addRenderableWidget(this.cartoucheLeftTextField);

        int row2Y = startY + Math.max(selectorH, fieldH) + s(34);
        this.cartoucheRightTypeRect = new SignEditorUi.Rect(
                innerX,
                row2Y,
                selectorW,
                selectorH
        );
        this.cartoucheRightTextField = new EditBox(
                this.font,
                innerX + selectorW + rowGap,
                row2Y,
                innerW - selectorW - rowGap,
                fieldH,
                Component.literal("Texte cartouche droit")
        );
        this.cartoucheRightTextField.setMaxLength(MAX_CARTOUCHE_LENGTH);
        this.cartoucheRightTextField.setValue(this.currentCartoucheRightText);
        this.addRenderableWidget(this.cartoucheRightTextField);

        int line1Y = row2Y + Math.max(selectorH, fieldH) + s(42);
        this.line1Field = new EditBox(
                this.font,
                innerX,
                line1Y,
                innerW,
                fieldH,
                Component.literal("Ligne 1")
        );
        this.line1Field.setMaxLength(MAX_LINE_LENGTH);
        this.line1Field.setValue(this.currentLine1);
        this.addRenderableWidget(this.line1Field);

        this.line2Field = new EditBox(
                this.font,
                innerX,
                line1Y + fieldH + s(30),
                innerW,
                fieldH,
                Component.literal("Ligne 2")
        );
        this.line2Field.setMaxLength(MAX_LINE_LENGTH);
        this.line2Field.setValue(this.currentLine2);
        this.addRenderableWidget(this.line2Field);

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
        this.setInitialFocus(this.line1Field);
    }

    private void updateFieldStates() {
        if (this.cartoucheLeftTextField != null) {
            this.cartoucheLeftTextField.active = this.cartoucheLeftType.isVisible();
        }
        if (this.cartoucheRightTextField != null) {
            this.cartoucheRightTextField.active = this.cartoucheRightType.isVisible();
        }
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
                compactUi()
                        ? ""
                        : "Deux cartouches indépendants + montage arrière sur la traverse"
        );

        drawPreview(graphics);

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "CONTENU",
                pagedUi()
                        ? ""
                        : "Clique sur le type pour choisir Aucun / Vert / Rouge / Jaune / Blanc / Bleu"
        );

        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.cartoucheLeftTypeRect,
                SignEditorUi.cartoucheLabel(this.cartoucheLeftType),
                this.cartoucheLeftType.isVisible(),
                true,
                mouseX,
                mouseY
        );

        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.cartoucheRightTypeRect,
                SignEditorUi.cartoucheLabel(this.cartoucheRightType),
                this.cartoucheRightType.isVisible(),
                true,
                mouseX,
                mouseY
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Cartouche gauche — type",
                    this.cartoucheLeftTypeRect.x(),
                    this.cartoucheLeftTypeRect.y() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Texte",
                    this.cartoucheLeftTextField.getX(),
                    this.cartoucheLeftTextField.getY() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Cartouche droit — type",
                    this.cartoucheRightTypeRect.x(),
                    this.cartoucheRightTypeRect.y() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Texte",
                    this.cartoucheRightTextField.getX(),
                    this.cartoucheRightTextField.getY() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Destination — ligne 1",
                    this.line1Field.getX(),
                    this.line1Field.getY() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Destination — ligne 2",
                    this.line2Field.getX(),
                    this.line2Field.getY() - s(12)
            );
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

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.previewRect,
                "APERÇU",
                pagedUi() ? "" : "DA31C + cartouches + portique"
        );

        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + (pagedUi() ? 14 : s(12));
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(6);

        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        int signW = Math.max(s(250), (int) (w * 0.78F));
        int signH = Math.max(s(140), (int) (h * 0.43F));
        int signX = x + (w - signW) / 2;
        int signY = y + h / 2 - signH / 2 + s(30);

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

        /* Deux bras arrière reliant visuellement la plaque à la traverse. */
        graphics.fill(signX + signW / 4 - s(3), beamY + s(8), signX + signW / 4 + s(3), signY + s(6), supportGrey);
        graphics.fill(signX + signW * 3 / 4 - s(3), beamY + s(8), signX + signW * 3 / 4 + s(3), signY + s(6), supportGrey);

        drawPreviewCartouche(
                graphics,
                this.cartoucheLeftType,
                this.cartoucheLeftTextField == null
                        ? this.currentCartoucheLeftText
                        : this.cartoucheLeftTextField.getValue(),
                signX + signW / 2 - s(70),
                signY - s(34),
                s(64),
                s(26)
        );

        drawPreviewCartouche(
                graphics,
                this.cartoucheRightType,
                this.cartoucheRightTextField == null
                        ? this.currentCartoucheRightText
                        : this.cartoucheRightTextField.getValue(),
                signX + signW / 2 + s(6),
                signY - s(34),
                s(64),
                s(26)
        );

        graphics.fill(signX, signY, signX + signW, signY + signH, 0xFF0B72B8);
        graphics.fill(signX + s(2), signY + s(2), signX + signW - s(2), signY + signH - s(2), 0xFF0E6FB5);

        String line1 = this.line1Field == null ? this.currentLine1 : this.line1Field.getValue();
        String line2 = this.line2Field == null ? this.currentLine2 : this.line2Field.getValue();
        drawCenteredPreviewText(graphics, line1, signX, signY + s(30), signW, 0xFFFFFFFF);
        drawCenteredPreviewText(graphics, line2, signX, signY + s(64), signW, 0xFFFFFFFF);

        int arrowY = signY + signH - s(48);
        drawArrow(graphics, signX + signW / 4, arrowY, s(68));
        drawArrow(graphics, signX + signW * 3 / 4, arrowY, s(68));
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

        int bg = switch (type) {
            case E41_45 -> 0xFF009B45;
            case E42 -> 0xFFD71920;
            case E43 -> 0xFFF2CF16;
            case E44 -> 0xFFF4F4F4;
            case E47 -> 0xFF1555A5;
            default -> 0xFFF4F4F4;
        };
        int fg = type == CartoucheType.E43 || type == CartoucheType.E44
                ? 0xFF000000
                : 0xFFFFFFFF;

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF252525);
        graphics.fill(x, y, x + width, y + height, bg);

        String safe = text == null ? "" : text;
        String display = SignEditorUi.fitText(this.font, safe, width - s(8));
        graphics.text(
                this.font,
                Component.literal(display),
                x + (width - this.font.width(display)) / 2,
                y + Math.max(2, (height - this.font.lineHeight) / 2),
                fg,
                false
        );
    }

    private void drawCenteredPreviewText(
            GuiGraphicsExtractor graphics,
            String text,
            int x,
            int y,
            int width,
            int color
    ) {
        String safe = text == null ? "" : text;
        String display = SignEditorUi.fitText(this.font, safe, width - s(22));
        int drawX = x + (width - this.font.width(display)) / 2;
        graphics.text(this.font, Component.literal(display), drawX, y, color, false);
    }

    private void drawArrow(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int size
    ) {
        int headHalf = Math.max(10, size / 4);
        int stemHalf = Math.max(4, size / 16);
        int stemTop = centerY - size / 2;
        int stemBottom = centerY - size / 8;
        graphics.fill(centerX - stemHalf, stemTop, centerX + stemHalf, stemBottom, 0xFFECECEC);
        for (int i = 0; i < headHalf; i++) {
            graphics.fill(
                    centerX - headHalf + i,
                    stemBottom + i / 2,
                    centerX + headHalf - i,
                    stemBottom + i / 2 + 1,
                    0xFFECECEC
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
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
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.cartoucheLeftType.getSerializedName(),
                        this.cartoucheLeftTextField.getValue(),
                        this.cartoucheRightType.getSerializedName(),
                        this.cartoucheRightTextField.getValue()
                )
        );
        this.onClose();
    }
}
