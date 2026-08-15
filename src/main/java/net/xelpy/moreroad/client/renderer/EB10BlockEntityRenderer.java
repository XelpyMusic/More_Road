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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.block.custom.EB10Block;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;

public class EB10BlockEntityRenderer
        implements BlockEntityRenderer<EB10BlockEntity, EB10RenderState> {

    /*
     * Position du texte sur le modèle.
     *
     * On pourra modifier ces valeurs très facilement ensuite
     * pour l'aligner au pixel près dans la zone grise.
     */
    private static final float TEXT_Y = 0.72F;
    private static final float TEXT_Z = 0.066F;

    /*
     * Taille maximale normale du texte.
     */
    private static final float BASE_SCALE = 0.0125F;

    /*
     * Largeur maximale occupée sur le panneau,
     * exprimée en blocs.
     */
    private static final float MAX_WORLD_WIDTH = 0.72F;

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

        renderState.cityName = blockEntity.getCityName();

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
        if (renderState.cityName == null ||
                renderState.cityName.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        FormattedCharSequence text =
                Component.literal(renderState.cityName)
                        .getVisualOrderText();

        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        /*
         * Réduit automatiquement les noms trop longs.
         *
         * Un nom court garde BASE_SCALE.
         * Un nom long est réduit pour rester dans la pancarte.
         */
        float scale = Math.min(
                BASE_SCALE,
                MAX_WORLD_WIDTH / textWidth
        );

        poseStack.pushPose();

        /*
         * Centre du bloc puis hauteur de la zone de texte.
         */
        poseStack.translate(
                0.5F,
                TEXT_Y,
                0.5F
        );

        /*
         * Oriente le texte vers la face avant du panneau.
         */
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

        /*
         * Sort légèrement le texte de la surface du panneau
         * pour éviter qu'il fusionne avec le modèle.
         */
        poseStack.translate(
                0F,
                0F,
                TEXT_Z
        );

        /*
         * Y est négatif parce que le repère du texte
         * doit être retourné pour être lisible dans le monde.
         */
        poseStack.scale(
                scale,
                -scale,
                scale
        );

        collector.submitText(
                poseStack,

                // Centrage horizontal
                -textWidth / 2.0F,

                // Minecraft fait environ 9 pixels de haut
                -4.5F,

                text,

                false,

                Font.DisplayMode.NORMAL,

                renderState.lightCoords,

                // Noir opaque
                0xFF000000,

                // Pas de fond
                0x00000000,

                // Pas de contour
                0x00000000
        );

        poseStack.popPose();
    }
}