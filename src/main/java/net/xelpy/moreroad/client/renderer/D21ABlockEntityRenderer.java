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
import net.xelpy.moreroad.block.custom.D21A2Block;
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21APanelLayout;
import net.xelpy.moreroad.block.custom.D21APanelModelBlock;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;


public class D21ABlockEntityRenderer
        implements BlockEntityRenderer<D21ABlockEntity, D21ARenderState> {

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

    private static final FontDescription.Resource ROAD_FONT_L2 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l2"
                    )
            );

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /* ============================================================
     * RÉGLAGES TEXTE - COMMUNS
     * ============================================================ */

    /*
     * V100 : les zones D21 sont resserrées vers leurs bords utiles :
     * kilométrage plus près de la pointe et destination plus près du bord
     * opposé, afin d'exploiter davantage la largeur réelle de la plaque.
     */

    private static final float TEXT_Z = 0.1925F;

    /*
     * V99 : le texte est posé légèrement devant la face du panneau et rendu
     * avec un écart de profondeur réel et le mode NORMAL pour éviter à la fois
     * les zones transparentes autour des glyphes et le z-fighting à distance.
     */

    /*
     * V49 : les kilométrages des D21A simples utilisent maintenant les
     * mêmes colonnes latérales que les variantes 2 lignes afin que toutes
     * les valeurs restent visuellement alignées d'un panneau à l'autre.
     */
    private static final float LEFT_DISTANCE_X = -0.250F;
    private static final float RIGHT_DISTANCE_X = 1.250F;

    /*
     * V52 : correction du sens du réglage précédent. Sur les D21A simples
     * orientés vers la gauche, la destination doit au contraire être décalée
     * vers la droite. Les ancres sont maintenant identiques à celles des
     * variantes D21A2 afin d'obtenir une colonne cohérente.
     */
    private static final float LEFT_DESTINATION_RIGHT_EDGE = 1.31F;

    /*
     * V50 : le texte destination des D21A simples orientés vers la droite
     * est légèrement ramené vers la gauche pour s'aligner visuellement avec
     * les autres panneaux du même ensemble.
     */
    private static final float RIGHT_DESTINATION_LEFT_EDGE = -0.31F;

    private static final float LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 0.90F;
    /* Miroir exact de 0.90 autour du centre X = 0.5. */
    private static final float RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.10F;

    private static final float DISTANCE_MAX_WIDTH = 0.28F;


    /* ============================================================
     * D21A - 1 LIGNE
     * ============================================================ */

    private static final float SINGLE_TEXT_Y = 0.675F;

    private static final float SINGLE_DESTINATION_BASE_SCALE = 0.0170F;
    private static final float SINGLE_DISTANCE_BASE_SCALE = 0.0160F;

    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.18F;
    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.50F;

    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO = 0.66F;
    private static final float SINGLE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO = 0.84F;

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

    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE = 1.18F;
    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE = 1.50F;

    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO = 0.62F;
    private static final float TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO = 0.76F;

    /*
     * Les distances gardent EXACTEMENT la position horizontale effective
     * de la V8. L'ancien offset -1/16 est simplement intégré directement
     * dans ces deux constantes pour que rien ne bouge de ce côté-là.
     */
    private static final float TWO_LINE_LEFT_DISTANCE_X = -0.250F;
    private static final float TWO_LINE_RIGHT_DISTANCE_X = 1.250F;

    /*
     * Le panneau blanc à flèche droite donne visuellement un peu plus
     * d'espace avant la pointe que les panneaux colorés. On avance donc
     * uniquement son kilométrage d'environ un pixel Minecraft, sans
     * modifier les panneaux verts/bleus.
     */
    private static final float TWO_LINE_RIGHT_DISTANCE_X_WHITE = 1.270F;

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
    private static final float TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE = 1.31F;
    private static final float TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE = -0.31F;

    private static final float TWO_LINE_LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 0.90F;
    private static final float TWO_LINE_RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.10F;

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

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel = blockEntity.getPanel(i);

            renderState.enabled[i] = panel.enabled();
            renderState.line1[i] = panel.line1();
            renderState.line2[i] = panel.line2();
            renderState.distance1[i] = panel.distance1();
            renderState.distance2[i] = panel.distance2();
            renderState.panelTypes[i] = panel.type();
            renderState.line1Fonts[i] = panel.line1Font();
            renderState.line2Fonts[i] = panel.line2Font();
            renderState.line1Spacing[i] = panel.line1Spacing();
            renderState.line2Spacing[i] = panel.line2Spacing();
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

        if (
                renderState.cartoucheType != null
                        && renderState.cartoucheType.isVisible()
        ) {
            double cartoucheBottomY =
                    CartoucheLayout.getD21BottomY(
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

        submitCartouche(
                renderState,
                poseStack,
                collector
        );

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
                    renderState.line1Fonts[i],
                    renderState.line2Fonts[i],
                    renderState.line1Spacing[i],
                    renderState.line2Spacing[i],
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

    private static void submitCartouche(
            D21ARenderState renderState,
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
                (float) CartoucheLayout.getD21HighestTopY(
                        renderState.enabled,
                        renderState.doubleLines
                );

        float yOffset =
                (float) CartoucheLayout.getD21BottomY(
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
            D21ARenderState renderState,
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

    private static void submitPanelText(
            String line1,
            String line2,
            String distance1,
            String distance2,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            boolean line1Spacing,
            boolean line2Spacing,
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
                    line1Font,
                    line2Font,
                    line1Spacing,
                    line2Spacing,
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
                line1Font,
                line1Spacing,
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
            RoadTextFont destinationFont,
            boolean destinationSpacing,
            int textColor,
            boolean arrowRight,
            boolean showAutorouteLogo,
            float yOffset,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float destinationMaxWidth;
        boolean destinationTracked = (destinationFont == RoadTextFont.L1 || destinationFont == RoadTextFont.L2)
                && destinationSpacing;

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

        /*
         * V99 : le corps rectangulaire des deux variantes est maintenant
         * réellement centré sur X = 8. Les ancres texte sont donc exprimées
         * directement dans ce repère commun, sans décalage supplémentaire.
         */
        float textY = SINGLE_TEXT_Y + yOffset;

        if (arrowRight) {
            if (!destination.isBlank()) {
                submitAnchoredTrackedText(
                        destination,
                        showAutorouteLogo
                                ? RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO
                                : RIGHT_DESTINATION_LEFT_EDGE,
                        textY,
                        SINGLE_DESTINATION_BASE_SCALE,
                        destinationMaxWidth,
                        TextAnchor.LEFT,
                        destinationFont,
                        textColor,
                        destinationTracked,
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
            submitAnchoredTrackedText(
                    destination,
                    showAutorouteLogo
                            ? LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO
                            : LEFT_DESTINATION_RIGHT_EDGE,
                    textY,
                    SINGLE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    TextAnchor.RIGHT,
                    destinationFont,
                    textColor,
                    destinationTracked,
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
            boolean line1Spacing,
            boolean line2Spacing,
            int textColor,
            D21AType panelType,
            boolean arrowRight,
            boolean showAutorouteLogo,
            float yOffset,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        boolean line1Tracked = (line1Font == RoadTextFont.L1 || line1Font == RoadTextFont.L2) && line1Spacing;
        boolean line2Tracked = (line2Font == RoadTextFont.L1 || line2Font == RoadTextFont.L2) && line2Spacing;

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
                destinationMaxWidth = 0.52F;
            } else {
                destinationMaxWidth =
                        hasAnyDistance
                                ? TWO_LINE_DESTINATION_MAX_WIDTH_WITH_DISTANCE_AND_AUTOROUTE_LOGO
                                : TWO_LINE_DESTINATION_MAX_WIDTH_WITHOUT_DISTANCE_AND_AUTOROUTE_LOGO;
            }
        } else {
            if (hasAnyDistance && arrowRight) {
                destinationMaxWidth = 1.12F;
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
            submitAnchoredTrackedText(
                    line1,
                    destinationAnchor,
                    line1Y,
                    TWO_LINE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    destinationTextAnchor,
                    line1Font,
                    textColor,
                    line1Tracked,
                    renderState,
                    poseStack,
                    collector
            );
        }

        if (hasBottom) {
            submitAnchoredTrackedText(
                    line2,
                    destinationAnchor,
                    line2Y,
                    TWO_LINE_DESTINATION_BASE_SCALE,
                    destinationMaxWidth,
                    destinationTextAnchor,
                    line2Font,
                    textColor,
                    line2Tracked,
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

    /**
     * Espacement des lettres réel (mots dessinés lettre par lettre, chacune
     * positionnée individuellement), et non plus une espace insérée dans le
     * texte : une espace pleine entre chaque lettre (629/2048 em, mesuré sur
     * caracteres_l1.ttf) est bien plus large que l'espacement d'un vrai
     * panneau, et une espace fine Unicode (U+2009) ressort en glyphe
     * manquant faute d'exister dans la police routière ou son repli.
     *
     * Un essai d'espacement "optique" (annuler le vide de bord propre à
     * chaque lettre, mesuré sur le contour réel des .ttf, avant d'ajouter
     * l'écart) a été tenté puis abandonné : sur le "L", ce vide mesuré sur
     * le tracé vectoriel est bien plus grand que ce que Minecraft affiche
     * réellement une fois la police rasterisée, et l'annuler faisait
     * carrément chevaucher le "L" et la lettre suivante. Espacement fixe
     * donc, plus sûr et prévisible, même s'il reste un peu moins régulier
     * que l'idéal théorique sur certaines lettres.
     *
     * Valeur en pixels de police (comme font.width(...), pas en unités de
     * dessin de la police) : à ajuster si l'écart ne correspond pas encore
     * aux vrais panneaux.
     */
    private static final float LETTER_TRACKING_PIXELS = 1.2F;

    private static void submitAnchoredTrackedText(
            String value,
            float anchorX,
            float worldY,
            float baseScale,
            float maxWorldWidth,
            TextAnchor anchor,
            RoadTextFont textFont,
            int color,
            boolean tracked,
            D21ARenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!tracked || value.codePointCount(0, value.length()) <= 1) {
            submitAnchoredText(
                    value, anchorX, worldY, baseScale, maxWorldWidth, anchor,
                    textFont, color, renderState, poseStack, collector
            );
            return;
        }

        Font trackedFont = Minecraft.getInstance().font;
        FontDescription.Resource resource = getRoadFont(textFont);
        int[] codePoints = value.codePoints().toArray();
        /*
         * Font.width(...) fait Mth.ceil(...) sur la largeur : appelé lettre
         * par lettre, chaque appel arrondit indépendamment au pixel
         * supérieur, ajoutant jusqu'à ~1px de bruit différent par lettre
         * (au lieu d'un seul arrondi sur la largeur totale de la chaîne).
         * C'était la vraie cause de l'espacement irrégulier : on utilise
         * donc stringWidth (float, sans arrondi intermédiaire) via le même
         * splitter que Font.width().
         */
        float[] charWidths = new float[codePoints.length];
        FormattedCharSequence[] sequences = new FormattedCharSequence[codePoints.length];
        for (int index = 0; index < codePoints.length; index++) {
            sequences[index] = Component.literal(new String(Character.toChars(codePoints[index])))
                    .withStyle(Style.EMPTY.withFont(resource))
                    .getVisualOrderText();
            charWidths[index] = trackedFont.getSplitter().stringWidth(sequences[index]);
        }

        float[] advances = new float[codePoints.length - 1];
        float totalWidth = charWidths[codePoints.length - 1];
        for (int index = 0; index < codePoints.length - 1; index++) {
            float advance = charWidths[index] + LETTER_TRACKING_PIXELS;
            advances[index] = advance;
            totalWidth += advance;
        }
        if (totalWidth <= 0.0F) {
            return;
        }

        float scale = Math.min(baseScale, maxWorldWidth / totalWidth);

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

        float startX = switch (anchor) {
            case LEFT -> 0F;
            case CENTER -> -totalWidth / 2.0F;
            case RIGHT -> -totalWidth;
        };
        float textY = -trackedFont.lineHeight / 2.0F;

        float cursor = startX;
        for (int index = 0; index < codePoints.length; index++) {
            collector.submitText(
                    poseStack, cursor, textY, sequences[index], false,
                    Font.DisplayMode.NORMAL, renderState.lightCoords, color, 0x00000000, 0x00000000
            );
            if (index < advances.length) {
                cursor += advances[index];
            }
        }

        poseStack.popPose();
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
                                Style.EMPTY.withFont(getRoadFont(textFont))
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

    private static FontDescription.Resource getRoadFont(
            RoadTextFont textFont
    ) {
        return switch (textFont) {
            case L1, NORMAL -> ROAD_FONT_L1;
            case L2 -> ROAD_FONT_L2;
            case L4 -> ROAD_FONT_L4;
        };
    }

    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .strip();
    }
}
