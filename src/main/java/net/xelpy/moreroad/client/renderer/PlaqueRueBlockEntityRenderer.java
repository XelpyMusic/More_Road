package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.PlaqueRueBlock;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.PlaqueRueBlockEntity;

/** Rendu des deux lignes blanches personnalisables sur la face avant de la plaque. */
public class PlaqueRueBlockEntityRenderer
        implements BlockEntityRenderer<PlaqueRueBlockEntity, PlaqueRueRenderState> {

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    /* Centre vertical du panneau sur poteau : Y 6 -> 12. */
    private static final float STANDING_CENTER_Y = 0.5625F;
    /* Face avant NORTH du modèle sur poteau : Z = 7/16. */
    private static final float STANDING_TEXT_Z = 0.0645F;

    /* Centre vertical du panneau mural : Y 5 -> 11. */
    private static final float WALL_CENTER_Y = 0.5000F;
    /*
     * Modèle mural NORTH : face bleue à Z = 14/16. Après la rotation NORTH
     * de 180°, -0.3720 place le texte juste DEVANT la face (et non dedans).
     */
    private static final float WALL_TEXT_Z = -0.3720F;

    private static final float SINGLE_LINE_SCALE = 0.0135F;
    /* Deux lignes volontairement plus petites que la version précédente. */
    private static final float TWO_LINES_SCALE = 0.0096F;
    private static final float SINGLE_LINE_MAX_TEXT_WIDTH = 0.645F;
    private static final float TWO_LINES_MAX_TEXT_WIDTH = 0.600F;
    /* Centres des deux lignes, symétriques autour du centre réel de la plaque. */
    private static final float TWO_LINES_OFFSET_Y = 0.055F;

    public PlaqueRueBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PlaqueRueRenderState createRenderState() {
        return new PlaqueRueRenderState();
    }

    @Override
    public void extractRenderState(
            PlaqueRueBlockEntity blockEntity,
            PlaqueRueRenderState renderState,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                renderState,
                partialTick,
                cameraPos,
                crumblingOverlay
        );

        renderState.line1 = blockEntity.getLine1();
        renderState.line2 = blockEntity.getLine2();
        renderState.line1Font = blockEntity.getLine1Font();
        renderState.line2Font = blockEntity.getLine2Font();

        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof PlaqueRueBlock) {
            renderState.facing = state.getValue(PlaqueRueBlock.FACING);
            renderState.face = state.getValue(PlaqueRueBlock.FACE);
        }
    }

    @Override
    public void submit(
            PlaqueRueRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        String line1 = cleanText(renderState.line1);
        String line2 = cleanText(renderState.line2);
        if (line1.isBlank() && line2.isBlank()) {
            return;
        }

        boolean wall = renderState.face == AttachFace.WALL;
        float centerY = wall ? WALL_CENTER_Y : STANDING_CENTER_Y;
        float textZ = wall ? WALL_TEXT_Z : STANDING_TEXT_Z;

        if (line1.isBlank() || line2.isBlank()) {
            String singleLine = line1.isBlank() ? line2 : line1;
            RoadTextFont singleFont = line1.isBlank()
                    ? renderState.line2Font
                    : renderState.line1Font;
            submitLine(
                    singleLine,
                    singleFont,
                    centerY,
                    textZ,
                    SINGLE_LINE_SCALE,
                    SINGLE_LINE_MAX_TEXT_WIDTH,
                    renderState,
                    poseStack,
                    collector
            );
            return;
        }

        /*
         * Les deux lignes partagent exactement la même échelle. La ligne la
         * plus longue fixe la réduction, ce qui évite l'effet d'une ligne
         * énorme au-dessus d'une autre plus petite et recentre visuellement
         * tout le bloc de texte.
         */
        int line1Width = textWidth(line1, renderState.line1Font);
        int line2Width = textWidth(line2, renderState.line2Font);
        int widest = Math.max(1, Math.max(line1Width, line2Width));
        float sharedScale = Math.min(
                TWO_LINES_SCALE,
                TWO_LINES_MAX_TEXT_WIDTH / widest
        );

        submitLine(
                line1,
                renderState.line1Font,
                centerY + TWO_LINES_OFFSET_Y,
                textZ,
                sharedScale,
                TWO_LINES_MAX_TEXT_WIDTH,
                renderState,
                poseStack,
                collector
        );
        submitLine(
                line2,
                renderState.line2Font,
                centerY - TWO_LINES_OFFSET_Y,
                textZ,
                sharedScale,
                TWO_LINES_MAX_TEXT_WIDTH,
                renderState,
                poseStack,
                collector
        );
    }

    private static void submitLine(
            String value,
            RoadTextFont roadFont,
            float worldY,
            float textZ,
            float baseScale,
            float maxTextWidth,
            PlaqueRueRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component component = styledComponent(value, roadFont);
        FormattedCharSequence text = component.getVisualOrderText();
        int textWidth = font.width(text);
        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxTextWidth / textWidth);

        poseStack.pushPose();
        poseStack.translate(0.5F, worldY, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(renderState.facing)));
        poseStack.translate(0.0F, 0.0F, textZ);
        poseStack.scale(scale, -scale, scale);

        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.NORMAL,
                renderState.lightCoords,
                0xFFFFFFFF,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
    }

    private static Component styledComponent(String value, RoadTextFont font) {
        String text = value == null ? "" : value;
        if (font == RoadTextFont.NORMAL) {
            return Component.literal(text);
        }
        FontDescription.Resource resource = font == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;
        return Component.literal(text).withStyle(Style.EMPTY.withFont(resource));
    }

    private static int textWidth(String value, RoadTextFont fontType) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Minecraft.getInstance().font.width(styledComponent(value, fontType));
    }

    private static float getFacingRotation(net.minecraft.core.Direction facing) {
        if (facing == null) {
            return 180.0F;
        }
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> -90.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static String cleanText(String value) {
        return value == null ? "" : value.strip();
    }
}
