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
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21APanelModelBlock;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;

import java.util.Locale;

public class D21ABlockEntityRenderer
        implements BlockEntityRenderer<D21ABlockEntity, D21ARenderState> {

    private static final FontDescription.Resource ROAD_FONT =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    /*
     * Contexte utilisé pour résoudre les modèles de plaques depuis un BER.
     */
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /* ============================================================
     * RÉGLAGES TEXTE CALIBRÉS
     * ============================================================ */

    private static final float TEXT_Y = 0.675F;
    private static final float TEXT_Z = 0.128F;

    private static final float LEFT_DISTANCE_X = -0.28F;
    private static final float RIGHT_DISTANCE_X = 1.22F;

    private static final float LEFT_DESTINATION_RIGHT_EDGE = 1.34F;
    private static final float RIGHT_DESTINATION_LEFT_EDGE = -0.26F;

    /*
     * Quand le pictogramme autoroute est présent, il occupe le bord opposé
     * à la pointe de flèche. La destination est donc légèrement repoussée
     * vers le centre pour ne jamais passer sous le logo.
     */
    private static final float LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 0.96F;
    private static final float RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.12F;

    private static final float DESTINATION_BASE_SCALE = 0.0170F;
    private static final float DISTANCE_BASE_SCALE = 0.0160F;

    private static final float DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.06F;
    private static final float DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.34F;

    private static final float DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO = 0.82F;
    private static final float DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO = 1.00F;

    private static final float DISTANCE_MAX_WIDTH = 0.28F;

    private final BlockModelResolver blockResolver;

    public D21ABlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockResolver =
                context.blockModelResolver();
    }

    @Override
    public D21ARenderState createRenderState() {
        return new D21ARenderState();
    }

    @Override
    public void extractRenderState(
            D21ABlockEntity blockEntity,
            D21ARenderState renderState,
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

        BlockState blockState =
                blockEntity.getBlockState();

        renderState.facing =
                blockState.getValue(
                        D21ABlock.FACING
                );

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel =
                    blockEntity.getPanel(i);

            renderState.enabled[i] = panel.enabled();
            renderState.destinations[i] = panel.destination();
            renderState.distances[i] = panel.distance();
            renderState.panelTypes[i] = panel.type();
            renderState.arrowRights[i] = panel.arrowRight();
            renderState.autorouteLogos[i] = panel.autorouteLogo();

            if (!panel.enabled()) {
                continue;
            }

            /*
             * Le bloc D21A_PANEL_MODEL n'existe jamais réellement dans le
             * monde. Il sert seulement à demander au BlockModelResolver le
             * bon modèle JSON pour ce panneau précis.
             */
            BlockState panelModelState =
                    MoreRoadBlocks.D21A_PANEL_MODEL
                            .get()
                            .defaultBlockState()
                            .setValue(
                                    D21APanelModelBlock.FACING,
                                    renderState.facing
                            )
                            .setValue(
                                    D21APanelModelBlock.TYPE,
                                    panel.type()
                            )
                            .setValue(
                                    D21APanelModelBlock.ARROW_RIGHT,
                                    panel.arrowRight()
                            )
                            .setValue(
                                    D21APanelModelBlock.AUTOROUTE_LOGO,
                                    panel.autorouteLogo()
                            );

            this.blockResolver.update(
                    renderState.panelModels[i],
                    panelModelState,
                    BLOCK_DISPLAY_CONTEXT
            );
        }
    }

    @Override
    public void submit(
            D21ARenderState renderState,
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

        int activeIndex = 0;

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            if (!renderState.enabled[i]) {
                continue;
            }

            float yOffset =
                    (float) D21ABlock.getPanelYOffset(
                            enabledCount,
                            activeIndex
                    );

            /* ====================================================
             * MODÈLE DE LA PLAQUE
             * ==================================================== */

            poseStack.pushPose();
            poseStack.translate(
                    0.0F,
                    yOffset,
                    0.0F
            );

            renderState.panelModels[i].submit(
                    poseStack,
                    collector,
                    renderState.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            poseStack.popPose();

            /* ====================================================
             * TEXTE DE LA PLAQUE
             * ==================================================== */

            submitPanelText(
                    cleanText(renderState.destinations[i]),
                    cleanText(renderState.distances[i]),
                    renderState.panelTypes[i],
                    renderState.arrowRights[i],
                    renderState.autorouteLogos[i],
                    yOffset,
                    renderState,
                    poseStack,
                    collector
            );

            activeIndex++;
        }
    }

    private static void submitPanelText(
            String destination,
            String distance,
            D21AType panelType,
            boolean arrowRight,
            boolean autorouteLogo,
            float yOffset,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (
                destination.isBlank()
                        && distance.isBlank()
        ) {
            return;
        }

        int textColor =
                panelType == D21AType.WHITE
                        ? 0xFF000000
                        : 0xFFFFFFFF;

        boolean showAutorouteLogo =
                autorouteLogo
                        && panelType != D21AType.WHITE;

        float destinationMaxWidth;

        if (showAutorouteLogo) {
            destinationMaxWidth =
                    distance.isBlank()
                            ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO
                            : DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO;
        } else {
            destinationMaxWidth =
                    distance.isBlank()
                            ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                            : DESTINATION_MAX_WIDTH_WITH_DISTANCE;
        }

        float textY =
                TEXT_Y + yOffset;

        if (arrowRight) {
            if (!destination.isBlank()) {
                submitAnchoredText(
                        destination,
                        showAutorouteLogo
                                ? RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO
                                : RIGHT_DESTINATION_LEFT_EDGE,
                        textY,
                        DESTINATION_BASE_SCALE,
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
                        RIGHT_DISTANCE_X,
                        textY,
                        DISTANCE_BASE_SCALE,
                        DISTANCE_MAX_WIDTH,
                        TextAnchor.CENTER,
                        textColor,
                        renderState,
                        poseStack,
                        collector
                );
            }

            return;
        }

        if (!distance.isBlank()) {
            submitAnchoredText(
                    distance,
                    LEFT_DISTANCE_X,
                    textY,
                    DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.CENTER,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!destination.isBlank()) {
            submitAnchoredText(
                    destination,
                    showAutorouteLogo
                            ? LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO
                            : LEFT_DESTINATION_RIGHT_EDGE,
                    textY,
                    DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
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
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return;
        }

        Font font =
                Minecraft
                        .getInstance()
                        .font;

        Component component =
                Component
                        .literal(value)
                        .withStyle(
                                Style.EMPTY
                                        .withFont(
                                                ROAD_FONT
                                        )
                        );

        FormattedCharSequence text =
                component.getVisualOrderText();

        int textWidth =
                font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale =
                Math.min(
                        baseScale,
                        maxWorldWidth / textWidth
                );

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                worldY,
                0.5F
        );

        float rotation =
                switch (renderState.facing) {
                    case SOUTH -> 0F;
                    case WEST -> -90F;
                    case NORTH -> 180F;
                    case EAST -> 90F;
                    default -> 0F;
                };

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotation
                )
        );

        poseStack.translate(
                anchorX - 0.5F,
                0F,
                TEXT_Z
        );

        poseStack.scale(
                scale,
                -scale,
                scale
        );

        float textX =
                switch (anchor) {
                    case LEFT -> 0F;
                    case CENTER -> -textWidth / 2.0F;
                    case RIGHT -> -textWidth;
                };

        float textY =
                -font.lineHeight / 2.0F;

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

        return text
                .strip()
                .toUpperCase(
                        Locale.ROOT
                );
    }
}
