package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.PanonceauBlock;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.custom.PanonceauVariant;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer du bloc générique de panonceaux.
 *
 * La plaque est réellement rendue en volume (face, dos et chants) et sa
 * largeur/hauteur s'adapte au ratio du SVG choisi. Trois plaques peuvent être
 * empilées dans le même bloc afin de conserver l'espacement réel d'un support
 * routier sans imposer un bloc Minecraft entier entre chaque panonceau.
 */
public class PanonceauBlockEntityRenderer
        implements BlockEntityRenderer<PanonceauBlockEntity, PanonceauRenderState> {

    /*
     * Le nouveau modèle Blockbench utilise la même texture gris foncé que le
     * poteau pour tous les chants et pour le dos de la plaque. Le corps 3D
     * dynamique reprend donc directement cette texture, exactement comme le
     * modèle fourni par l'utilisateur.
     */
    private static final Identifier SOLID_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    MoreRoad.MODID,
                    "textures/block/poteau_block.png"
            );
    private static final Identifier LEGACY_FRONT_FACE_TEXTURE = texture("panel_face_white.png");
    private static final Identifier FRONT_BLACK_TEXTURE = texture("front_black.png");
    private static final Identifier FRONT_WHITE_TEXTURE = texture("front_white.png");
    private static final Identifier FRONT_RED_TEXTURE = texture("front_red.png");
    private static final Identifier HARDWARE_TEXTURE = texture("mounting_metal.png");

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );
    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    /*
     * Géométrie V3 basée sur le dernier modèle Blockbench réellement fourni
     * par l'utilisateur.
     *
     * La plaque NORTH occupe Z = 6 -> 7 dans le modèle. Dans le repère local
     * du renderer (centre du bloc = 8), cela donne :
     *   face arrière : (8 - 7) / 16 = 0.0625
     *   face avant   : (8 - 6) / 16 = 0.125
     *
     * On reprend donc exactement cette profondeur, sans rajouter d'avance
     * supplémentaire. C'est ce qui permet d'obtenir le même placement que le
     * modèle Blockbench validé par l'utilisateur.
     */
    private static final float POLE_FRONT_Z = 0.0625F;
    private static final float PANEL_Z_BACK = 0.0625F;
    private static final float PANEL_Z_FRONT = 0.125F;
    private static final float FRONT_BLACK_Z = PANEL_Z_FRONT + 0.0008F;
    private static final float FRONT_WHITE_Z = PANEL_Z_FRONT + 0.0016F;
    private static final float CONTENT_Z = PANEL_Z_FRONT + 0.0024F;
    private static final float TEXT_Z = CONTENT_Z + 0.0020F;
    private static final float TEXT_VERTICAL_BIAS = 1.15F;

    /*
     * Les deux rails du modèle restent à Z = 6.90 -> 7.18. Convertis dans le
     * même repère local, ils occupent 0.05125 -> 0.06875 : juste derrière la
     * plaque et très légèrement dans la face avant du poteau, comme dans le
     * modèle Blockbench d'origine.
     */
    private static final float HARDWARE_Z_BACK = 0.05125F;
    private static final float HARDWARE_Z_FRONT = 0.06875F;

    // Le nouveau gabarit utilise de nombreux petits éléments pour former les
    // arrondis. On reproduit ce principe dynamiquement pour toutes les variantes.
    private static final int ROUNDED_CORNER_STEPS = 10;
    private static final float ROUNDED_CORNER_RATIO = 0.13F;

    private static final float STACK_TOP = 0.965F;
    private static final float STACK_BOTTOM = 0.055F;
    private static final float STACK_GAP = 0.030F;
    private static final float MAX_PANEL_WIDTH = 0.92F;

    /*
     * M11B regroupe deux panonceaux horizontaux distincts dans une même entrée.
     * Les ratios ci-dessous correspondent aux deux groupes graphiques du PNG :
     * un panonceau supérieur et un panonceau inférieur séparés par un vrai vide.
     */
    private static final float M11B_UPPER_BOTTOM = 0.600F;
    private static final float M11B_UPPER_TOP = 0.912F;
    private static final float M11B_LOWER_BOTTOM = 0.088F;
    private static final float M11B_LOWER_TOP = 0.400F;

    public PanonceauBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PanonceauRenderState createRenderState() {
        return new PanonceauRenderState();
    }

    @Override
    public void extractRenderState(
            PanonceauBlockEntity blockEntity,
            PanonceauRenderState renderState,
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

        BlockState state = blockEntity.getBlockState();
        renderState.facing = state.getValue(PanonceauBlock.FACING);

        PanonceauEntry[] entries = blockEntity.getEntries();
        for (int i = 0; i < renderState.entries.length; i++) {
            renderState.entries[i] = i < entries.length
                    ? entries[i]
                    : PanonceauEntry.disabled();
        }
    }

    @Override
    public void submit(
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        List<PanelLayout> panels = buildLayouts(state.entries);

        for (int i = 0; i < panels.size(); i++) {
            PanelLayout panel = panels.get(i);

            int baseOrder = i * 5;
            submitPanelBody(panel, state, poseStack, collector, baseOrder);
            submitRearHardware(panel, state, poseStack, collector, baseOrder + 1);
            submitFrontFace(panel, state, poseStack, collector, baseOrder + 2);
            submitFrontTexture(panel, state, poseStack, collector, baseOrder + 3);
            submitDynamicContent(panel, state, poseStack, collector);
        }
    }

    private static List<PanelLayout> buildLayouts(PanonceauEntry[] entries) {
        List<PanonceauEntry> active = new ArrayList<>();
        for (PanonceauEntry entry : entries) {
            if (entry != null && entry.enabled()) {
                active.add(entry);
            }
        }

        if (active.isEmpty()) {
            return List.of();
        }

        float[] preferredHeights = new float[active.size()];
        float totalHeight = 0.0F;

        for (int i = 0; i < active.size(); i++) {
            float height = preferredHeight(active.get(i).variant().aspectRatio());
            preferredHeights[i] = height;
            totalHeight += height;
        }
        totalHeight += STACK_GAP * Math.max(0, active.size() - 1);

        float available = STACK_TOP - STACK_BOTTOM;
        float scale = totalHeight > available ? available / totalHeight : 1.0F;
        float gap = STACK_GAP * scale;

        List<PanelLayout> result = new ArrayList<>();
        float top = STACK_TOP;

        for (int i = 0; i < active.size(); i++) {
            PanonceauEntry entry = active.get(i);
            float aspect = Math.max(0.45F, entry.variant().aspectRatio());
            float height = preferredHeights[i] * scale;
            float width = Math.min(MAX_PANEL_WIDTH, height * aspect);

            if (width >= MAX_PANEL_WIDTH - 0.0001F) {
                height = width / aspect;
            }

            float bottom = top - height;
            result.add(new PanelLayout(entry, width, height, bottom, top));
            top = bottom - gap;
        }

        return result;
    }

    private static float preferredHeight(float aspectRatio) {
        if (aspectRatio >= 5.0F) {
            return 0.165F;
        }
        if (aspectRatio >= 3.0F) {
            return 0.215F;
        }
        if (aspectRatio >= 1.80F) {
            return 0.285F;
        }
        if (aspectRatio >= 1.05F) {
            return 0.355F;
        }
        return 0.475F;
    }

    private static float panelY(PanelLayout panel, float ratioFromBottom) {
        return panel.bottom() + panel.height() * ratioFromBottom;
    }

    private static void submitPanelBody(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        float left = -panel.width() / 2.0F;
        float right = panel.width() / 2.0F;
        float bottom = panel.bottom();
        float top = panel.top();

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_TEXTURE),
                (pose, consumer) -> {
                    PanonceauVariant variant = panel.entry().variant();
                    if (variant == PanonceauVariant.M11B) {
                        // M11B représente deux panonceaux physiques superposés,
                        // pas une grande plaque carrée pleine.
                        float upperBottom = panelY(panel, M11B_UPPER_BOTTOM);
                        float upperTop = panelY(panel, M11B_UPPER_TOP);
                        float lowerBottom = panelY(panel, M11B_LOWER_BOTTOM);
                        float lowerTop = panelY(panel, M11B_LOWER_TOP);

                        addRoundedRectPrism(
                                pose, consumer, left, right, upperBottom, upperTop,
                                PANEL_Z_BACK, PANEL_Z_FRONT, light
                        );
                        addRoundedRectPrism(
                                pose, consumer, left, right, lowerBottom, lowerTop,
                                PANEL_Z_BACK, PANEL_Z_FRONT, light
                        );
                    } else if (variant.isTriangular()) {
                        addRoundedTrianglePrism(
                                pose, consumer, left, right, bottom, top,
                                PANEL_Z_BACK, PANEL_Z_FRONT, light
                        );
                    } else if (variant == PanonceauVariant.M12A_C || variant == PanonceauVariant.M12B_C) {
                        // Variante sur subjectile carré foncé.
                        addBox(pose, consumer, left, right, bottom, top, PANEL_Z_BACK, PANEL_Z_FRONT, light);
                    } else {
                        addRoundedRectPrism(
                                pose, consumer, left, right, bottom, top,
                                PANEL_Z_BACK, PANEL_Z_FRONT, light
                        );
                    }
                }
        );

        poseStack.popPose();
    }

    private static void submitRearHardware(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        float centerY = (panel.bottom() + panel.top()) / 2.0F;
        float railHalfWidth = Math.min(
                panel.width() / 2.0F - 0.060F,
                Math.max(0.090F, panel.width() * 0.24F)
        );
        float railThickness = Math.min(
                0.022F,
                Math.max(0.012F, panel.height() * 0.06F)
        );

        float[] railCenters;
        if (panel.entry().variant() == PanonceauVariant.M11B) {
            railCenters = new float[]{
                    (panelY(panel, M11B_LOWER_BOTTOM) + panelY(panel, M11B_LOWER_TOP)) * 0.5F,
                    (panelY(panel, M11B_UPPER_BOTTOM) + panelY(panel, M11B_UPPER_TOP)) * 0.5F
            };
        } else if (panel.height() < 0.23F) {
            railCenters = new float[]{centerY};
        } else {
            float offset = Math.min(0.074F, panel.height() * 0.22F);
            railCenters = new float[]{centerY - offset, centerY + offset};
        }

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(HARDWARE_TEXTURE),
                (pose, consumer) -> {
                    for (float railY : railCenters) {
                        float halfHeight = railThickness / 2.0F;
                        float currentRailHalfWidth = railHalfWidth;

                        if (panel.entry().variant().isTriangular()) {
                            float relativeHeight = (railY - panel.bottom()) / Math.max(0.001F, panel.height());
                            float triangularHalfWidth = (panel.width() * 0.5F) * relativeHeight;
                            currentRailHalfWidth = Math.min(
                                    railHalfWidth,
                                    Math.max(0.080F, triangularHalfWidth - 0.022F)
                            );
                        }

                        // Rail horizontal gris derrière la plaque.
                        addBox(
                                pose,
                                consumer,
                                -currentRailHalfWidth,
                                currentRailHalfWidth,
                                railY - halfHeight,
                                railY + halfHeight,
                                HARDWARE_Z_BACK,
                                HARDWARE_Z_FRONT,
                                light
                        );

                        // Petites brides plus discrètes, dans la même teinte grise que le dos du panneau.
                        float collarOuter = 0.090F;
                        float collarInner = 0.066F;
                        float collarHalfHeight = railThickness * 0.95F;

                        addBox(
                                pose,
                                consumer,
                                -collarOuter,
                                -collarInner,
                                railY - collarHalfHeight,
                                railY + collarHalfHeight,
                                HARDWARE_Z_BACK - 0.0025F,
                                HARDWARE_Z_FRONT,
                                light
                        );
                        addBox(
                                pose,
                                consumer,
                                collarInner,
                                collarOuter,
                                railY - collarHalfHeight,
                                railY + collarHalfHeight,
                                HARDWARE_Z_BACK - 0.0025F,
                                HARDWARE_Z_FRONT,
                                light
                        );
                    }
                }
        );

        poseStack.popPose();
    }

    private static void submitFrontFace(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        PanonceauVariant variant = panel.entry().variant();
        float left = -panel.width() / 2.0F;
        float right = panel.width() / 2.0F;
        float bottom = panel.bottom();
        float top = panel.top();

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        if (variant == PanonceauVariant.M12A || variant == PanonceauVariant.M12B) {
            /*
             * La texture M12 contient désormais la face réglementaire complète
             * (bande rouge, fond blanc, vélo et flèche). On ne redessine plus
             * un second triangle par-dessus : c'était la source des bandes
             * rouges décalées et des superpositions visibles.
             */
            poseStack.popPose();
            return;
        }

        if (variant == PanonceauVariant.M12A_C || variant == PanonceauVariant.M12B_C) {
            // Subjectile carré foncé ; le triangle complet est dans la texture.
            collector.order(order).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(FRONT_BLACK_TEXTURE),
                    (pose, consumer) -> addFaceQuad(pose, consumer, left, right, bottom, top, FRONT_BLACK_Z, light)
            );
            poseStack.popPose();
            return;
        }

        if (variant == PanonceauVariant.M11B) {
            float upperBottom = panelY(panel, M11B_UPPER_BOTTOM);
            float upperTop = panelY(panel, M11B_UPPER_TOP);
            float lowerBottom = panelY(panel, M11B_LOWER_BOTTOM);
            float lowerTop = panelY(panel, M11B_LOWER_TOP);
            float bandHeight = upperTop - upperBottom;
            float outerRadius = bandHeight * 0.14F;
            float border = Math.max(0.0035F, bandHeight * 0.035F);
            float innerRadius = Math.max(0.001F, outerRadius - border);

            collector.order(order).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(FRONT_BLACK_TEXTURE),
                    (pose, consumer) -> {
                        addRoundedRectFace(
                                pose, consumer, left, right, upperBottom, upperTop,
                                outerRadius, FRONT_BLACK_Z, light
                        );
                        addRoundedRectFace(
                                pose, consumer, left, right, lowerBottom, lowerTop,
                                outerRadius, FRONT_BLACK_Z, light
                        );
                    }
            );

            collector.order(order).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(FRONT_WHITE_TEXTURE),
                    (pose, consumer) -> {
                        addRoundedRectFace(
                                pose, consumer,
                                left + border, right - border,
                                upperBottom + border, upperTop - border,
                                innerRadius, FRONT_WHITE_Z, light
                        );
                        addRoundedRectFace(
                                pose, consumer,
                                left + border, right - border,
                                lowerBottom + border, lowerTop - border,
                                innerRadius, FRONT_WHITE_Z, light
                        );
                    }
            );

            poseStack.popPose();
            return;
        }

        if (!usesModelMatchedFace(variant)) {
            /*
             * Certaines variantes conservent volontairement leur rendu SVG
             * historique : les triangles M12a/M12b ainsi que les versions
             * M12a_C / M12b_C qui gardent leur support carré directement dans
             * la texture d'origine.
             */
            collector.order(order).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(LEGACY_FRONT_FACE_TEXTURE),
                    (pose, consumer) -> addFaceQuad(
                            pose,
                            consumer,
                            left,
                            right,
                            bottom,
                            top,
                            FRONT_WHITE_Z,
                            light
                    )
            );
            poseStack.popPose();
            return;
        }

        float minDimension = Math.min(panel.width(), panel.height());
        float outerRadius = minDimension * ROUNDED_CORNER_RATIO;
        float border = Math.max(0.0035F, minDimension * 0.018F);
        float innerLeft = left + border;
        float innerRight = right - border;
        float innerBottom = bottom + border;
        float innerTop = top - border;
        float innerRadius = Math.max(0.001F, outerRadius - border);

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(FRONT_BLACK_TEXTURE),
                (pose, consumer) -> addRoundedRectFace(
                        pose,
                        consumer,
                        left,
                        right,
                        bottom,
                        top,
                        outerRadius,
                        FRONT_BLACK_Z,
                        light
                )
        );

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(FRONT_WHITE_TEXTURE),
                (pose, consumer) -> addRoundedRectFace(
                        pose,
                        consumer,
                        innerLeft,
                        innerRight,
                        innerBottom,
                        innerTop,
                        innerRadius,
                        FRONT_WHITE_Z,
                        light
                )
        );

        poseStack.popPose();
    }

    private static boolean usesModelMatchedFace(PanonceauVariant variant) {
        /*
         * Toutes les familles M non triangulaires utilisent désormais la même
         * méthode que M4 : la face blanche et le filet noir sont dessinés par
         * le modèle 3D lui-même, et les textures SVG ne contiennent plus que
         * le pictogramme / texte utile. Cela supprime définitivement les coins
         * blancs parasites et garantit des arrondis parfaitement identiques
         * entre le visualiseur et le rendu en jeu.
         */
        return variant != null
                && !variant.isTriangular()
                && variant != PanonceauVariant.M11B
                && variant != PanonceauVariant.M12A_C
                && variant != PanonceauVariant.M12B_C;
    }

    private static void submitTriangleFront(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order,
            float left,
            float right,
            float bottom,
            float top,
            int light,
            boolean compact
    ) {
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(FRONT_RED_TEXTURE),
                (pose, consumer) -> addRoundedTriangleFace(pose, consumer, left, right, bottom, top, FRONT_BLACK_Z, light)
        );

        float width = right - left;
        float height = top - bottom;
        float borderX = width * (compact ? 0.095F : 0.085F);
        float borderTop = height * (compact ? 0.090F : 0.080F);
        float borderBottom = height * (compact ? 0.145F : 0.135F);
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(FRONT_WHITE_TEXTURE),
                (pose, consumer) -> addRoundedTriangleFace(
                        pose,
                        consumer,
                        left + borderX,
                        right - borderX,
                        bottom + borderBottom,
                        top - borderTop,
                        FRONT_WHITE_Z,
                        light
                )
        );
    }

    private static void addRoundedTriangleFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int light
    ) {
        float[][] points = roundedTrianglePoints(left, right, bottom, top);
        float centerX = 0.0F;
        float centerY = 0.0F;
        for (float[] point : points) {
            centerX += point[0];
            centerY += point[1];
        }
        centerX /= points.length;
        centerY /= points.length;

        for (int i = 0; i < points.length; i++) {
            float[] a = points[i];
            float[] b = points[(i + 1) % points.length];
            quad(
                    pose, consumer,
                    centerX, centerY, z,
                    a[0], a[1], z,
                    b[0], b[1], z,
                    b[0], b[1], z,
                    0.0F, 0.0F, 1.0F, light
            );
        }
    }

    private static void addRoundedTrianglePrism(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int light
    ) {
        float[][] points = roundedTrianglePoints(left, right, bottom, top);
        float centerX = 0.0F;
        float centerY = 0.0F;
        for (float[] point : points) {
            centerX += point[0];
            centerY += point[1];
        }
        centerX /= points.length;
        centerY /= points.length;

        for (int i = 0; i < points.length; i++) {
            float[] a = points[i];
            float[] b = points[(i + 1) % points.length];

            quad(
                    pose, consumer,
                    centerX, centerY, front,
                    a[0], a[1], front,
                    b[0], b[1], front,
                    b[0], b[1], front,
                    0.0F, 0.0F, 1.0F, light
            );
            quad(
                    pose, consumer,
                    centerX, centerY, back,
                    b[0], b[1], back,
                    a[0], a[1], back,
                    a[0], a[1], back,
                    0.0F, 0.0F, -1.0F, light
            );

            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = len > 0.0001F ? dy / len : 0.0F;
            float ny = len > 0.0001F ? -dx / len : 0.0F;
            quad(
                    pose, consumer,
                    a[0], a[1], front,
                    a[0], a[1], back,
                    b[0], b[1], back,
                    b[0], b[1], front,
                    nx, ny, 0.0F, light
            );
        }
    }

    private static float[][] roundedTrianglePoints(float left, float right, float bottom, float top) {
        float width = right - left;
        float height = top - bottom;
        float topCornerX = width * 0.075F;
        float topCornerY = height * 0.055F;
        float tipCornerX = width * 0.040F;
        float tipCornerY = height * 0.060F;
        float centerX = (left + right) * 0.5F;
        return new float[][]{
                {left + topCornerX, top},
                {right - topCornerX, top},
                {right, top - topCornerY},
                {centerX + tipCornerX, bottom + tipCornerY},
                {centerX, bottom},
                {centerX - tipCornerX, bottom + tipCornerY},
                {left, top - topCornerY}
        };
    }

    private static void submitFrontTexture(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        Identifier texture = texture(panel.entry().variant().textureFile());
        float left = -panel.width() / 2.0F;
        float right = panel.width() / 2.0F;

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFaceQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        panel.bottom(),
                        panel.top(),
                        CONTENT_Z,
                        light
                )
        );

        poseStack.popPose();
    }

    private static void submitDynamicContent(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        PanonceauVariant variant = panel.entry().variant();
        String value = cleanText(panel.entry().value());
        float centerY = (panel.bottom() + panel.top()) / 2.0F;
        float width = panel.width();
        float height = panel.height();

        switch (variant.renderMode()) {
            case FIXED -> {
            }
            case CENTER_VALUE -> submitFittedText(
                    value,
                    0.0F,
                    centerY,
                    width * 0.80F,
                    height * 0.57F,
                    state,
                    poseStack,
                    collector
            );
            case CENTER_VALUE_WITH_ARROWS -> submitFittedText(
                    value,
                    0.0F,
                    centerY,
                    width * 0.52F,
                    height * 0.58F,
                    state,
                    poseStack,
                    collector
            );
            case M3B_RIGHT_VALUE -> submitFittedText(
                    value,
                    width * 0.245F,
                    centerY,
                    width * 0.40F,
                    height * 0.58F,
                    state,
                    poseStack,
                    collector
            );
            case M3B_LEFT_VALUE -> submitFittedText(
                    value,
                    -width * 0.245F,
                    centerY,
                    width * 0.40F,
                    height * 0.58F,
                    state,
                    poseStack,
                    collector
            );
            case LOWER_VALUE -> submitFittedText(
                    value,
                    0.0F,
                    centerY - height * 0.285F,
                    width * 0.62F,
                    height * 0.25F,
                    state,
                    poseStack,
                    collector
            );
            case UPPER_VALUE -> submitFittedText(
                    value,
                    0.0F,
                    centerY + height * 0.285F,
                    width * 0.66F,
                    height * 0.29F,
                    state,
                    poseStack,
                    collector
            );
            case M5A -> {
                submitFittedText(
                        "STOP",
                        0.0F,
                        centerY + height * 0.205F,
                        width * 0.78F,
                        height * 0.30F,
                        state,
                        poseStack,
                        collector
                );
                submitFittedText(
                        value,
                        0.0F,
                        centerY - height * 0.205F,
                        width * 0.82F,
                        height * 0.31F,
                        state,
                        poseStack,
                        collector
                );
            }
            case M5B -> {
                submitFittedText(
                        "STOP",
                        -width * 0.225F,
                        centerY,
                        width * 0.40F,
                        height * 0.55F,
                        state,
                        poseStack,
                        collector
                );
                submitFittedText(
                        value,
                        width * 0.235F,
                        centerY,
                        width * 0.43F,
                        height * 0.55F,
                        state,
                        poseStack,
                        collector
                );
            }
            case M1A_DUAL_TEXT -> {
                String[] values = splitCustomText(value);
                submitWrappedFittedText(
                        values[0], -width * 0.315F, centerY,
                        width * 0.255F, height * 0.72F, 4,
                        0xFF111111, ROAD_FONT_L1,
                        state, poseStack, collector
                );
                submitFittedText(values[1], width * 0.205F, centerY,
                        width * 0.52F, height * 0.54F,
                        0xFF111111, ROAD_FONT_L1,
                        state, poseStack, collector);
            }
            case M8_VERTICAL_TOP_VALUE -> submitFittedText(
                    value, 0.0F, centerY + height * 0.385F,
                    width * 0.72F, height * 0.16F,
                    0xFF111111, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M8_RIGHT_TOP_VALUE -> submitFittedText(
                    value, -width * 0.205F, centerY + height * 0.335F,
                    width * 0.36F, height * 0.20F,
                    0xFF111111, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M8_LEFT_TOP_VALUE -> submitFittedText(
                    value, width * 0.205F, centerY + height * 0.335F,
                    width * 0.36F, height * 0.20F,
                    0xFF111111, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M8F_DUAL_VALUE -> {
                String[] values = splitCustomText(value);
                submitFittedText(values[0], -width * 0.175F, centerY + height * 0.335F,
                        width * 0.23F, height * 0.20F,
                        0xFF111111, ROAD_FONT_L1, state, poseStack, collector);
                submitFittedText(values[1], width * 0.175F, centerY + height * 0.335F,
                        width * 0.23F, height * 0.20F,
                        0xFF111111, ROAD_FONT_L1, state, poseStack, collector);
            }
            case M10_ROUTE_WHITE -> submitFittedText(
                    value, 0.0F, centerY,
                    width * 0.48F, height * 0.64F,
                    0xFFFFFFFF, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M10_EXIT_NUMBER -> submitFittedText(
                    value, width * 0.148F, centerY,
                    width * 0.27F, height * 0.52F,
                    0xFF111111, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M10_C2_WHITE -> submitFittedText(
                    value, width * 0.169F, centerY,
                    width * 0.47F, height * 0.62F,
                    0xFFFFFFFF, ROAD_FONT_L1,
                    state, poseStack, collector
            );
            case M10Z_TEXT -> submitFittedText(
                    value, 0.0F, centerY,
                    width * 0.84F, height * 0.54F,
                    0xFF111111, ROAD_FONT_L4,
                    state, poseStack, collector
            );
            case CUSTOM_TEXT -> {
                String[] lines = splitCustomText(value);
                if (lines[1].isBlank()) {
                    submitFittedText(
                            lines[0],
                            0.0F,
                            centerY,
                            width * 0.86F,
                            height * 0.48F,
                            state,
                            poseStack,
                            collector
                    );
                } else {
                    submitFittedText(
                            lines[0],
                            0.0F,
                            centerY + height * 0.205F,
                            width * 0.86F,
                            height * 0.29F,
                            state,
                            poseStack,
                            collector
                    );
                    submitFittedText(
                            lines[1],
                            0.0F,
                            centerY - height * 0.205F,
                            width * 0.86F,
                            height * 0.29F,
                            state,
                            poseStack,
                            collector
                    );
                }
            }
        }
    }

    private static void submitFittedText(
            String value,
            float x,
            float y,
            float maxWidth,
            float maxHeight,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitFittedText(value, x, y, maxWidth, maxHeight, 0xFF000000, ROAD_FONT_L1, state, poseStack, collector);
    }

    private static void submitFittedText(
            String value,
            float x,
            float y,
            float maxWidth,
            float maxHeight,
            int color,
            FontDescription.Resource fontResource,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        FormattedCharSequence text = formattedText(value, fontResource);
        int textWidth = font.width(text);
        if (textWidth <= 0) {
            return;
        }

        float widthScale = maxWidth / textWidth;
        float heightScale = maxHeight / font.lineHeight;
        float scale = Math.min(widthScale, heightScale);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(state.facing)));
        poseStack.translate(x, y, TEXT_Z);
        poseStack.scale(scale, -scale, scale);

        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F + TEXT_VERTICAL_BIAS,
                text,
                false,
                Font.DisplayMode.NORMAL,
                state.lightCoords,
                color,
                0x00000000,
                0x00000000
        );
        poseStack.popPose();
    }

    private static void submitWrappedFittedText(
            String value,
            float x,
            float y,
            float maxWidth,
            float maxHeight,
            int maxLines,
            int color,
            FontDescription.Resource fontResource,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        String[] lines = balancedWrap(value, maxLines);
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> sequences = new ArrayList<>();
        int widest = 1;
        for (String line : lines) {
            FormattedCharSequence sequence = formattedText(line, fontResource);
            sequences.add(sequence);
            widest = Math.max(widest, font.width(sequence));
        }
        float lineBlockHeight = font.lineHeight * lines.length;
        float scale = Math.min(maxWidth / widest, maxHeight / Math.max(1.0F, lineBlockHeight));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(state.facing)));
        poseStack.translate(x, y, TEXT_Z);
        poseStack.scale(scale, -scale, scale);
        float firstY = -((lines.length - 1) * font.lineHeight) / 2.0F - font.lineHeight / 2.0F + TEXT_VERTICAL_BIAS;
        for (int i = 0; i < sequences.size(); i++) {
            FormattedCharSequence sequence = sequences.get(i);
            int lineWidth = font.width(sequence);
            collector.submitText(
                    poseStack,
                    -lineWidth / 2.0F,
                    firstY + i * font.lineHeight,
                    sequence,
                    false,
                    Font.DisplayMode.NORMAL,
                    state.lightCoords,
                    color,
                    0x00000000,
                    0x00000000
            );
        }
        poseStack.popPose();
    }

    private static String[] balancedWrap(String value, int maxLines) {
        String cleaned = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (cleaned.isBlank() || maxLines <= 1) {
            return new String[]{cleaned};
        }
        String[] words = cleaned.split(" ");
        int lineCount = Math.min(maxLines, words.length);
        String[] result = new String[lineCount];
        int index = 0;
        int remainingChars = cleaned.length();
        for (int line = 0; line < lineCount; line++) {
            int remainingLines = lineCount - line;
            int target = Math.max(1, remainingChars / remainingLines);
            StringBuilder builder = new StringBuilder();
            while (index < words.length) {
                String candidate = builder.isEmpty() ? words[index] : builder + " " + words[index];
                int wordsLeft = words.length - (index + 1);
                int linesLeft = remainingLines - 1;
                if (!builder.isEmpty() && candidate.length() > target && wordsLeft >= linesLeft) {
                    break;
                }
                builder.append(builder.isEmpty() ? "" : " ").append(words[index]);
                remainingChars -= words[index].length() + (index < words.length - 1 ? 1 : 0);
                index++;
                if (words.length - index == linesLeft) {
                    break;
                }
            }
            result[line] = builder.toString();
        }
        return result;
    }

    private static void orientToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(facing)));
    }

    private static void addFaceQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, z, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, light);
        addVertex(pose, consumer, right, bottom, z, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, light);
        addVertex(pose, consumer, right, top, z, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, light);
        addVertex(pose, consumer, left, top, z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, light);
    }

    private static void addRearFaceQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int light
    ) {
        addVertex(pose, consumer, right, bottom, z, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, light);
        addVertex(pose, consumer, left, bottom, z, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, light);
        addVertex(pose, consumer, left, top, z, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, light);
        addVertex(pose, consumer, right, top, z, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, light);
    }

    private static void addTriangularPrism(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int light
    ) {
        float centerX = (left + right) * 0.5F;

        // Face avant : quad dégénéré pour conserver le mode QUADS du RenderType.
        quad(pose, consumer,
                left, top, front,
                right, top, front,
                centerX, bottom, front,
                centerX, bottom, front,
                0.0F, 0.0F, 1.0F, light);

        // Face arrière.
        quad(pose, consumer,
                right, top, back,
                left, top, back,
                centerX, bottom, back,
                centerX, bottom, back,
                0.0F, 0.0F, -1.0F, light);

        // Chant supérieur.
        quad(pose, consumer,
                left, top, front,
                left, top, back,
                right, top, back,
                right, top, front,
                0.0F, 1.0F, 0.0F, light);

        // Chant oblique gauche.
        quad(pose, consumer,
                left, top, back,
                left, top, front,
                centerX, bottom, front,
                centerX, bottom, back,
                -0.75F, -0.66F, 0.0F, light);

        // Chant oblique droit.
        quad(pose, consumer,
                right, top, front,
                right, top, back,
                centerX, bottom, back,
                centerX, bottom, front,
                0.75F, -0.66F, 0.0F, light);
    }

    private static void addRoundedRectFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float radius,
            float z,
            int light
    ) {
        float width = right - left;
        float height = top - bottom;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        radius = Math.min(radius, Math.min(width, height) * 0.49F);
        if (radius <= 0.001F) {
            addFaceQuad(pose, consumer, left, right, bottom, top, z, light);
            return;
        }

        float innerLeft = left + radius;
        float innerRight = right - radius;
        if (innerRight > innerLeft) {
            addFaceQuad(pose, consumer, innerLeft, innerRight, bottom, top, z, light);
        }

        float sliceWidth = radius / ROUNDED_CORNER_STEPS;
        for (int i = 0; i < ROUNDED_CORNER_STEPS; i++) {
            float outerOffset = i * sliceWidth;
            float innerOffset = (i + 1) * sliceWidth;
            float sampleOffset = (outerOffset + innerOffset) * 0.5F;
            float dx = radius - sampleOffset;
            float inside = Math.max(0.0F, radius * radius - dx * dx);
            float verticalInset = radius - (float) Math.sqrt(inside);
            float sliceBottom = bottom + verticalInset;
            float sliceTop = top - verticalInset;
            if (sliceTop <= sliceBottom) {
                continue;
            }

            addFaceQuad(
                    pose,
                    consumer,
                    left + outerOffset,
                    left + innerOffset,
                    sliceBottom,
                    sliceTop,
                    z,
                    light
            );
            addFaceQuad(
                    pose,
                    consumer,
                    right - innerOffset,
                    right - outerOffset,
                    sliceBottom,
                    sliceTop,
                    z,
                    light
            );
        }
    }

    /**
     * Reproduit dynamiquement la construction du nouveau modèle Blockbench :
     * un grand corps central et une série de petites tranches verticales sur
     * chaque côté. Leur hauteur suit un quart de cercle, ce qui donne des coins
     * réellement arrondis même vus de profil, et pas seulement une transparence
     * dessinée dans la texture.
     */
    private static void addRoundedRectPrism(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int light
    ) {
        float width = right - left;
        float height = top - bottom;

        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        float radius = Math.min(width, height) * ROUNDED_CORNER_RATIO;
        radius = Math.min(radius, Math.min(width, height) * 0.49F);

        if (radius <= 0.001F) {
            addBox(pose, consumer, left, right, bottom, top, back, front, light);
            return;
        }

        float innerLeft = left + radius;
        float innerRight = right - radius;

        // Corps central : même principe que le gros élément principal du modèle.
        if (innerRight > innerLeft) {
            addBox(
                    pose,
                    consumer,
                    innerLeft,
                    innerRight,
                    bottom,
                    top,
                    back,
                    front,
                    light
            );
        }

        float sliceWidth = radius / ROUNDED_CORNER_STEPS;

        for (int i = 0; i < ROUNDED_CORNER_STEPS; i++) {
            float outerOffset = i * sliceWidth;
            float innerOffset = (i + 1) * sliceWidth;
            float sampleOffset = (outerOffset + innerOffset) * 0.5F;

            // Distance du point échantillonné au centre du quart de cercle.
            float dx = radius - sampleOffset;
            float inside = Math.max(0.0F, radius * radius - dx * dx);
            float verticalInset = radius - (float) Math.sqrt(inside);

            float sliceBottom = bottom + verticalInset;
            float sliceTop = top - verticalInset;

            if (sliceTop <= sliceBottom) {
                continue;
            }

            float leftX0 = left + outerOffset;
            float leftX1 = left + innerOffset;
            float rightX0 = right - innerOffset;
            float rightX1 = right - outerOffset;

            addBox(
                    pose,
                    consumer,
                    leftX0,
                    leftX1,
                    sliceBottom,
                    sliceTop,
                    back,
                    front,
                    light
            );

            addBox(
                    pose,
                    consumer,
                    rightX0,
                    rightX1,
                    sliceBottom,
                    sliceTop,
                    back,
                    front,
                    light
            );
        }
    }

    private static void addBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int light
    ) {
        // Face avant (+Z)
        quad(pose, consumer,
                left, bottom, front,
                right, bottom, front,
                right, top, front,
                left, top, front,
                0.0F, 0.0F, 1.0F, light);

        // Face arrière (-Z)
        quad(pose, consumer,
                right, bottom, back,
                left, bottom, back,
                left, top, back,
                right, top, back,
                0.0F, 0.0F, -1.0F, light);

        // Chant gauche (-X)
        quad(pose, consumer,
                left, bottom, back,
                left, bottom, front,
                left, top, front,
                left, top, back,
                -1.0F, 0.0F, 0.0F, light);

        // Chant droit (+X)
        quad(pose, consumer,
                right, bottom, front,
                right, bottom, back,
                right, top, back,
                right, top, front,
                1.0F, 0.0F, 0.0F, light);

        // Dessus (+Y)
        quad(pose, consumer,
                left, top, front,
                right, top, front,
                right, top, back,
                left, top, back,
                0.0F, 1.0F, 0.0F, light);

        // Dessous (-Y)
        quad(pose, consumer,
                left, bottom, back,
                right, bottom, back,
                right, bottom, front,
                left, bottom, front,
                0.0F, -1.0F, 0.0F, light);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz,
            int light
    ) {
        addVertex(pose, consumer, x1, y1, z1, 0.0F, 1.0F, nx, ny, nz, light);
        addVertex(pose, consumer, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, light);
        addVertex(pose, consumer, x3, y3, z3, 1.0F, 0.0F, nx, ny, nz, light);
        addVertex(pose, consumer, x4, y4, z4, 0.0F, 0.0F, nx, ny, nz, light);
    }

    private static void addVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int light
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static FormattedCharSequence formattedText(String value) {
        return formattedText(value, ROAD_FONT_L1);
    }

    private static FormattedCharSequence formattedText(String value, FontDescription.Resource fontResource) {
        return Component.literal(value)
                .withStyle(Style.EMPTY.withFont(fontResource))
                .getVisualOrderText();
    }

    private static Identifier texture(String filename) {
        return Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/block/panonceaux/" + filename
        );
    }

    private static float getFacingRotation(Direction facing) {
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

    private static String[] splitCustomText(String value) {
        String normalized = value == null ? "" : value.replace("\r", "");
        String[] raw = normalized.split("\n", 2);
        String first = raw.length > 0 ? raw[0].strip() : "";
        String second = raw.length > 1 ? raw[1].strip() : "";
        return new String[]{first, second};
    }

    private record PanelLayout(
            PanonceauEntry entry,
            float width,
            float height,
            float bottom,
            float top
    ) {
    }
}
