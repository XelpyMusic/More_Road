package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.network.UpdateE31TextPayload;

public class E31EditScreen extends Screen {

    private static final int MAX_TEXT_LENGTH = 48;

    private final BlockPos blockPos;
    private final String currentText;
    private final boolean waterName;

    private EditBox textField;
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public E31EditScreen(BlockPos blockPos, String currentText, boolean waterName) {
        super(Component.literal(waterName ? "E31b — Nom des cours d'eau" : "E31a — Lieu-dit"));
        this.blockPos = blockPos.immutable();
        this.currentText = currentText == null ? "" : currentText;
        this.waterName = waterName;
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
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 900.0F, 560.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(280), Math.round((this.windowWidth - pad * 2 - gap) * 0.50F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.contentRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.contentRect.x() + s(12);
        int innerW = this.contentRect.width() - s(24);
        int fieldY = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 54));
        this.textField = new EditBox(
                this.font,
                innerX,
                fieldY,
                innerW,
                pagedUi() ? 20 : s(24),
                Component.literal("Texte du panneau")
        );
        this.textField.setMaxLength(MAX_TEXT_LENGTH);
        this.textField.setValue(this.currentText);
        this.addRenderableWidget(this.textField);

        int actionY = this.windowY + this.windowHeight - s(36);
        int actionW = s(145);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, s(28));
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, s(28));

        this.setInitialFocus(this.textField);
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
                this.waterName ? "E31b" : "E31a",
                this.waterName ? "Nom des cours d'eau" : "Lieu-dit",
                compactUi() ? "" : "Configuration en direct"
        );

        SignEditorUi.drawE31Preview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                this.waterName,
                this.textField == null ? this.currentText : this.textField.getValue()
        );

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "CONTENU",
                pagedUi() ? "" : (this.waterName ? "Nom du cours d'eau affiché sur le panneau" : "Nom du lieu-dit affiché sur le panneau")
        );
        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(graphics, this.font, "Texte", this.textField.getX(), this.textField.getY() - s(12));
            graphics.text(
                    this.font,
                    Component.literal("Police : Italique"),
                    this.contentRect.x() + s(12),
                    this.textField.getY() + s(38),
                    SignEditorUi.MODERN_MUTED,
                    false
            );
            graphics.text(
                    this.font,
                    Component.literal("La police de ce panneau est imposée par le modèle de signalisation."),
                    this.contentRect.x() + s(12),
                    this.textField.getY() + s(52),
                    SignEditorUi.MODERN_MUTED,
                    false
            );
        }

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
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
        ClientPacketDistributor.sendToServer(new UpdateE31TextPayload(this.blockPos, this.textField.getValue()));
        this.onClose();
    }
}
