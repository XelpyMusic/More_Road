package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowModelBlock;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61ABlock;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.block.custom.D61APanelLayout;
import net.xelpy.moreroad.block.custom.D61APanelModelBlock;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;


public class D61ABlockEntityRenderer
        implements BlockEntityRenderer<D61ABlockEntity, D61ARenderState> {

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

    private static final Identifier AUTOROUTE_LOGO_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    MoreRoad.MODID,
                    "textures/block/autoroute_logo.png"
            );

    /* V99 : stabilisation du texte devant la face + POLYGON_OFFSET. */
    private static final float TEXT_Z = 0.1935F;
    private static final float ARROW_Z = 0.1925F;
    private static final float AUTOROUTE_LOGO_Z = 0.1915F;

    private static final float SIMPLE_LINE_Y = 0.765F;
    private static final float DOUBLE_LINE_Y_TOP = 0.720F;
    private static final float DOUBLE_LINE_Y_BOTTOM = 0.540F;
    private static final float DOUBLE_LINE_Y_SINGLE = 0.630F;

    private static final float SIMPLE_DESTINATION_LEFT_EDGE = -0.43F;
    private static final float SIMPLE_DISTANCE_RIGHT_EDGE = 1.43F;

    private static final float DOUBLE_DESTINATION_LEFT_EDGE = -0.42F;
    private static final float DOUBLE_DISTANCE_RIGHT_EDGE = 1.42F;

    /*
     * Avec une flèche à gauche, le texte commence juste après le PNG.
     * Avec une flèche à droite, le texte conserve sa marge gauche normale
     * mais sa largeur maximale s'arrête avant la flèche.
     */
    private static final float SIMPLE_DESTINATION_LEFT_EDGE_WITH_LEFT_ARROW = 0.05F;
    private static final float DOUBLE_DESTINATION_LEFT_EDGE_WITH_LEFT_ARROW = 0.08F;

    /*
     * Le logo autoroute ne doit jamais recentrer artificiellement la destination.
     * Le texte conserve donc exactement la même ancre gauche que sans logo ; seule
     * sa largeur maximale est réduite afin de réserver la zone du pictogramme.
     */
    private static final float SIMPLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = SIMPLE_DESTINATION_LEFT_EDGE;
    private static final float DOUBLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = DOUBLE_DESTINATION_LEFT_EDGE;
    private static final float SIMPLE_DESTINATION_LEFT_EDGE_WITH_LOGO_AND_LEFT_ARROW = -0.08F;
    private static final float DOUBLE_DESTINATION_LEFT_EDGE_WITH_LOGO_AND_LEFT_ARROW = -0.06F;

    private static final float SINGLE_DESTINATION_BASE_SCALE = 0.0165F;
    private static final float SINGLE_DISTANCE_BASE_SCALE = 0.0160F;
    private static final float DOUBLE_DESTINATION_BASE_SCALE = 0.0153F;
    private static final float DOUBLE_DISTANCE_BASE_SCALE = 0.0150F;

    private static final float DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.42F;
    private static final float DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.70F;

    /*
     * Flèche OU kilométrage : lorsqu'une flèche est active, aucun kilométrage
     * n'est rendu. La destination dispose donc de toute la place restante
     * jusqu'à la zone occupée par le PNG.
     */
    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_LEFT_ARROW = 1.34F;
    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_RIGHT_ARROW = 1.46F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_LEFT_ARROW = 1.32F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_RIGHT_ARROW = 1.44F;

    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO = 1.28F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO = 1.18F;
    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_RIGHT_ARROW = 1.00F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_RIGHT_ARROW = 0.94F;
    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_LEFT_ARROW = 1.06F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_LEFT_ARROW = 0.96F;

    private static final float DISTANCE_MAX_WIDTH = 0.26F;

    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_AND_DISTANCE = 0.96F;
    private static final float SIMPLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_NO_DISTANCE = 1.28F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_AND_DISTANCE = 0.90F;
    private static final float DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_NO_DISTANCE = 1.18F;


    /*
     * Les nouveaux modèles internes sont de simples plans qui utilisent
     * directement fleche_blanche.png / fleche_noir.png. Le PNG est vertical
     * et son ratio 3:4 est respecté par le plan 12 x 16 pixels.
     */
    private static final float SIMPLE_ARROW_LEFT_X = -0.32F;
    private static final float SIMPLE_ARROW_RIGHT_X = 1.32F;
    private static final float DOUBLE_ARROW_LEFT_X = -0.30F;
    private static final float DOUBLE_ARROW_RIGHT_X = 1.30F;

    private static final float SIMPLE_ARROW_Y = 0.794F;
    private static final float DOUBLE_ARROW_Y = 0.669F;

    private static final float SIMPLE_ARROW_SCALE = 0.24F;
    private static final float DOUBLE_ARROW_SCALE = 0.34F;


    // Dimensions exactes des anciens overlays Blockbench du logo autoroute.
    private static final float SIMPLE_AUTOROUTE_LOGO_SIZE = 3.65F / 16.0F;
    private static final float DOUBLE_AUTOROUTE_LOGO_SIZE = 5.00F / 16.0F;
    private static final float SIMPLE_AUTOROUTE_LOGO_Y = 12.685F / 16.0F;
    private static final float DOUBLE_AUTOROUTE_LOGO_Y = 10.58F / 16.0F;

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

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            D61APanelData panel = blockEntity.getPanel(i);

            renderState.enabled[i] = panel.enabled();
            renderState.line1[i] = panel.line1();
            renderState.line2[i] = panel.line2();
            renderState.distance1[i] = panel.distance1();
            renderState.distance2[i] = panel.distance2();
            renderState.panelTypes[i] = sanitizeType(panel.type());
            renderState.line1Fonts[i] = panel.line1Font();
            renderState.line2Fonts[i] = panel.line2Font();
            renderState.doubleLines[i] = panel.doubleLine();
            renderState.autorouteLogos[i] =
                    renderState.panelTypes[i] != D21AType.WHITE
                            && panel.autorouteLogo();
            renderState.arrowEnabled[i] = panel.arrowEnabled();
            renderState.arrowPositions[i] = panel.arrowPosition();
            renderState.arrowDirections[i] = panel.arrowDirection();

            if (!panel.enabled()) {
                continue;
            }

            BlockState panelModelState =
                    (panel.doubleLine()
                            ? MoreRoadBlocks.D61A2_PANEL_MODEL.get()
                            : MoreRoadBlocks.D61A_PANEL_MODEL.get())
                            .defaultBlockState()
                            .setValue(D61APanelModelBlock.FACING, renderState.facing)
                            .setValue(D61APanelModelBlock.TYPE, renderState.panelTypes[i])
                            .setValue(
                                    D61APanelModelBlock.AUTOROUTE_LOGO,
                                    false
                            );

            this.blockResolver.update(
                    renderState.panelModels[i],
                    panelModelState,
                    BLOCK_DISPLAY_CONTEXT
            );

            if (panel.arrowEnabled()) {
                BlockState arrowModelState =
                        MoreRoadBlocks.D61A_ARROW_MODEL.get()
                                .defaultBlockState()
                                .setValue(
                                        D61AArrowModelBlock.BLACK,
                                        renderState.panelTypes[i] == D21AType.WHITE
                                );

                this.blockResolver.update(
                        renderState.arrowModels[i],
                        arrowModelState,
                        BLOCK_DISPLAY_CONTEXT
                );
            }
        }

        if (
                renderState.cartoucheType != null
                        && renderState.cartoucheType.isVisible()
        ) {
            double cartoucheBottomY =
                    CartoucheLayout.getD61BottomY(
                            renderState.enabled,
                            renderState.doubleLines
                    );

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

        submitCartouche(
                renderState,
                poseStack,
                collector
        );

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

            if (renderState.autorouteLogos[i]) {
                submitAutorouteLogo(
                        renderState.doubleLines[i],
                        renderState.arrowEnabled[i],
                        renderState.arrowPositions[i],
                        yOffset,
                        renderState,
                        poseStack,
                        collector
                );
            }

            if (renderState.arrowEnabled[i]) {
                submitArrow(
                        renderState.arrowModels[i],
                        renderState.arrowPositions[i],
                        renderState.arrowDirections[i],
                        renderState.doubleLines[i],
                        yOffset,
                        renderState,
                        poseStack,
                        collector
                );
            }

            submitPanelText(
                    cleanText(renderState.line1[i]),
                    cleanText(renderState.line2[i]),
                    cleanText(renderState.distance1[i]),
                    cleanText(renderState.distance2[i]),
                    renderState.line1Fonts[i],
                    renderState.line2Fonts[i],
                    renderState.panelTypes[i],
                    renderState.autorouteLogos[i],
                    renderState.doubleLines[i],
                    renderState.arrowEnabled[i],
                    renderState.arrowPositions[i],
                    yOffset,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }

    private static void submitCartouche(
            D61ARenderState renderState,
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
                (float) CartoucheLayout.getD61HighestTopY(
                        renderState.enabled,
                        renderState.doubleLines
                );

        float yOffset =
                (float) CartoucheLayout.getD61BottomY(
                        renderState.enabled,
                        renderState.doubleLines
                );

        submitCartoucheSupport(
                renderState,
                highestPanelTopY,
                yOffset,
                poseStack,
                collector
        );

        poseStack.pushPose();
        poseStack.translate(0.5F, yOffset, 0.5F);
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
            D61ARenderState renderState,
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

    private static void submitAutorouteLogo(
            boolean doubleLine,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        D61AArrowPosition effectiveArrowPosition = arrowPosition == null
                ? D61AArrowPosition.RIGHT
                : arrowPosition;

        float leftSideX = doubleLine ? DOUBLE_ARROW_LEFT_X : SIMPLE_ARROW_LEFT_X;
        float rightSideX = doubleLine ? DOUBLE_ARROW_RIGHT_X : SIMPLE_ARROW_RIGHT_X;

        /*
         * Sans flèche on garde la position historique du logo. Avec une flèche,
         * le logo passe systématiquement sur le côté opposé : les deux éléments
         * ne peuvent donc plus jamais se superposer.
         */
        float logoCenterX = !arrowEnabled
                ? rightSideX
                : (effectiveArrowPosition == D61AArrowPosition.LEFT
                ? rightSideX
                : leftSideX);

        float logoCenterY = (doubleLine
                ? DOUBLE_AUTOROUTE_LOGO_Y
                : SIMPLE_AUTOROUTE_LOGO_Y) + yOffset;
        float logoSize = doubleLine
                ? DOUBLE_AUTOROUTE_LOGO_SIZE
                : SIMPLE_AUTOROUTE_LOGO_SIZE;

        poseStack.pushPose();
        poseStack.translate(0.5F, logoCenterY, 0.5F);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        getFacingRotation(renderState.facing)
                )
        );

        float centerX = logoCenterX - 0.5F;
        float half = logoSize / 2.0F;
        int light = renderState.lightCoords;

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(AUTOROUTE_LOGO_TEXTURE),
                (pose, consumer) -> addAutorouteLogoQuad(
                        pose,
                        consumer,
                        centerX - half,
                        centerX + half,
                        -half,
                        half,
                        AUTOROUTE_LOGO_Z,
                        light
                )
        );

        poseStack.popPose();
    }

    private static void submitArrow(
            net.minecraft.client.renderer.block.BlockModelRenderState arrowModel,
            D61AArrowPosition position,
            D61AArrowDirection direction,
            boolean doubleLine,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (position == null) {
            position = D61AArrowPosition.RIGHT;
        }

        if (direction == null) {
            direction = D61AArrowDirection.UP;
        }

        float arrowX;

        if (doubleLine) {
            arrowX = position == D61AArrowPosition.LEFT
                    ? DOUBLE_ARROW_LEFT_X
                    : DOUBLE_ARROW_RIGHT_X;
        } else {
            arrowX = position == D61AArrowPosition.LEFT
                    ? SIMPLE_ARROW_LEFT_X
                    : SIMPLE_ARROW_RIGHT_X;
        }

        float arrowY =
                (doubleLine ? DOUBLE_ARROW_Y : SIMPLE_ARROW_Y)
                        + yOffset;

        float scale =
                doubleLine
                        ? DOUBLE_ARROW_SCALE
                        : SIMPLE_ARROW_SCALE;

        poseStack.pushPose();

        /*
         * Centre du panneau, puis rotation selon l'orientation du bloc.
         */
        poseStack.translate(0.5F, arrowY, 0.5F);

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        getFacingRotation(renderState.facing)
                )
        );

        /*
         * Le modèle PNG est centré en X/Y autour de 0.5.
         */
        poseStack.translate(
                arrowX - 0.5F,
                0.0F,
                ARROW_Z
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        direction.modelRotationDegrees()
                )
        );

        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        arrowModel.submit(
                poseStack,
                collector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }

    private static void submitPanelText(
            String line1,
            String line2,
            String distance1,
            String distance2,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            D21AType panelType,
            boolean autorouteLogo,
            boolean doubleLine,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
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

        int textColor = panelType == D21AType.WHITE
                ? 0xFF000000
                : 0xFFFFFFFF;

        if (doubleLine) {
            submitTwoLinePanelText(
                    line1,
                    line2,
                    distance1,
                    distance2,
                    line1Font,
                    line2Font,
                    textColor,
                    autorouteLogo,
                    arrowEnabled,
                    arrowPosition,
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
                line1Font,
                textColor,
                autorouteLogo,
                arrowEnabled,
                arrowPosition,
                yOffset,
                renderState,
                poseStack,
                collector
        );
    }

    private static void submitSingleLinePanelText(
            String destination,
            String distance,
            RoadTextFont destinationFont,
            int textColor,
            boolean autorouteLogo,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        boolean leftArrow =
                arrowEnabled
                        && arrowPosition == D61AArrowPosition.LEFT;

        boolean rightArrow =
                arrowEnabled
                        && arrowPosition == D61AArrowPosition.RIGHT;

        boolean showAutorouteLogo = autorouteLogo;

        float destinationLeftEdge;
        float destinationMaxWidth;

        if (showAutorouteLogo && leftArrow) {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE_WITH_LOGO_AND_LEFT_ARROW;
            destinationMaxWidth = SIMPLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_LEFT_ARROW;
        } else if (showAutorouteLogo && rightArrow) {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO;
            destinationMaxWidth = SIMPLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_RIGHT_ARROW;
        } else if (showAutorouteLogo) {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO;
            destinationMaxWidth = distance.isBlank()
                    ? SIMPLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_NO_DISTANCE
                    : SIMPLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_AND_DISTANCE;
        } else if (leftArrow) {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE_WITH_LEFT_ARROW;
            destinationMaxWidth = SIMPLE_DESTINATION_MAX_WIDTH_WITH_LEFT_ARROW;
        } else if (rightArrow) {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE;
            destinationMaxWidth = SIMPLE_DESTINATION_MAX_WIDTH_WITH_RIGHT_ARROW;
        } else {
            destinationLeftEdge = SIMPLE_DESTINATION_LEFT_EDGE;
            destinationMaxWidth = distance.isBlank()
                    ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                    : DESTINATION_MAX_WIDTH_WITH_DISTANCE;
        }

        float textY = SIMPLE_LINE_Y + yOffset;

        if (!destination.isBlank()) {
            submitAnchoredText(
                    destination,
                    destinationLeftEdge,
                    textY,
                    SINGLE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    TextAnchor.LEFT,
                    destinationFont,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        /*
         * Flèche OU kilométrage : la distance reste mémorisée dans le BE
         * mais n'est pas affichée tant que la flèche est activée.
         */
        if (!arrowEnabled && !distance.isBlank()) {
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
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            int textColor,
            boolean autorouteLogo,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
            float yOffset,
            D61ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        boolean hasTop = !line1.isBlank();
        boolean hasBottom = !line2.isBlank();

        boolean leftArrow =
                arrowEnabled
                        && arrowPosition == D61AArrowPosition.LEFT;

        boolean rightArrow =
                arrowEnabled
                        && arrowPosition == D61AArrowPosition.RIGHT;

        boolean showAutorouteLogo = autorouteLogo;

        float destinationLeftEdge;
        float arrowDestinationMaxWidth;

        if (showAutorouteLogo && leftArrow) {
            destinationLeftEdge = DOUBLE_DESTINATION_LEFT_EDGE_WITH_LOGO_AND_LEFT_ARROW;
            arrowDestinationMaxWidth = DOUBLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_LEFT_ARROW;
        } else if (showAutorouteLogo && rightArrow) {
            destinationLeftEdge = DOUBLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO;
            arrowDestinationMaxWidth = DOUBLE_DESTINATION_MAX_WIDTH_WITH_LOGO_AND_RIGHT_ARROW;
        } else if (showAutorouteLogo) {
            destinationLeftEdge = DOUBLE_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO;
            arrowDestinationMaxWidth = DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO;
        } else if (leftArrow) {
            destinationLeftEdge = DOUBLE_DESTINATION_LEFT_EDGE_WITH_LEFT_ARROW;
            arrowDestinationMaxWidth = DOUBLE_DESTINATION_MAX_WIDTH_WITH_LEFT_ARROW;
        } else {
            destinationLeftEdge = DOUBLE_DESTINATION_LEFT_EDGE;
            arrowDestinationMaxWidth = DOUBLE_DESTINATION_MAX_WIDTH_WITH_RIGHT_ARROW;
        }

        float line1Y =
                (hasTop && hasBottom
                        ? DOUBLE_LINE_Y_TOP
                        : DOUBLE_LINE_Y_SINGLE)
                        + yOffset;

        float line2Y =
                (hasTop && hasBottom
                        ? DOUBLE_LINE_Y_BOTTOM
                        : DOUBLE_LINE_Y_SINGLE)
                        + yOffset;

        if (hasTop) {
            float maxWidth = arrowEnabled
                    ? arrowDestinationMaxWidth
                    : (showAutorouteLogo
                    ? (distance1.isBlank()
                    ? DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_NO_DISTANCE
                    : DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_AND_DISTANCE)
                    : (distance1.isBlank()
                    ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                    : DESTINATION_MAX_WIDTH_WITH_DISTANCE));

            submitAnchoredText(
                    line1,
                    destinationLeftEdge,
                    line1Y,
                    DOUBLE_DESTINATION_BASE_SCALE,
                    maxWidth,
                    TextAnchor.LEFT,
                    line1Font,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!arrowEnabled && !distance1.isBlank()) {
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
            float maxWidth = arrowEnabled
                    ? arrowDestinationMaxWidth
                    : (showAutorouteLogo
                    ? (distance2.isBlank()
                    ? DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_NO_DISTANCE
                    : DOUBLE_DESTINATION_MAX_WIDTH_WITH_AUTOROUTE_LOGO_AND_DISTANCE)
                    : (distance2.isBlank()
                    ? DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE
                    : DESTINATION_MAX_WIDTH_WITH_DISTANCE));

            submitAnchoredText(
                    line2,
                    destinationLeftEdge,
                    line2Y,
                    DOUBLE_DESTINATION_BASE_SCALE,
                    maxWidth,
                    TextAnchor.LEFT,
                    line2Font,
                    textColor,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (!arrowEnabled && !distance2.isBlank()) {
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
        submitAnchoredText(
                value,
                anchorX,
                worldY,
                baseScale,
                maxWorldWidth,
                anchor,
                RoadTextFont.L1,
                color,
                renderState,
                poseStack,
                collector
        );
    }

    private static void submitAnchoredText(
            String value,
            float anchorX,
            float worldY,
            float baseScale,
            float maxWorldWidth,
            TextAnchor anchor,
            RoadTextFont textFont,
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
                        .withStyle(
                                Style.EMPTY.withFont(getRoadFont(textFont))
                        );

        FormattedCharSequence text = component.getVisualOrderText();
        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWorldWidth / textWidth);

        poseStack.pushPose();

        poseStack.translate(0.5F, worldY, 0.5F);

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        getFacingRotation(renderState.facing)
                )
        );

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

    private static void addAutorouteLogoQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int light
    ) {
        addAutorouteLogoVertex(pose, consumer, left, bottom, z, 0.0F, 1.0F, light);
        addAutorouteLogoVertex(pose, consumer, right, bottom, z, 1.0F, 1.0F, light);
        addAutorouteLogoVertex(pose, consumer, right, top, z, 1.0F, 0.0F, light);
        addAutorouteLogoVertex(pose, consumer, left, top, z, 0.0F, 0.0F, light);
    }

    private static void addAutorouteLogoVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static FontDescription.Resource getRoadFont(
            RoadTextFont textFont
    ) {
        return textFont == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;
    }

    private static float getFacingRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case SOUTH -> 0F;
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    private static D21AType sanitizeType(D21AType type) {
        if (type == D21AType.GREEN) {
            return D21AType.GREEN;
        }
        if (type == D21AType.BLUE) {
            return D21AType.BLUE;
        }
        return D21AType.WHITE;
    }

    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.strip();
    }
}
