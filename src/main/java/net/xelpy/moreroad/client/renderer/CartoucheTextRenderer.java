package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.CartoucheLayout;
import net.xelpy.moreroad.block.custom.RoadTextFont;


/**
 * V101 : texte rapproché au maximum de la face du panneau, avec seulement
 * un très léger écart anti-z-fighting pour éviter l'effet de texte flottant.
 *
 * Rendu commun du texte libre affiché sur les cartouches E41 à E47.
 * Le texte est centré sur la face avant et réduit automatiquement quand
 * il devient trop long pour conserver les proportions du cartouche.
 */
public final class CartoucheTextRenderer {

    private static final FontDescription.Resource ROAD_FONT =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    /*
     * V99 : le texte est placé un peu plus franchement devant la face et
     * utilise maintenant un écart de profondeur réel avec le mode NORMAL. Cela évite les zones transparentes autour des glyphes tout en stabilisant le rendu après le
     * recentrage des modèles, sans donner d'effet de texte flottant.
     *
     * V58 : les nouveaux modèles sont géométriquement centrés sur X = 8.
     * Le décalage optique horizontal est donc supprimé : le texte est rendu
     * sur l'axe exact du cartouche.
     *
     * Après les derniers retours en jeu, le texte des cartouches paraît un
     * peu trop bas. V61 le remonte légèrement pour retrouver un centrage
     * visuel plus naturel, tout en conservant le recentrage horizontal
     * optique déjà validé.
     */
    private static final float TEXT_Y_FROM_BOTTOM = 0.132F;
    private static final float TEXT_Z = 0.1400F;
    private static final float CENTER_X_NUDGE = 0.000F;

    private static final float BASE_SCALE = 0.0185F;
    private static final float MAX_WORLD_WIDTH = 0.64F;

    private CartoucheTextRenderer() {
    }

    public static void submit(
            String value,
            CartoucheType type,
            float cartoucheBottomY,
            float cartoucheScale,
            Direction facing,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (type == null || !type.isVisible()) {
            return;
        }

        String cleaned = cleanText(value);

        if (cleaned.isBlank()) {
            return;
        }

        float scaleFactor = cartoucheScale > 0.0F
                ? cartoucheScale
                : 1.0F;

        Font font = Minecraft.getInstance().font;

        Component component =
                Component.literal(cleaned)
                        .withStyle(
                                Style.EMPTY.withFont(ROAD_FONT)
                        );

        FormattedCharSequence text =
                component.getVisualOrderText();

        int textWidth = font.width(text);

        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(
                BASE_SCALE * scaleFactor,
                (MAX_WORLD_WIDTH * scaleFactor) / textWidth
        );

        poseStack.pushPose();

        poseStack.translate(
                0.5F,
                cartoucheBottomY + (TEXT_Y_FROM_BOTTOM * scaleFactor),
                0.5F
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        getFacingRotation(facing)
                )
        );

        /*
         * La profondeur doit suivre la même échelle que le modèle 3D.
         * Avec les grands cartouches du D63c (échelle > MODEL_SCALE),
         * l'ancienne constante plaçait le texte à l'intérieur du modèle :
         * il était donc entièrement masqué par la face colorée.
         */
        float depthScale = scaleFactor / CartoucheLayout.MODEL_SCALE;
        poseStack.translate(
                CENTER_X_NUDGE * scaleFactor,
                0.0F,
                TEXT_Z * depthScale
        );

        RoadGlyphAtlasRenderer.submitCentered(
                cleaned,
                RoadTextFont.L1,
                0.0F,
                0.0F,
                0.0F,
                textWidth * scale,
                getTextColor(type),
                lightCoords,
                poseStack,
                collector,
                -3
        );

        poseStack.popPose();
    }

    /**
     * Texte générique des panneaux D/DA rendu avec le même pipeline que les
     * cartouches. Les translations latérale/avant sont effectuées AVANT le
     * centrage et la rotation, exactement comme submitCartoucheModelScaled +
     * submit(), afin qu'Iris voie la même structure de matrice.
     *
     * panelForward est la translation du centre de la plaque en unités monde.
     * Le décalage Z est volontairement EXACTEMENT TEXT_Z (0,14), comme dans
     * submit() pour les cartouches. C'est plus en avant que la face de la
     * grande plaque (3/32 = 0,09375) et évite que le depth/parallax du shader
     * ne masque les glyphes collés à la surface.
     */
    public static void submitPanelText(
            String value,
            RoadTextFont roadFont,
            int color,
            float lateralWorld,
            float centerYWorld,
            float panelForward,
            float targetWorldWidth,
            Direction facing,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        String cleaned = cleanText(value);
        if (cleaned.isBlank() || targetWorldWidth <= 0.0F) {
            return;
        }

        Direction safeFacing = facing == null ? Direction.NORTH : facing;
        float directionX = safeFacing.getStepX();
        float directionZ = safeFacing.getStepZ();

        float lateralX = switch (safeFacing) {
            case SOUTH -> lateralWorld;
            case NORTH -> -lateralWorld;
            default -> 0.0F;
        };
        float lateralZ = switch (safeFacing) {
            case WEST -> lateralWorld;
            case EAST -> -lateralWorld;
            default -> 0.0F;
        };

        poseStack.pushPose();

        // Même pré-translation monde que celle utilisée par les cartouches.
        poseStack.translate(
                lateralX + directionX * panelForward,
                0.0F,
                lateralZ + directionZ * panelForward
        );

        // Même centre de bloc + rotation horizontale que submit().
        poseStack.translate(0.5F, centerYWorld, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(safeFacing)));

        // Même profondeur post-rotation que les textes de cartouches.
        poseStack.translate(0.0F, 0.0F, TEXT_Z);

        RoadGlyphAtlasRenderer.submitCentered(
                cleaned,
                roadFont == null ? RoadTextFont.L1 : roadFont,
                0.0F,
                0.0F,
                0.0F,
                targetWorldWidth,
                color,
                lightCoords,
                poseStack,
                collector,
                -3
        );

        poseStack.popPose();
    }

    private static float getFacingRotation(Direction facing) {
        if (facing == null) {
            return 180F;
        }

        return switch (facing) {
            case SOUTH -> 0F;
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    private static int getTextColor(CartoucheType type) {
        return switch (type) {
            case E41_45, E42, E47 -> 0xFFFFFFFF;
            case E43, E44 -> 0xFF000000;
            case NONE -> 0xFFFFFFFF;
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
