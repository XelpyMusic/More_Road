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
import net.xelpy.moreroad.block.custom.D42bBlock;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;

import java.util.EnumSet;

/**
 * Renderer 2D du panneau D42b.
 *
 * V88 : correction finale du rendu du rond et du métrage.
 *
 * - le PNG du rond est désormais rendu sans inversion visuelle (flip X compensé) ;
 * - le texte "150 m" est remonté à une position intermédiaire plus juste ;
 * - le reste des réglages V87 est conservé.
 *
 * V87 : espacement adaptatif des lignes et zones de sécurité pour les
 * noms de villes longs.
 *
 * - sans cartouche coloré, les deux lignes sont beaucoup plus proches ;
 * - avec au moins un cartouche coloré, on conserve l'espacement validé en V86 ;
 * - les zones gauche/droite sont éloignées des flèches ;
 * - les textes longs sont automatiquement réduits pour rester dans leur zone,
 *   cartouche et marges compris.
 *
 * V80 : le diagramme n'utilise plus de BlockModelRenderState pour le rond et
 * les flèches. Les PNG préparés sur le canevas 1500 x 1247 sont dessinés
 * directement sur un quad texturé via SubmitNodeCollector#submitCustomGeometry.
 *
 * Le résultat important est le suivant : la position visible d'une flèche est
 * désormais celle encodée dans SON PNG. Il n'y a plus de rotation, de recadrage
 * ni de repositionnement 3D susceptible de la décaler. En V80 les flèches sont
 * également rendues AU-DESSUS du rond afin que leurs pointes et leurs fûts ne
 * soient plus masqués par l'anneau.
 */
public class D42bBlockEntityRenderer
        implements BlockEntityRenderer<D42bBlockEntity, D42bRenderState> {

    private static final int CANVAS_WIDTH = 1500;
    private static final int CANVAS_HEIGHT = 1247;

    /*
     * Dimensions de la face du D42b V77, déjà agrandie avec ses deux poteaux.
     * Ces valeurs correspondent au modèle réellement utilisé et ne sont plus
     * modifiées par le renderer.
     */
    private static final float PANEL_LEFT = -0.788000F;
    private static final float PANEL_RIGHT = 1.788000F;
    private static final float PANEL_BOTTOM = -0.057782F;
    private static final float PANEL_TOP = 1.993750F;
    private static final float PANEL_WIDTH = PANEL_RIGHT - PANEL_LEFT;
    private static final float PANEL_HEIGHT = PANEL_TOP - PANEL_BOTTOM;

    /*
     * Décalage vertical appliqué uniquement au PNG du rond.
     * Valeur positive = plus bas sur le panneau.
     */
    private static final float CIRCLE_OFFSET_Y_PIXELS = 206.0F;

    /*
     * La face avant du modèle est autour de Z = 0.635. On place les couches
     * dynamiques quelques millièmes devant pour supprimer le z-fighting.
     */
    private static final float FACE_Z_ARROWS = 0.1375F;
    private static final float FACE_Z_CIRCLE = 0.1385F;
    private static final float FACE_Z_BACKGROUND = 0.1395F;
    private static final float FACE_Z_TEXT = 0.1405F;

    private static final int ORDER_CIRCLE = -3;
    private static final int ORDER_ARROWS = -2;
    private static final int ORDER_BACKGROUND = -1;

    private static final Identifier CIRCLE_TEXTURE =
            texture("d42b_cercle_canvas.png");

    /*
     * Les six fichiers ci-dessous sont les PNG préparés par l'utilisateur sur
     * le canevas complet 1500 x 1247. On les affiche sans transformation 2D.
     */
    private static final Identifier ARROW_UP_TEXTURE =
            texture("d42b_fleche_droite.png");
    private static final Identifier ARROW_UP_RIGHT_TEXTURE =
            texture("d42b_fleche45.png");
    private static final Identifier ARROW_UP_LEFT_TEXTURE =
            texture("d42b_fleche45_bis.png");
    private static final Identifier ARROW_RIGHT_TEXTURE =
            texture("d42b_fleche90.png");
    private static final Identifier ARROW_LEFT_TEXTURE =
            texture("d42b_fleche90_bis.png");
    private static final Identifier ARROW_DOWN_LEFT_TEXTURE =
            texture("d42b_fleche135.png");
    private static final Identifier ARROW_DOWN_RIGHT_TEXTURE =
            texture("d42b_fleche135_bis.png");

    private static final Identifier GREEN_TEXTURE =
            texture("d42b_green.png");
    private static final Identifier BLUE_TEXTURE =
            texture("d42b_blue.png");

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    private static final float TEXT_BASE_SCALE = 0.0109F;
    private static final float TEXT_PADDING_X = 0.032F;
    private static final float TEXT_PADDING_Y = 0.018F;
    private static final int PLAIN_LINE_SPACING_PIXELS = 64;
    private static final int COLORED_LINE_SPACING_PIXELS = 84;

    private static final float DISTANCE_BASE_SCALE = 0.0144F;
    private static final float DISTANCE_MAX_WIDTH = 0.42F;

    private enum TextAnchor {
        LEFT,
        CENTER,
        RIGHT
    }

    private enum ArrowVariant {
        UP,
        UP_RIGHT,
        UP_LEFT,
        RIGHT,
        LEFT,
        DOWN_RIGHT,
        DOWN_LEFT
    }

    /**
     * x/y sont exprimés dans le même repère 1500 x 1247 que les PNG.
     * maxWidthPixels fixe la zone disponible pour le texte et les cartouches.
     */
    private record LabelZone(
            TextAnchor anchor,
            float x,
            float line1Y,
            float maxWidthPixels
    ) {
        float line2Y(float spacingPixels) {
            return this.line1Y + spacingPixels;
        }
    }

    public D42bBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public D42bRenderState createRenderState() {
        return new D42bRenderState();
    }

    @Override
    public void extractRenderState(
            D42bBlockEntity blockEntity,
            D42bRenderState renderState,
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
        renderState.facing = state.getValue(D42bBlock.FACING);
        renderState.distanceText = blockEntity.getDistanceText();

        for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
            renderState.branches[i] = blockEntity.getBranch(i);
        }
    }

    @Override
    public void submit(
            D42bRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        /*
         * Le rond est rendu en premier. Les PNG de flèches préparés par
         * l'utilisateur contiennent déjà leur longueur et leur raccord exact
         * avec le giratoire. Ils doivent donc passer DEVANT le rond : sinon
         * l'anneau masque les pointes et une grande partie des diagonales.
         */
        submitShiftedCanvasTextureFlippedX(
                CIRCLE_TEXTURE,
                0.0F,
                CIRCLE_OFFSET_Y_PIXELS,
                FACE_Z_CIRCLE,
                ORDER_CIRCLE,
                renderState,
                poseStack,
                collector
        );

        /*
         * On n'affiche qu'une fois chaque direction graphique. Cela évite de
         * superposer inutilement exactement le même PNG si deux entrées de
         * configuration utilisent la même famille d'angle.
         */
        EnumSet<ArrowVariant> renderedArrows = EnumSet.noneOf(ArrowVariant.class);

        for (D42bBranchData branch : renderState.branches) {
            if (branch == null || !branch.enabled()) {
                continue;
            }

            ArrowVariant variant = getArrowVariant(
                    normalizedAngle(branch.angleDegrees())
            );

            if (renderedArrows.add(variant)) {
                submitCanvasTexture(
                        textureForArrow(variant),
                        FACE_Z_ARROWS,
                        ORDER_ARROWS,
                        renderState,
                        poseStack,
                        collector
                );
            }
        }

        for (D42bBranchData branch : renderState.branches) {
            if (branch == null || !branch.enabled()) {
                continue;
            }

            ArrowVariant variant = getArrowVariant(
                    normalizedAngle(branch.angleDegrees())
            );

            submitBranchLabels(
                    branch,
                    variant,
                    renderState,
                    poseStack,
                    collector
            );
        }

        submitDistance(
                renderState.distanceText,
                renderState,
                poseStack,
                collector
        );
    }

    private static void submitBranchLabels(
            D42bBranchData branch,
            ArrowVariant variant,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        String line1 = cleanText(branch.line1());
        String line2 = cleanText(branch.line2());

        if (line1.isBlank() && line2.isBlank()) {
            return;
        }

        LabelZone zone = zoneFor(variant);
        float lineSpacingPixels = lineSpacingFor(branch, line1, line2);

        if (!line1.isBlank()) {
            submitLabelLine(
                    line1,
                    branch.line1Font(),
                    branch.line1Color(),
                    zone.x(),
                    zone.line1Y(),
                    zone.maxWidthPixels(),
                    zone.anchor(),
                    state,
                    poseStack,
                    collector
            );
        }

        if (!line2.isBlank()) {
            submitLabelLine(
                    line2,
                    branch.line2Font(),
                    branch.line2Color(),
                    zone.x(),
                    line1.isBlank() ? zone.line1Y() : zone.line2Y(lineSpacingPixels),
                    zone.maxWidthPixels(),
                    zone.anchor(),
                    state,
                    poseStack,
                    collector
            );
        }
    }

    private static float lineSpacingFor(
            D42bBranchData branch,
            String line1,
            String line2
    ) {
        if (line1.isBlank() || line2.isBlank()) {
            return 0.0F;
        }

        boolean line1Colored =
                branch.line1Color() != null
                        && branch.line1Color() != D42bLabelColor.NONE;
        boolean line2Colored =
                branch.line2Color() != null
                        && branch.line2Color() != D42bLabelColor.NONE;

        return line1Colored || line2Colored
                ? COLORED_LINE_SPACING_PIXELS
                : PLAIN_LINE_SPACING_PIXELS;
    }

    private static LabelZone zoneFor(ArrowVariant variant) {
        return switch (variant) {
            /* Tout droit : texte recentré et légèrement remonté. */
            case UP -> new LabelZone(
                    TextAnchor.CENTER,
                    750.0F,
                    136.0F,
                    520.0F
            );

            /* Haut-gauche : déplacé nettement vers le coin haut-gauche. */
            case UP_LEFT -> new LabelZone(
                    TextAnchor.RIGHT,
                    445.0F,
                    265.0F,
                    420.0F
            );

            /* Haut-droite : déplacé vers le coin haut-droit. */
            case UP_RIGHT -> new LabelZone(
                    TextAnchor.LEFT,
                    1070.0F,
                    265.0F,
                    420.0F
            );

            /* Gauche : plus loin du rond et un peu plus bas. */
            case LEFT -> new LabelZone(
                    TextAnchor.RIGHT,
                    350.0F,
                    560.0F,
                    330.0F
            );

            /* Droite : plus loin du rond et remonté pour dégager la flèche. */
            case RIGHT -> new LabelZone(
                    TextAnchor.LEFT,
                    1150.0F,
                    520.0F,
                    330.0F
            );

            /* Bas-gauche : zone gardée large et plus basse. */
            case DOWN_LEFT -> new LabelZone(
                    TextAnchor.RIGHT,
                    390.0F,
                    900.0F,
                    400.0F
            );

            /* Bas-droite : déplacé à droite et légèrement vers le bas. */
            case DOWN_RIGHT -> new LabelZone(
                    TextAnchor.LEFT,
                    1100.0F,
                    905.0F,
                    370.0F
            );
        };
    }

    private static void submitLabelLine(
            String value,
            RoadTextFont textFont,
            D42bLabelColor background,
            float anchorCanvasX,
            float centerCanvasY,
            float maxWidthPixels,
            TextAnchor anchor,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        Font font = Minecraft.getInstance().font;
        FormattedCharSequence text = formattedText(value, textFont);
        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float maxWorldWidth = canvasWidth(maxWidthPixels);
        float horizontalPadding =
                background != null && background != D42bLabelColor.NONE
                        ? TEXT_PADDING_X * 2.0F
                        : 0.0F;
        float availableTextWidth = Math.max(
                0.01F,
                maxWorldWidth - horizontalPadding
        );
        float scale = Math.min(
                TEXT_BASE_SCALE,
                availableTextWidth / textWidth
        );

        float worldWidth = textWidth * scale;
        float worldHeight = font.lineHeight * scale;

        float x = canvasX(anchorCanvasX);
        float y = canvasY(centerCanvasY);
        float textY = y;

        float effectiveWidth = worldWidth;
        float effectiveHeight = worldHeight;

        if (background != null && background != D42bLabelColor.NONE) {
            effectiveWidth += TEXT_PADDING_X * 2.0F;
            effectiveHeight += TEXT_PADDING_Y * 2.0F;
        }

        x = clampAnchorX(x, anchor, effectiveWidth);
        y = clampY(y, effectiveHeight);

        TextAnchor textAnchor = anchor;
        float textX = x;

        if (background != null && background != D42bLabelColor.NONE) {
            Identifier texture = background == D42bLabelColor.BLUE
                    ? BLUE_TEXTURE
                    : GREEN_TEXTURE;

            float backgroundWidth = worldWidth + TEXT_PADDING_X * 2.0F;
            float backgroundHeight = worldHeight + TEXT_PADDING_Y * 2.0F;

            float centerX = switch (anchor) {
                case LEFT -> x + backgroundWidth / 2.0F;
                case CENTER -> x;
                case RIGHT -> x - backgroundWidth / 2.0F;
            };

            submitTexturedRect(
                    texture,
                    centerX - backgroundWidth / 2.0F,
                    centerX + backgroundWidth / 2.0F,
                    y - backgroundHeight / 2.0F,
                    y + backgroundHeight / 2.0F,
                    FACE_Z_BACKGROUND,
                    ORDER_BACKGROUND,
                    state,
                    poseStack,
                    collector
            );

            /* Sur cartouche coloré, le texte doit être centré dans le fond,
             * avec un léger décalage vers le bas pour un rendu plus juste. */
            textAnchor = TextAnchor.CENTER;
            textX = centerX;
            textY = y - canvasHeight(8.0F);
        }

        int color = background == null || background == D42bLabelColor.NONE
                ? 0xFF000000
                : 0xFFFFFFFF;

        submitText(
                text,
                textWidth,
                scale,
                textX,
                textY,
                textAnchor,
                color,
                state,
                poseStack,
                collector
        );
    }

    private static void submitDistance(
            String value,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        String cleaned = cleanText(value);
        if (cleaned.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        FormattedCharSequence text = formattedText(cleaned, RoadTextFont.L4);
        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(
                DISTANCE_BASE_SCALE,
                DISTANCE_MAX_WIDTH / textWidth
        );

        /* Centre de la case métrage de la texture 1500 x 1247. */
        float x = canvasX(145.0F);
        float y = canvasY(1175.0F);

        submitText(
                text,
                textWidth,
                scale,
                x,
                y,
                TextAnchor.CENTER,
                0xFF000000,
                state,
                poseStack,
                collector
        );
    }

    private static void submitText(
            FormattedCharSequence text,
            int textWidth,
            float scale,
            float x,
            float y,
            TextAnchor anchor,
            int color,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5F, y, 0.5F);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(getFacingRotation(state.facing))
        );
        poseStack.translate(x - 0.5F, 0.0F, FACE_Z_TEXT);
        poseStack.scale(scale, -scale, scale);

        float textX = switch (anchor) {
            case LEFT -> 0.0F;
            case CENTER -> -textWidth / 2.0F;
            case RIGHT -> -textWidth;
        };

        collector.submitText(
                poseStack,
                textX,
                -font.lineHeight / 2.0F,
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

    /**
     * Affiche une texture 1500 x 1247 exactement sur toute la face du D42b.
     */
    private static void submitCanvasTexture(
            Identifier texture,
            float z,
            int order,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitTexturedRect(
                texture,
                PANEL_LEFT,
                PANEL_RIGHT,
                PANEL_BOTTOM,
                PANEL_TOP,
                z,
                order,
                state,
                poseStack,
                collector
        );
    }

    private static void submitShiftedCanvasTexture(
            Identifier texture,
            float offsetXPixels,
            float offsetYPixels,
            float z,
            int order,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float dx = canvasWidth(offsetXPixels);
        float dy = -(offsetYPixels / CANVAS_HEIGHT) * PANEL_HEIGHT;

        submitTexturedRect(
                texture,
                PANEL_LEFT + dx,
                PANEL_RIGHT + dx,
                PANEL_BOTTOM + dy,
                PANEL_TOP + dy,
                z,
                order,
                state,
                poseStack,
                collector
        );
    }

    private static void submitShiftedCanvasTextureFlippedX(
            Identifier texture,
            float offsetXPixels,
            float offsetYPixels,
            float z,
            int order,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float dx = canvasWidth(offsetXPixels);
        float dy = -(offsetYPixels / CANVAS_HEIGHT) * PANEL_HEIGHT;

        submitTexturedRectFlippedX(
                texture,
                PANEL_LEFT + dx,
                PANEL_RIGHT + dx,
                PANEL_BOTTOM + dy,
                PANEL_TOP + dy,
                z,
                order,
                state,
                poseStack,
                collector
        );
    }

    /**
     * Quad texturé 2D utilisé à la fois pour le diagramme et les cartouches.
     * Le RenderType entityCutout respecte l'alpha des PNG : les zones
     * transparentes ne masquent donc jamais la texture blanche du panneau.
     */
    private static void submitTexturedRect(
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int order,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (right <= left || top <= bottom) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(getFacingRotation(state.facing))
        );
        poseStack.translate(-0.5F, 0.0F, z);

        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        bottom,
                        top,
                        light
                )
        );

        poseStack.popPose();
    }

    private static void submitTexturedRectFlippedX(
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int order,
            D42bRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (right <= left || top <= bottom) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(getFacingRotation(state.facing))
        );
        poseStack.translate(-0.5F, 0.0F, z);

        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addQuadFlippedX(
                        pose,
                        consumer,
                        left,
                        right,
                        bottom,
                        top,
                        light
                )
        );

        poseStack.popPose();
    }

    private static void addQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, 0.0F, 0.0F, 1.0F, light);
        addVertex(pose, consumer, right, bottom, 0.0F, 1.0F, 1.0F, light);
        addVertex(pose, consumer, right, top, 0.0F, 1.0F, 0.0F, light);
        addVertex(pose, consumer, left, top, 0.0F, 0.0F, 0.0F, light);
    }

    private static void addQuadFlippedX(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, 0.0F, 1.0F, 1.0F, light);
        addVertex(pose, consumer, right, bottom, 0.0F, 0.0F, 1.0F, light);
        addVertex(pose, consumer, right, top, 0.0F, 0.0F, 0.0F, light);
        addVertex(pose, consumer, left, top, 0.0F, 1.0F, 0.0F, light);
    }

    private static void addVertex(
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

    private static FormattedCharSequence formattedText(
            String value,
            RoadTextFont font
    ) {
        FontDescription.Resource resource = font == RoadTextFont.L4
                ? ROAD_FONT_L4
                : ROAD_FONT_L1;

        return Component.literal(value)
                .withStyle(Style.EMPTY.withFont(resource))
                .getVisualOrderText();
    }

    private static Identifier texture(String filename) {
        return Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/block/" + filename
        );
    }

    private static Identifier textureForArrow(ArrowVariant variant) {
        return switch (variant) {
            case UP -> ARROW_UP_TEXTURE;
            case UP_RIGHT -> ARROW_UP_RIGHT_TEXTURE;
            case UP_LEFT -> ARROW_UP_LEFT_TEXTURE;
            case RIGHT -> ARROW_RIGHT_TEXTURE;
            case LEFT -> ARROW_LEFT_TEXTURE;
            case DOWN_RIGHT -> ARROW_DOWN_RIGHT_TEXTURE;
            case DOWN_LEFT -> ARROW_DOWN_LEFT_TEXTURE;
        };
    }

    private static ArrowVariant getArrowVariant(float angle) {
        if (angle >= -22.5F && angle <= 22.5F) {
            return ArrowVariant.UP;
        }
        if (angle > 22.5F && angle < 67.5F) {
            return ArrowVariant.UP_RIGHT;
        }
        if (angle >= 67.5F && angle <= 112.5F) {
            return ArrowVariant.RIGHT;
        }
        if (angle > 112.5F) {
            return ArrowVariant.DOWN_RIGHT;
        }
        if (angle < -22.5F && angle > -67.5F) {
            return ArrowVariant.UP_LEFT;
        }
        if (angle <= -67.5F && angle >= -112.5F) {
            return ArrowVariant.LEFT;
        }
        return ArrowVariant.DOWN_LEFT;
    }

    private static float canvasX(float pixelX) {
        return PANEL_LEFT + (pixelX / CANVAS_WIDTH) * PANEL_WIDTH;
    }

    private static float canvasY(float pixelY) {
        return PANEL_TOP - (pixelY / CANVAS_HEIGHT) * PANEL_HEIGHT;
    }

    private static float canvasWidth(float pixels) {
        return (pixels / CANVAS_WIDTH) * PANEL_WIDTH;
    }

    private static float canvasHeight(float pixels) {
        return (pixels / CANVAS_HEIGHT) * PANEL_HEIGHT;
    }

    private static float clampAnchorX(
            float x,
            TextAnchor anchor,
            float width
    ) {
        return switch (anchor) {
            case LEFT -> clamp(
                    x,
                    PANEL_LEFT + TEXT_PADDING_X,
                    PANEL_RIGHT - width - TEXT_PADDING_X
            );
            case CENTER -> clamp(
                    x,
                    PANEL_LEFT + width / 2.0F + TEXT_PADDING_X,
                    PANEL_RIGHT - width / 2.0F - TEXT_PADDING_X
            );
            case RIGHT -> clamp(
                    x,
                    PANEL_LEFT + width + TEXT_PADDING_X,
                    PANEL_RIGHT - TEXT_PADDING_X
            );
        };
    }

    private static float clampY(float y, float height) {
        return clamp(
                y,
                PANEL_BOTTOM + height / 2.0F + TEXT_PADDING_Y,
                PANEL_TOP - height / 2.0F - TEXT_PADDING_Y
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float normalizedAngle(float angle) {
        float normalized = angle % 360.0F;

        if (normalized <= -180.0F) {
            normalized += 360.0F;
        }
        if (normalized > 180.0F) {
            normalized -= 360.0F;
        }

        return normalized;
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
}
