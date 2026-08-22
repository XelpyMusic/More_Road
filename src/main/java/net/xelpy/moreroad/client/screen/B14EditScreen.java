package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.B14Speed;
import net.xelpy.moreroad.network.UpdateB14Payload;

public class B14EditScreen extends Screen {

    private final BlockPos blockPos;
    private B14Speed selectedSpeed;

    private final SignEditorUi.Rect[] speedRects = new SignEditorUi.Rect[B14Speed.values().length];
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect speedsRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public B14EditScreen(BlockPos blockPos, B14Speed currentSpeed) {
        super(Component.literal("B14 — Limitation de vitesse"));
        this.blockPos = blockPos.immutable();
        this.selectedSpeed = currentSpeed == null ? B14Speed.KMH_5 : currentSpeed;
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(10, Math.min(40, this.width / 20));
        int marginY = Math.max(8, Math.min(28, this.height / 20));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 900.0F, 580.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(260), Math.round((this.windowWidth - pad * 2 - gap) * 0.44F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.speedsRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.speedsRect.x() + s(12);
        int innerW = this.speedsRect.width() - s(24);
        int cols = pagedUi() ? 5 : (this.width < 700 ? 4 : 5);
        int buttonGap = pagedUi() ? 5 : s(compactUi() ? 5 : 8);
        int buttonW = (innerW - buttonGap * (cols - 1)) / cols;
        int buttonH = pagedUi() ? 20 : s(30);
        int startY = this.speedsRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 48));

        for (int i = 0; i < this.speedRects.length; i++) {
            int col = i % cols;
            int row = i / cols;
            this.speedRects[i] = new SignEditorUi.Rect(
                    innerX + col * (buttonW + buttonGap),
                    startY + row * (buttonH + buttonGap),
                    buttonW,
                    buttonH
            );
        }

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(74, Math.max(s(145), Math.max(this.font.width("✓  Appliquer"), this.font.width("×  Annuler")) + 18));
        int actionGap = Math.max(4, s(10));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH);
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - actionGap - actionW, actionY, actionW, actionH);
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
                "B14",
                "Limitation de vitesse",
                compactUi() ? "" : "Configuration en direct"
        );

        SignEditorUi.drawB14Preview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                Integer.toString(this.selectedSpeed.value())
        );

        SignEditorUi.drawModernSection(graphics, this.font, this.speedsRect, "VITESSE", pagedUi() ? "" : "Choisissez la limitation affichée sur le panneau");
        B14Speed[] speeds = B14Speed.values();
        for (int i = 0; i < speeds.length; i++) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.speedRects[i],
                    speeds[i].value() + " km/h",
                    speeds[i] == this.selectedSpeed,
                    true,
                    mouseX,
                    mouseY
            );
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
            B14Speed[] speeds = B14Speed.values();
            for (int i = 0; i < this.speedRects.length; i++) {
                if (this.speedRects[i].contains(x, y)) {
                    this.selectedSpeed = speeds[i];
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
        ClientPacketDistributor.sendToServer(new UpdateB14Payload(this.blockPos, this.selectedSpeed));
        this.onClose();
    }
}
