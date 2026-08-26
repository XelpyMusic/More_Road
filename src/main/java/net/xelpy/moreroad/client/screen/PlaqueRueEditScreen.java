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
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.PlaqueRueBlockEntity;
import net.xelpy.moreroad.network.UpdatePlaqueRueTextPayload;

/** Éditeur à deux lignes de la plaque de rue. */
public class PlaqueRueEditScreen extends Screen {

    private static final int MAX_TEXT_LENGTH = 64;

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );
    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    private final BlockPos blockPos;
    private final String currentLine1;
    private final String currentLine2;

    private RoadTextFont line1Font;
    private RoadTextFont line2Font;

    private EditBox line1Field;
    private EditBox line2Field;
    private SignEditorUi.Rect line1FontRect;
    private SignEditorUi.Rect line2FontRect;
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public PlaqueRueEditScreen(
            BlockPos blockPos,
            String currentLine1,
            String currentLine2,
            RoadTextFont line1Font,
            RoadTextFont line2Font
    ) {
        super(Component.literal("Plaque de rue"));
        this.blockPos = blockPos.immutable();
        this.currentLine1 = currentLine1 == null ? "" : currentLine1;
        this.currentLine2 = currentLine2 == null ? "" : currentLine2;
        this.line1Font = line1Font == null ? RoadTextFont.NORMAL : line1Font;
        this.line2Font = line2Font == null ? RoadTextFont.NORMAL : line2Font;
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
                900.0F,
                560.0F
        );

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
        int fontW = Math.max(s(118), this.font.width("L4 - Italique") + s(24));
        int fieldGap = s(8);
        int textW = Math.max(s(120), innerW - fontW - fieldGap);
        int fieldH = Math.max(20, s(24));
        int startY = this.contentRect.y() + s(compactUi() ? 38 : 62);
        int rowGap = s(compactUi() ? 42 : 58);

        this.line1Field = new EditBox(
                this.font,
                innerX,
                startY,
                textW,
                fieldH,
                Component.literal("Ligne 1")
        );
        this.line1Field.setMaxLength(MAX_TEXT_LENGTH);
        this.line1Field.setValue(this.currentLine1);
        this.addRenderableWidget(this.line1Field);
        this.line1FontRect = new SignEditorUi.Rect(
                innerX + textW + fieldGap,
                startY,
                fontW,
                fieldH
        );

        int secondY = startY + rowGap;
        this.line2Field = new EditBox(
                this.font,
                innerX,
                secondY,
                textW,
                fieldH,
                Component.literal("Ligne 2")
        );
        this.line2Field.setMaxLength(MAX_TEXT_LENGTH);
        this.line2Field.setValue(this.currentLine2);
        this.addRenderableWidget(this.line2Field);
        this.line2FontRect = new SignEditorUi.Rect(
                innerX + textW + fieldGap,
                secondY,
                fontW,
                fieldH
        );

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(
                74,
                Math.max(s(145), Math.max(
                        this.font.width("✓  Appliquer"),
                        this.font.width("×  Annuler")
                ) + 18)
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

        this.setInitialFocus(this.line1Field);
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
                "RUE",
                "Plaque de rue personnalisable",
                compactUi() ? "" : "Deux lignes indépendantes avec police normale, standard ou italique"
        );

        drawPreview(graphics);

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "CONTENU",
                compactUi() ? "" : "Texte et police de chaque ligne"
        );

        if (!compactUi()) {
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Ligne 1",
                    this.line1Field.getX(),
                    this.line1Field.getY() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Police",
                    this.line1FontRect.x(),
                    this.line1FontRect.y() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Ligne 2",
                    this.line2Field.getX(),
                    this.line2Field.getY() - s(12)
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Police",
                    this.line2FontRect.x(),
                    this.line2FontRect.y() - s(12)
            );
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
                this.line2FontRect,
                SignEditorUi.fontLabel(this.line2Font),
                false,
                true,
                mouseX,
                mouseY
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

    private void drawPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.previewRect,
                "APERÇU",
                compactUi() ? "" : "Rendu indicatif de la plaque"
        );

        int usableW = this.previewRect.width() - s(50);
        int plateW = Math.min(usableW, s(390));
        int plateH = Math.max(s(100), Math.round(plateW * 0.42F));
        int plateX = this.previewRect.x() + (this.previewRect.width() - plateW) / 2;
        int plateY = this.previewRect.y() + (this.previewRect.height() - plateH) / 2 + s(6);

        int border = Math.max(2, s(3));
        graphics.fill(plateX, plateY, plateX + plateW, plateY + plateH, 0xFFFFFFFF);
        graphics.fill(
                plateX + border,
                plateY + border,
                plateX + plateW - border,
                plateY + plateH - border,
                0xFF10258A
        );

        String line1 = this.line1Field == null ? this.currentLine1 : this.line1Field.getValue();
        String line2 = this.line2Field == null ? this.currentLine2 : this.line2Field.getValue();
        int maxWidth = plateW - s(30);

        if (line2.isBlank()) {
            drawPreviewLine(
                    graphics,
                    line1,
                    this.line1Font,
                    plateX + plateW / 2,
                    plateY + (plateH - this.font.lineHeight) / 2,
                    maxWidth
            );
            return;
        }

        int firstY = plateY + plateH / 3 - this.font.lineHeight / 2;
        int secondY = plateY + plateH * 2 / 3 - this.font.lineHeight / 2;
        drawPreviewLine(
                graphics,
                line1,
                this.line1Font,
                plateX + plateW / 2,
                firstY,
                maxWidth
        );
        drawPreviewLine(
                graphics,
                line2,
                this.line2Font,
                plateX + plateW / 2,
                secondY,
                maxWidth
        );
    }

    private void drawPreviewLine(
            GuiGraphicsExtractor graphics,
            String value,
            RoadTextFont roadFont,
            int centerX,
            int y,
            int maxWidth
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        Component component = fitStyledComponent(value, roadFont, maxWidth);
        int x = centerX - this.font.width(component) / 2;
        graphics.text(this.font, component, x, y, 0xFFFFFFFF, false);
    }

    private Component fitStyledComponent(String value, RoadTextFont roadFont, int maxWidth) {
        String clean = value == null ? "" : value;
        Component component = roadComponent(clean, roadFont);
        if (this.font.width(component) <= maxWidth) {
            return component;
        }

        String suffix = "…";
        int end = clean.length();
        while (end > 0) {
            Component candidate = roadComponent(clean.substring(0, end) + suffix, roadFont);
            if (this.font.width(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return roadComponent(suffix, roadFont);
    }

    private static Component roadComponent(String value, RoadTextFont roadFont) {
        String text = value == null ? "" : value;
        if (roadFont == RoadTextFont.NORMAL) {
            return Component.literal(text);
        }
        FontDescription.Resource resource = roadFont == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;
        return Component.literal(text).withStyle(Style.EMPTY.withFont(resource));
    }

    /** Cycle propre à la plaque : Normal -> Standard L1 -> Italique L4. */
    private static RoadTextFont nextPlaqueFont(RoadTextFont current) {
        if (current == null) {
            return RoadTextFont.NORMAL;
        }
        return switch (current) {
            case NORMAL -> RoadTextFont.L1;
            case L1 -> RoadTextFont.L4;
            case L4 -> RoadTextFont.NORMAL;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();
            if (this.line1FontRect.contains(x, y)) {
                this.line1Font = nextPlaqueFont(this.line1Font);
                return true;
            }
            if (this.line2FontRect.contains(x, y)) {
                this.line2Font = nextPlaqueFont(this.line2Font);
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

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        String line1 = this.line1Field.getValue();
        String line2 = this.line2Field.getValue();

        /*
         * Mise à jour optimiste côté client : le texte apparaît immédiatement
         * sur la plaque, sans attendre le retour du paquet serveur.
         */
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(this.blockPos) instanceof PlaqueRueBlockEntity blockEntity) {
            blockEntity.setContent(line1, line2, this.line1Font, this.line2Font);
        }

        ClientPacketDistributor.sendToServer(
                new UpdatePlaqueRueTextPayload(
                        this.blockPos,
                        line1,
                        line2,
                        this.line1Font,
                        this.line2Font
                )
        );
        this.onClose();
    }
}
