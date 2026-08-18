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

import java.util.Locale;

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

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /* ============================================================
     * RÉGLAGES TEXTE - COMMUNS
     * ============================================================ */

    private static final float TEXT_Z = 0.128F;

    /*
     * V49 : les kilométrages des D21A simples utilisent maintenant les
     * mêmes colonnes latérales que les variantes 2 lignes afin que toutes
     * les valeurs restent visuellement alignées d'un panneau à l'autre.
     */
    private static final float LEFT_DISTANCE_X = -0.1625F;
    private static final float RIGHT_DISTANCE_X = 1.055F;

    /*
     * V52 : correction du sens du réglage précédent. Sur les D21A simples
     * orientés vers la gauche, la destination doit au contraire être décalée
     * vers la droite. Les ancres sont maintenant identiques à celles des
     * variantes D21A2 afin d'obtenir une colonne cohérente.
     */
    private static final float LEFT_DESTINATION_RIGHT_EDGE = 1.43F;

    /*
     * V50 : le texte destination des D21A simples orientés vers la droite
     * est légèrement ramené vers la gauche pour s'aligner visuellement avec
     * les autres panneaux du même ensemble.
     */
    private static final float RIGHT_DESTINATION_LEFT_EDGE = -0.43F;

    private static final float LEFT_DESTINATION_RIGHT_EDGE_WITH_AUTOROUTE_LOGO = 1.00F;
    private static final float RIGHT_DESTINATION_LEFT_EDGE_WITH_AUTOROUTE_LOGO = 0.00F;

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

        /*
         * V48 : les deux modèles D21A simples sont désormais réellement
         * centrés sur X = 8. Le texte utilise donc directement le repère
         * commun du panneau, sans compensation gauche/droite.
         */
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
                        destinationFont,
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
                    destinationFont,
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
                    line1Font,
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
                    line2Font,
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
        return textFont == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;
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
