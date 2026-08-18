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
import net.xelpy.moreroad.block.custom.D21A2Block;
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21APanelLayout;
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

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /* ============================================================
     * RÉGLAGES TEXTE - COMMUNS
     * ============================================================ */

    private static final float TEXT_Z = 0.128F;

    private static final float LEFT_DISTANCE_X = -0.28F;
    private static final float RIGHT_DISTANCE_X = 1.22F;

    private static final float LEFT_DESTINATION_RIGHT_EDGE = 1.34F;
    private static final float RIGHT_DESTINATION_LEFT_EDGE = -0.26F;

    private static final float LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 0.96F;
    private static final float RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.12F;

    private static final float DISTANCE_MAX_WIDTH = 0.28F;

    /* ============================================================
     * D21A - 1 LIGNE
     * ============================================================ */

    private static final float SINGLE_TEXT_Y = 0.675F;

    private static final float SINGLE_DESTINATION_BASE_SCALE = 0.0170F;
    private static final float SINGLE_DISTANCE_BASE_SCALE = 0.0160F;

    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.06F;
    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.34F;

    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO = 0.82F;
    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO = 1.00F;

    /* ============================================================
     * D21A2 - 2 LIGNES
     * ============================================================ */

    /*
     * Le modèle D21A2 est désormais physiquement centré dans ses JSON.
     * Il ne faut donc PLUS appliquer ici l'ancien décalage X de -1/16.
     * Toutes les coordonnées ci-dessous sont directement exprimées dans
     * le repère réel du panneau D21A2 centré autour du poteau.
     */

    private static final float TWO_LINE_TEXT_Y_TOP = 0.720F;
    private static final float TWO_LINE_TEXT_Y_BOTTOM = 0.540F;
    private static final float TWO_LINE_TEXT_Y_SINGLE = 0.630F;
    private static final float TWO_LINE_DISTANCE_Y_CENTER = 0.630F;

    private static final float TWO_LINE_DESTINATION_BASE_SCALE = 0.0158F;
    private static final float TWO_LINE_DISTANCE_BASE_SCALE = 0.0155F;

    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.08F;
    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.34F;

    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO = 0.78F;
    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO = 0.88F;

    /*
     * Les distances gardent EXACTEMENT la position horizontale effective
     * de la V8. L'ancien offset -1/16 est simplement intégré directement
     * dans ces deux constantes pour que rien ne bouge de ce côté-là.
     */
    private static final float TWO_LINE_LEFT_DISTANCE_X = -0.1625F;
    private static final float TWO_LINE_RIGHT_DISTANCE_X = 1.055F;

    /*
     * Le panneau blanc à flèche droite donne visuellement un peu plus
     * d'espace avant la pointe que les panneaux colorés. On avance donc
     * uniquement son kilométrage d'environ un pixel Minecraft, sans
     * modifier les panneaux verts/bleus.
     */
    private static final float TWO_LINE_RIGHT_DISTANCE_X_WHITE = 1.115F;

    /*
     * Marges D21A2 définitives.
     *
     * Le panneau centré occupe environ X = -0.4975 à X = 1.4975.
     * Sans logo, le texte reste à ~1 pixel Minecraft du bord utile.
     * Avec logo, il vient juste contre la zone du pictogramme avec une
     * petite respiration, au lieu de rester artificiellement au centre.
     *
     * Les valeurs sont symétriques autour de X = 0.5 :
     *  - flèche à gauche  -> destination alignée à droite ;
     *  - flèche à droite  -> destination alignée à gauche.
     */
    private static final float TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE = 1.43F;
    private static final float TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE = -0.43F;

    private static final float TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 1.00F;
    private static final float TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.00F;

    private final BlockModelResolver blockResolver;

    public D21ABlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockResolver = context.blockModelResolver();
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

        BlockState blockState = blockEntity.getBlockState();

        renderState.facing = blockState.getValue(D21ABlock.FACING);

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel = blockEntity.getPanel(i);

            renderState.enabled[i] = panel.enabled();
            renderState.line1[i] = panel.line1();
            renderState.line2[i] = panel.line2();
            renderState.distance1[i] = panel.distance1();
            renderState.distance2[i] = panel.distance2();
            renderState.panelTypes[i] = panel.type();
            renderState.arrowRights[i] = panel.arrowRight();
            renderState.autorouteLogos[i] = panel.autorouteLogo();
            renderState.doubleLines[i] = panel.doubleLine();

            if (!panel.enabled()) {
                continue;
            }

            BlockState panelModelState =
                    (panel.doubleLine()
                            ? MoreRoadBlocks.D21A2_PANEL_MODEL.get()
                            : MoreRoadBlocks.D21A_PANEL_MODEL.get())
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

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            if (!renderState.enabled[i]) {
                continue;
            }

            float yOffset =
                    (float) D21APanelLayout.getPanelYOffset(
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
                    renderState.arrowRights[i],
                    renderState.autorouteLogos[i],
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
            boolean arrowRight,
            boolean autorouteLogo,
            boolean twoLineMode,
            float yOffset,
            D21ARenderState renderState,
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

        int textColor =
                panelType == D21AType.WHITE
                        ? 0xFF000000
                        : 0xFFFFFFFF;

        boolean showAutorouteLogo =
                autorouteLogo
                        && panelType != D21AType.WHITE;

        if (twoLineMode) {
            submitTwoLinePanelText(
                    line1,
                    line2,
                    distance1,
                    distance2,
                    textColor,
                    panelType,
                    arrowRight,
                    showAutorouteLogo,
                    yOffset,
                    renderState,
                    poseStack,
                    collector
            );
            return;
        }

        String singleDistance =
                !distance1.isBlank()
                        ? distance1
                        : distance2;

        submitSingleLinePanelText(
                line1,
                singleDistance,
                textColor,
                arrowRight,
                showAutorouteLogo,
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
            boolean arrowRight,
            boolean showAutorouteLogo,
            float yOffset,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float destinationMaxWidth;

        if (showAutorouteLogo) {
            destinationMaxWidth =
                    distance.isBlank()
                            ? SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO
                            : SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO;
        } else {
            destinationMaxWidth =
                    distance.isBlank()
                            ? SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                            : SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE;
        }

        float textY = SINGLE_TEXT_Y + yOffset;

        if (arrowRight) {
            if (!destination.isBlank()) {
                submitAnchoredText(
                        destination,
                        showAutorouteLogo
                                ? RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO
                                : RIGHT_DESTINATION_LEFT_EDGE,
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
                        RIGHT_DISTANCE_X,
                        textY,
                        SINGLE_DISTANCE_BASE_SCALE,
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
                    SINGLE_DISTANCE_BASE_SCALE,
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
                    SINGLE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
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
            D21AType panelType,
            boolean arrowRight,
            boolean showAutorouteLogo,
            float yOffset,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        boolean hasDistance1 = !distance1.isBlank();
        boolean hasDistance2 = !distance2.isBlank();
        boolean hasAnyDistance = hasDistance1 || hasDistance2;

        float destinationMaxWidth;

        if (showAutorouteLogo) {
            if (hasAnyDistance && arrowRight) {
                /*
                 * Flèche à droite : le kilométrage est lui aussi à droite.
                 * On garde sa position actuelle et on réserve sa zone pour
                 * empêcher tout chevauchement avec une destination longue.
                 */
                destinationMaxWidth = 0.66F;
            } else {
                destinationMaxWidth =
                        hasAnyDistance
                                ? TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO
                                : TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO;
            }
        } else {
            if (hasAnyDistance && arrowRight) {
                destinationMaxWidth = 1.00F;
            } else {
                destinationMaxWidth =
                        hasAnyDistance
                                ? TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE
                                : TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE;
            }
        }

        boolean hasTop = !line1.isBlank();
        boolean hasBottom = !line2.isBlank();

        float line1Y =
                (hasTop && hasBottom
                        ? TWO_LINE_TEXT_Y_TOP
                        : TWO_LINE_TEXT_Y_SINGLE)
                        + yOffset;

        float line2Y =
                TWO_LINE_TEXT_Y_BOTTOM
                        + yOffset;

        float destinationAnchor =
                arrowRight
                        ? (showAutorouteLogo
                        ? TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO
                        : TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE)
                        : (showAutorouteLogo
                        ? TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO
                        : TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE);

        TextAnchor destinationTextAnchor =
                arrowRight
                        ? TextAnchor.LEFT
                        : TextAnchor.RIGHT;

        float distanceAnchor;

        if (arrowRight) {
            distanceAnchor =
                    panelType == D21AType.WHITE
                            ? TWO_LINE_RIGHT_DISTANCE_X_WHITE
                            : TWO_LINE_RIGHT_DISTANCE_X;
        } else {
            distanceAnchor = TWO_LINE_LEFT_DISTANCE_X;
        }

        if (hasTop) {
            submitAnchoredText(
                    line1,
                    destinationAnchor,
                    line1Y,
                    TWO_LINE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    destinationTextAnchor,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (hasBottom) {
            submitAnchoredText(
                    line2,
                    destinationAnchor,
                    line2Y,
                    TWO_LINE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    destinationTextAnchor,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (hasDistance1 && hasDistance2) {
            submitAnchoredText(
                    distance1,
                    distanceAnchor,
                    TWO_LINE_TEXT_Y_TOP + yOffset,
                    TWO_LINE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.CENTER,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );

            submitAnchoredText(
                    distance2,
                    distanceAnchor,
                    TWO_LINE_TEXT_Y_BOTTOM + yOffset,
                    TWO_LINE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.CENTER,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        } else if (hasAnyDistance) {
            String sharedDistance =
                    hasDistance1
                            ? distance1
                            : distance2;

            submitAnchoredText(
                    sharedDistance,
                    distanceAnchor,
                    TWO_LINE_DISTANCE_Y_CENTER + yOffset,
                    TWO_LINE_DISTANCE_BASE_SCALE,
                    DISTANCE_MAX_WIDTH,
                    TextAnchor.CENTER,
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
                Axis.YP.rotationDegrees(rotation)
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
                .toUpperCase(Locale.ROOT);
    }
}
