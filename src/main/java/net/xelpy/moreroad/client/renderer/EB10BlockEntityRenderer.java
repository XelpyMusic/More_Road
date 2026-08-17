package net.xelpy.moreroad.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.EB10Block;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;

import java.util.Locale;

public class EB10BlockEntityRenderer
        implements BlockEntityRenderer<EB10BlockEntity, EB10RenderState> {

    /*
     * ============================================================
     * POLICE L1
     * ============================================================
     */

    private static final FontDescription.Resource ROAD_FONT =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l1"
                    )
            );

    /*
     * ============================================================
     * POSITION GÉNÉRALE
     * ============================================================
     */

    private static final float TEXT_X_OFFSET = 0.025F;

    // Distance devant la face du panneau
    private static final float TEXT_Z = 0.066F;

    /*
     * ============================================================
     * UNE SEULE LIGNE
     * ============================================================
     */

    // Position que nous avions réglée pour NICE / TOULOUSE
    private static final float SINGLE_LINE_Y = 0.748F;

    private static final float SINGLE_LINE_BASE_SCALE = 0.0130F;

    /*
     * ============================================================
     * DEUX LIGNES
     * ============================================================
     *
     * Les deux lignes ont chacune leur propre position dans
     * le monde. Elles ne peuvent donc plus se chevaucher.
     */

    private static final float FIRST_LINE_Y = 0.795F;
    private static final float SECOND_LINE_Y = 0.680F;

    private static final float TWO_LINES_BASE_SCALE = 0.0100F;

    /*
     * ============================================================
     * LARGEUR MAXIMALE
     * ============================================================
     *
     * Même si l'utilisateur tape une ligne très longue,
     * elle sera réduite juste assez pour ne pas sortir du panneau.
     */

    private static final float MAX_TEXT_WORLD_WIDTH = 0.80F;


    public EB10BlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }


    /*
     * ============================================================
     * RENDER STATE
     * ============================================================
     */

    @Override
    public EB10RenderState createRenderState() {
        return new EB10RenderState();
    }


    /*
     * ============================================================
     * RÉCUPÉRATION DES DONNÉES
     * ============================================================
     */

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

        /*
         * Nouveau système :
         * deux lignes indépendantes.
         */
        renderState.line1 = blockEntity.getLine1();
        renderState.line2 = blockEntity.getLine2();

        renderState.facing = blockEntity
                .getBlockState()
                .getValue(EB10Block.FACING);
    }


    /*
     * ============================================================
     * RENDU
     * ============================================================
     */

    @Override
    public void submit(
            EB10RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {

        String line1 = cleanText(renderState.line1);
        String line2 = cleanText(renderState.line2);

        /*
         * Aucun texte.
         */
        if (line1.isBlank() && line2.isBlank()) {
            return;
        }

        /*
         * --------------------------------------------------------
         * UNE SEULE LIGNE
         * --------------------------------------------------------
         *
         * Si la deuxième ligne est vide, on garde exactement le
         * comportement prévu pour NICE, TOULOUSE, ARCACHON...
         */
        if (line2.isBlank()) {

            submitLine(
                    line1,
                    SINGLE_LINE_Y,
                    SINGLE_LINE_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );

            return;
        }

        /*
         * --------------------------------------------------------
         * DEUX LIGNES
         * --------------------------------------------------------
         *
         * L'utilisateur décide lui-même de la coupure.
         *
         * Exemple :
         *
         * Ligne 1 : SAINT-RÉMY-
         * Ligne 2 : DE-PROVENCE
         *
         * Les tirets sont conservés exactement.
         */

        if (!line1.isBlank()) {
            submitLine(
                    line1,
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
                    SECOND_LINE_Y,
                    TWO_LINES_BASE_SCALE,
                    renderState,
                    poseStack,
                    collector
            );
        }
    }


    /*
     * ============================================================
     * RENDU D'UNE LIGNE
     * ============================================================
     */

    private static void submitLine(
            String value,
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


        /*
         * Police Caractères L1.
         */
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


        /*
         * --------------------------------------------------------
         * REDIMENSIONNEMENT AUTOMATIQUE
         * --------------------------------------------------------
         *
         * Une ligne normale garde sa taille de base.
         *
         * Si elle devient trop longue, on la réduit juste assez
         * pour respecter MAX_TEXT_WORLD_WIDTH.
         */

        float scale = Math.min(
                baseScale,
                MAX_TEXT_WORLD_WIDTH / textWidth
        );


        poseStack.pushPose();


        /*
         * Centre du bloc.
         */
        poseStack.translate(
                0.5F,
                worldY,
                0.5F
        );


        /*
         * Orientation du panneau.
         */
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


        /*
         * Position sur la face.
         */
        poseStack.translate(
                TEXT_X_OFFSET,
                0F,
                TEXT_Z
        );


        /*
         * Taille.
         */
        poseStack.scale(
                scale,
                -scale,
                scale
        );


        /*
         * Centrage horizontal et vertical.
         */
        float textX =
                -textWidth / 2.0F;

        float textY =
                -font.lineHeight / 2.0F;


        /*
         * Rendu.
         */
        collector.submitText(
                poseStack,

                textX,
                textY,

                text,

                false,

                Font.DisplayMode.NORMAL,

                renderState.lightCoords,

                // Noir
                0xFF000000,

                // Aucun fond
                0x00000000,

                // Aucun contour
                0x00000000
        );


        poseStack.popPose();
    }


    /*
     * ============================================================
     * NETTOYAGE
     * ============================================================
     */

    private static String cleanText(String text) {

        if (text == null) {
            return "";
        }

        /*
         * On met uniquement en majuscules.
         *
         * IMPORTANT :
         * aucun tiret n'est supprimé.
         */
        return text
                .strip()
                .toUpperCase(Locale.ROOT);
    }
}