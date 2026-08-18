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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61ABlock;
import net.xelpy.moreroad.block.custom.D61APanelLayout;
import net.xelpy.moreroad.block.custom.D61APanelModelBlock;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;

import java.util.Locale;

public class D61ABlockEntityRenderer
        implements BlockEntityRenderer<D61ABlockEntity, D61ARenderState> {

    private static final FontDescription.Resource ROAD_FONT =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    private static final float TEXT_Z = 0.128F;

    private static final float SIMPLE_LINE_Y = 0.765F;
    private static final float DOUBLE_LINE_Y_TOP = 0.720F;
    private static final float DOUBLE_LINE_Y_BOTTOM = 0.540F;
    private static final float DOUBLE_LINE_Y_SINGLE = 0.630F;

    /*
     * Le D61A simple est plus large que l'ancien modèle : on profite de
     * toute la zone utile avec une petite marge au bord.
     */
    private static final float SIMPLE_DESTINATION_LEFT_EDGE = -0.43F;
    private static final float SIMPLE_DISTANCE_RIGHT_EDGE = 1.43F;

    /*
     * Le D61A double conserve exactement ses réglages actuels.
     */
    private static final float DOUBLE_DESTINATION_LEFT_EDGE = -0.42F;
    private static final float DOUBLE_DISTANCE_RIGHT_EDGE = 1.42F;

    private static final float SINGLE_DESTINATION_BASE_SCALE = 0.0165F;
    private static final float SINGLE_DISTANCE_BASE_SCALE = 0.0160F;
    private static final float DOUBLE_DESTINATION_BASE_SCALE = 0.0153F;
    private static final float DOUBLE_DISTANCE_BASE_SCALE = 0.0150F;

    private static final float DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.42F;
    private static final float DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.70F;
    private static final float DISTANCE_MAX_WIDTH = 0.26F;

    private final BlockModelResolver blockResolver;

    public D61ABlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockResolver = context.blockModelResolver();
    }

    @Override
    public D61ARenderState createRenderState() {
        return new D61ARenderState();
    }

    @Override
    public void extractRenderState(
            D61ABlockEntity blockEntity,
            D61ARenderState renderState,
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

        BlockState blockState = blockEntity.getBlockState();
        renderState.facing = blockState.getValue(D61ABlock.FACING);

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel = blockEntity.getPanel(i);

            renderState.enabled[i] = panel.enabled();
            renderState.line1[i] = panel.line1();
            renderState.line2[i] = panel.line2();
            renderState.distance1[i] = panel.distance1();
            renderState.distance2[i] = panel.distance2();
            renderState.panelTypes[i] = panel.type() == D21AType.GREEN
                    ? D21AType.GREEN
                    : D21AType.WHITE;
            renderState.doubleLines[i] = panel.doubleLine();

            if (!panel.enabled()) {
                continue;
            }

            BlockState panelModelState =
                    (panel.doubleLine()
                            ? MoreRoadBlocks.D61A2_PANEL_MODEL.get()
                            : MoreRoadBlocks.D61A_PANEL_MODEL.get())
                            .defaultBlockState()
                            .setValue(D61APanelModelBlock.FACING, renderState.facing)
                            .setValue(D61APanelModelBlock.TYPE, renderState.panelTypes[i]);

            this.blockResolver.update(
                    renderState.panelModels[i],
                    panelModelState,
                    BLOCK_DISPLAY_CONTEXT
            );
        }
    }

    @Override
    public void submit(
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        int enabledCount = 0;

        for (boolean enabled : renderState.enabled) {
            if (enabled) {
                enabledCount++;
            }
        }

        if (enabledCount <= 0) {
            return;
        }

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            if (!renderState.enabled[i]) {
                continue;
            }

            float yOffset =
                    (float) D61APanelLayout.getPanelYOffset(
                            renderState.enabled,
                            renderState.doubleLines,
                            i
                    );

            poseStack.pushPose();
            poseStack.translate(0.0F, yOffset, 0.0F);

            renderState.panelModels[i].submit(
                    poseStack,
                    collector,
                    renderState.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            poseStack.popPose();

            submitPanelText(
                    cleanText(renderState.line1[i]),
                    cleanText(renderState.line2[i]),
                    cleanText(renderState.distance1[i]),
                    cleanText(renderState.distance2[i]),
                    renderState.panelTypes[i],
                    renderState.doubleLines[i],
                    yOffset,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private static void submitPanelText(
            String line1,
            String line2,
            String distance1,
            String distance2,
            D21AType panelType,
            boolean doubleLine,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (
                line1.isBlank()
                        && line2.isBlank()
                        && distance1.isBlank()
                        && distance2.isBlank()
        ) {
            return;
        }

        int textColor = panelType == D21AType.GREEN
                ? 0xFFFFFFFF
                : 0xFF000000;

        if (doubleLine) {
            submitTwoLinePanelText(
                    line1,
                    line2,
                    distance1,
                    distance2,
                    textColor,
                    yOffset,
                    renderState,
                    poseStack,
                    collector
            );
            return;
        }

        String distance = !distance1.isBlank() ? distance1 : distance2;

        submitSingleLinePanelText(
                line1,
                distance,
                textColor,
                yOffset,
                renderState,
                poseStack,
                collector
        );
    }

    private static void submitSingleLinePanelText(
            String destination,
            String distance,
            int textColor,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float destinationMaxWidth = distance.isBlank()
                ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                : DESTINATION_MAX_WIDTH_WITH_DISTANCE;

        float textY = SIMPLE_LINE_Y + yOffset;

        if (!destination.isBlank()) {
            submitAnchoredText(
                    destination,
                    SIMPLE_DESTINATION_LEFT_EDGE,
                    textY,
                    SINGLE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    TextAnchor.LEFT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!distance.isBlank()) {
            submitAnchoredText(
                    distance,
                    SIMPLE_DISTANCE_RIGHT_EDGE,
                    textY,
                    SINGLE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.RIGHT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private static void submitTwoLinePanelText(
            String line1,
            String line2,
            String distance1,
            String distance2,
            int textColor,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        boolean hasTop = !line1.isBlank();
        boolean hasBottom = !line2.isBlank();

        float line1Y = (hasTop && hasBottom ? DOUBLE_LINE_Y_TOP : DOUBLE_LINE_Y_SINGLE) + yOffset;
        float line2Y = (hasTop && hasBottom ? DOUBLE_LINE_Y_BOTTOM : DOUBLE_LINE_Y_SINGLE) + yOffset;

        if (hasTop) {
            submitAnchoredText(
                    line1,
                    DOUBLE_DESTINATION_LEFT_EDGE,
                    line1Y,
                    DOUBLE_DESTINATION_BASE_SCALE,
                    distance1.isBlank() ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE : DESTINATION_MAX_WIDTH_WITH_DISTANCE,
                    TextAnchor.LEFT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!distance1.isBlank()) {
            submitAnchoredText(
                    distance1,
                    DOUBLE_DISTANCE_RIGHT_EDGE,
                    line1Y,
                    DOUBLE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.RIGHT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (hasBottom) {
            submitAnchoredText(
                    line2,
                    DOUBLE_DESTINATION_LEFT_EDGE,
                    line2Y,
                    DOUBLE_DESTINATION_BASE_SCALE,
                    distance2.isBlank() ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE : DESTINATION_MAX_WIDTH_WITH_DISTANCE,
                    TextAnchor.LEFT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!distance2.isBlank()) {
            submitAnchoredText(
                    distance2,
                    DOUBLE_DISTANCE_RIGHT_EDGE,
                    line2Y,
                    DOUBLE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.RIGHT,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private enum TextAnchor {
        LEFT,
        CENTER,
        RIGHT
    }

    private static void submitAnchoredText(
            String value,
            float anchorX,
            float worldY,
            float baseScale,
            float maxWorldWidth,
            TextAnchor anchor,
            int color,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        Component component =
                Component.literal(value)
                        .withStyle(Style.EMPTY.withFont(ROAD_FONT));

        FormattedCharSequence text = component.getVisualOrderText();
        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWorldWidth / textWidth);

        poseStack.pushPose();

        poseStack.translate(0.5F, worldY, 0.5F);

        float rotation = switch (renderState.facing) {
            case SOUTH -> 0F;
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };

        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(anchorX - 0.5F, 0F, TEXT_Z);
        poseStack.scale(scale, -scale, scale);

        float textX = switch (anchor) {
            case LEFT -> 0F;
            case CENTER -> -textWidth / 2.0F;
            case RIGHT -> -textWidth;
        };

        float textY = -font.lineHeight / 2.0F;

        collector.submitText(
                poseStack,
                textX,
                textY,
                text,
                false,
                Font.DisplayMode.NORMAL,
                renderState.lightCoords,
                color,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
    }

    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.strip().toUpperCase(Locale.ROOT);
    }
}
