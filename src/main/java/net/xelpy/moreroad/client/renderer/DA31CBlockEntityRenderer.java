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
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.DA31CBlock;
import net.xelpy.moreroad.block.entity.DA31CBlockEntity;

/**
 * V6 : le renderer ne dessine plus aucun modèle de cartouche. Les cartouches
 * sont désormais de vrais modèles du blockstate DA31C : plus de rotation
 * doublée, plus de rectangles sur la tranche, plus de texte projeté au sol.
 * Ici on ne rend que les quatre textes, dans le même repère local que la plaque.
 */
public class DA31CBlockEntityRenderer
        implements BlockEntityRenderer<DA31CBlockEntity, DA31CRenderState> {

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    /*
     * V7 : le texte est volontairement un peu plus avancé devant la face.
     * Associé à POLYGON_OFFSET, cela supprime les contours transparents /
     * effets de profondeur qui laissaient entrevoir le décor au bord des glyphes.
     */
    private static final float TEXT_Z = 0.1090F;
    private static final float CARTOUCHE_TEXT_Z = 0.1260F;

    private static final float LINE1_Y = 1.285F;
    private static final float LINE2_Y = 0.925F;
    private static final float LINE_BASE_SCALE = 0.0280F;
    private static final float LINE_MAX_WIDTH = 2.58F;

    /* Centres exacts des deux cartouches V6 (coordonnées locales au panneau). */
    private static final float CARTOUCHE_LEFT_X = 0.4000F;
    private static final float CARTOUCHE_RIGHT_X = -0.4000F;
    private static final float CARTOUCHE_TEXT_Y = 1.8315F;
    private static final float CARTOUCHE_BASE_SCALE = 0.0195F;
    private static final float CARTOUCHE_MAX_WIDTH = 0.58F;

    public DA31CBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public DA31CRenderState createRenderState() {
        return new DA31CRenderState();
    }

    @Override
    public void extractRenderState(
            DA31CBlockEntity blockEntity,
            DA31CRenderState renderState,
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

        renderState.line1 = cleanText(blockEntity.getLine1());
        renderState.line2 = cleanText(blockEntity.getLine2());
        renderState.cartoucheLeftText = cleanText(blockEntity.getCartoucheLeftText());
        renderState.cartoucheRightText = cleanText(blockEntity.getCartoucheRightText());

        /*
         * Les types sont lus dans le blockstate, car c'est lui qui décide quels
         * modèles 3D de cartouches sont réellement visibles. Ainsi le texte ne
         * peut jamais exister sans son cartouche, même lors de la migration
         * d'un ancien monde V5 vers V6.
         */
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof DA31CBlock) {
            renderState.facing = state.getValue(DA31CBlock.FACING);
            renderState.cartoucheLeftType = state.getValue(DA31CBlock.CARTOUCHE_LEFT);
            renderState.cartoucheRightType = state.getValue(DA31CBlock.CARTOUCHE_RIGHT);
        } else {
            renderState.cartoucheLeftType = CartoucheType.NONE;
            renderState.cartoucheRightType = CartoucheType.NONE;
        }
    }

    @Override
    public void submit(
            DA31CRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(renderState.facing)));

        drawCenteredLine(
                collector,
                poseStack,
                font,
                renderState.line1,
                0.0F,
                LINE1_Y,
                TEXT_Z,
                LINE_BASE_SCALE,
                LINE_MAX_WIDTH,
                0xFFFFFFFF,
                0xFF0505B8,
                renderState.lightCoords
        );

        drawCenteredLine(
                collector,
                poseStack,
                font,
                renderState.line2,
                0.0F,
                LINE2_Y,
                TEXT_Z,
                LINE_BASE_SCALE,
                LINE_MAX_WIDTH,
                0xFFFFFFFF,
                0xFF0505B8,
                renderState.lightCoords
        );

        if (renderState.cartoucheLeftType != null
                && renderState.cartoucheLeftType.isVisible()) {
            drawCenteredLine(
                    collector,
                    poseStack,
                    font,
                    renderState.cartoucheLeftText,
                    CARTOUCHE_LEFT_X,
                    CARTOUCHE_TEXT_Y,
                    CARTOUCHE_TEXT_Z,
                    CARTOUCHE_BASE_SCALE,
                    CARTOUCHE_MAX_WIDTH,
                    getCartoucheTextColor(renderState.cartoucheLeftType),
                    getCartoucheBackdropColor(renderState.cartoucheLeftType),
                    renderState.lightCoords
            );
        }

        if (renderState.cartoucheRightType != null
                && renderState.cartoucheRightType.isVisible()) {
            drawCenteredLine(
                    collector,
                    poseStack,
                    font,
                    renderState.cartoucheRightText,
                    CARTOUCHE_RIGHT_X,
                    CARTOUCHE_TEXT_Y,
                    CARTOUCHE_TEXT_Z,
                    CARTOUCHE_BASE_SCALE,
                    CARTOUCHE_MAX_WIDTH,
                    getCartoucheTextColor(renderState.cartoucheRightType),
                    getCartoucheBackdropColor(renderState.cartoucheRightType),
                    renderState.lightCoords
            );
        }

        poseStack.popPose();
    }

    private static void drawCenteredLine(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float x,
            float y,
            float z,
            float baseScale,
            float maxWidth,
            int color,
            int backdropColor,
            int lightCoords
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        FormattedCharSequence text = Component.literal(value)
                .withStyle(Style.EMPTY.withFont(ROAD_FONT_L1))
                .getVisualOrderText();

        int textWidth = font.width(text);
        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWidth / textWidth);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, -scale, scale);

        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.POLYGON_OFFSET,
                lightCoords,
                backdropColor,
                0x00000000,
                0x00000000
        );

        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.POLYGON_OFFSET,
                lightCoords,
                color,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
    }

    private static int getCartoucheTextColor(CartoucheType type) {
        return type == CartoucheType.E43 || type == CartoucheType.E44
                ? 0xFF000000
                : 0xFFFFFFFF;
    }

    private static int getCartoucheBackdropColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> 0xFF1B8F2D;
            case E42 -> 0xFFC40000;
            case E43 -> 0xFFF2F2F2;
            case E44 -> 0xFFD5C400;
            case E47 -> 0xFF2A56C7;
            default -> 0xFF0505B8;
        };
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
