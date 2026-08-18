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
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.EB10Block;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;

import java.util.Locale;

public class EB10BlockEntityRenderer
        implements BlockEntityRenderer<EB10BlockEntity, EB10RenderState> {

    private static final FontDescription.Resource ROAD_FONT =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    /*
     * Le grand modèle EB10/EB20 est centré à X = 8.
     * Le texte doit donc rester vraiment centré horizontalement.
     */
    private static final float TEXT_X_OFFSET = 0.0F;

    /*
     * Le texte est rendu légèrement devant la face pour rester visible.
     */
    private static final float TEXT_Z = 0.128F;

    /*
     * Une ligne : encore un tout petit peu plus bas.
     */
    private static final float SINGLE_LINE_Y = 0.560F;
    private static final float SINGLE_LINE_BASE_SCALE = 0.0205F;

    /*
     * Deux lignes : même ajustement vertical léger.
     */
    private static final float FIRST_LINE_Y = 0.640F;
    private static final float SECOND_LINE_Y = 0.480F;
    private static final float TWO_LINES_BASE_SCALE = 0.0155F;

    /*
     * La plaque est large : on laisse une grande zone utile avant de réduire.
     */
    private static final float MAX_TEXT_WORLD_WIDTH = 1.28F;

    public EB10BlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public EB10RenderState createRenderState() {
        return new EB10RenderState();
    }

    @Override
    public void extractRenderState(
            EB10BlockEntity blockEntity,
            EB10RenderState renderState,
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

        renderState.facing = blockEntity
                .getBlockState()
                .getValue(EB10Block.FACING);
    }

    @Override
    public void submit(
            EB10RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        String line1 = cleanText(renderState.line1);
        String line2 = cleanText(renderState.line2);

        if (line1.isBlank() && line2.isBlank()) {
            return;
        }

        if (line2.isBlank()) {
            submitLine(
                    line1,
                    SINGLE_LINE_Y,
                    SINGLE_LINE_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );
            return;
        }

        if (!line1.isBlank()) {
            submitLine(
                    line1,
                    FIRST_LINE_Y,
                    TWO_LINES_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!line2.isBlank()) {
            submitLine(
                    line2,
                    SECOND_LINE_Y,
                    TWO_LINES_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private static void submitLine(
            String value,
            float worldY,
            float baseScale,
            EB10RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        Component component =
                Component.literal(value)
                        .withStyle(
                                Style.EMPTY.withFont(ROAD_FONT)
                        );

        FormattedCharSequence text =
                component.getVisualOrderText();

        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(
                baseScale,
                MAX_TEXT_WORLD_WIDTH / textWidth
        );

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                worldY,
                0.5F
        );

        float rotation = switch (renderState.facing) {
            case SOUTH -> 0F;
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };

        poseStack.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );

        poseStack.translate(
                TEXT_X_OFFSET,
                0F,
                TEXT_Z
        );

        poseStack.scale(
                scale,
                -scale,
                scale
        );

        float textX = -textWidth / 2.0F;
        float textY = -font.lineHeight / 2.0F;

        collector.submitText(
                poseStack,
                textX,
                textY,
                text,
                false,
                Font.DisplayMode.NORMAL,
                renderState.lightCoords,
                0xFF000000,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
    }

    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .strip()
                .toUpperCase(Locale.ROOT);
    }
}
