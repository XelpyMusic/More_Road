package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.CartoucheLayout;
import net.xelpy.moreroad.block.custom.CartoucheModelBlock;
import net.xelpy.moreroad.block.custom.EB10Block;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;


public class EB10BlockEntityRenderer
        implements BlockEntityRenderer<EB10BlockEntity, EB10RenderState> {

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l4"
                    )
            );

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

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

    private final BlockModelResolver blockResolver;

    public EB10BlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockResolver = context.blockModelResolver();
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
        renderState.line1Font = blockEntity.getLine1Font();
        renderState.line2Font = blockEntity.getLine2Font();

        BlockState blockState = blockEntity.getBlockState();

        renderState.facing = blockState.getValue(EB10Block.FACING);
        renderState.cartoucheType = blockEntity.getCartoucheType();
        renderState.cartoucheText = blockEntity.getCartoucheText();

        BlockState cartoucheModelState =
                MoreRoadBlocks.CARTOUCHE_MODEL.get()
                        .defaultBlockState()
                        .setValue(
                                CartoucheModelBlock.FACING,
                                renderState.facing
                        )
                        .setValue(
                                CartoucheModelBlock.TYPE,
                                renderState.cartoucheType
                        );

        this.blockResolver.update(
                renderState.cartoucheModel,
                cartoucheModelState,
                BLOCK_DISPLAY_CONTEXT
        );

        BlockState cartoucheSupportModelState =
                MoreRoadBlocks.CARTOUCHE_SUPPORT_MODEL.get()
                        .defaultBlockState()
                        .setValue(
                                HorizontalDirectionalBlock.FACING,
                                renderState.facing
                        );

        this.blockResolver.update(
                renderState.cartoucheSupportModel,
                cartoucheSupportModelState,
                BLOCK_DISPLAY_CONTEXT
        );

        if (
                renderState.cartoucheType != null
                        && renderState.cartoucheType.isVisible()
        ) {
            double cartoucheBottomY =
                    CartoucheLayout.getEBBottomY();

            CartoucheLayout.PoleAnchor anchor =
                    CartoucheLayout.findNearestPoleAnchor(
                            blockEntity.getLevel(),
                            blockEntity.getBlockPos(),
                            renderState.facing,
                            cartoucheBottomY
                    );

            renderState.cartoucheSupportOffsetX = anchor.offsetX();
            renderState.cartoucheSupportOffsetZ = anchor.offsetZ();
            renderState.cartoucheSupportPoleTopY = anchor.poleTopY();
        } else {
            renderState.cartoucheSupportOffsetX = 0.0D;
            renderState.cartoucheSupportOffsetZ = 0.0D;
            renderState.cartoucheSupportPoleTopY = 1.0D;
        }
    }

    @Override
    public void submit(
            EB10RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        submitCartouche(
                renderState,
                poseStack,
                collector
        );

        String line1 = cleanText(renderState.line1);
        String line2 = cleanText(renderState.line2);

        if (line1.isBlank() && line2.isBlank()) {
            return;
        }

        if (line2.isBlank()) {
            submitLine(
                    line1,
                    renderState.line1Font,
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
                    renderState.line1Font,
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
                    renderState.line2Font,
                    SECOND_LINE_Y,
                    TWO_LINES_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private static void submitCartouche(
            EB10RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (
                renderState.cartoucheType == null
                        || !renderState.cartoucheType.isVisible()
        ) {
            return;
        }

        float highestPanelTopY =
                (float) CartoucheLayout.getEBTopY();

        float yOffset =
                (float) CartoucheLayout.getEBBottomY();

        submitCartoucheSupport(
                renderState,
                highestPanelTopY,
                yOffset,
                poseStack,
                collector
        );

        poseStack.pushPose();
        poseStack.translate(
                0.5F,
                yOffset,
                0.5F
        );
        poseStack.scale(
                CartoucheLayout.MODEL_SCALE,
                CartoucheLayout.MODEL_SCALE,
                CartoucheLayout.MODEL_SCALE
        );
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        renderState.cartoucheModel.submit(
                poseStack,
                collector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();

        CartoucheTextRenderer.submit(
                renderState.cartoucheText,
                renderState.cartoucheType,
                yOffset,
                CartoucheLayout.MODEL_SCALE,
                renderState.facing,
                renderState.lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitCartoucheSupport(
            EB10RenderState renderState,
            float highestPanelTopY,
            float cartoucheBottomY,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        CartoucheLayout.PoleAnchor anchor =
                new CartoucheLayout.PoleAnchor(
                        renderState.cartoucheSupportOffsetX,
                        renderState.cartoucheSupportOffsetZ,
                        renderState.cartoucheSupportPoleTopY
                );

        float supportBottomY =
                (float) CartoucheLayout.getSupportBottomY(anchor);

        float supportTopY =
                (float) CartoucheLayout.getSupportTopY(cartoucheBottomY);

        float supportHeight = supportTopY - supportBottomY;

        if (supportHeight <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                (float) anchor.offsetX(),
                supportBottomY,
                (float) anchor.offsetZ()
        );
        poseStack.scale(1.0F, supportHeight, 1.0F);

        renderState.cartoucheSupportModel.submit(
                poseStack,
                collector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }

    private static void submitLine(
            String value,
            RoadTextFont textFont,
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
                                Style.EMPTY.withFont(getRoadFont(textFont))
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

    private static FontDescription.Resource getRoadFont(
            RoadTextFont textFont
    ) {
        return textFont == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;
    }

    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .strip();
    }
}
