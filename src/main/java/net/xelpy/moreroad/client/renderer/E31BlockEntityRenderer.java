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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.AbstractE31Block;
import net.xelpy.moreroad.block.entity.E31BlockEntity;

/**
 * V101 : texte rapproché au maximum de la face du panneau, avec seulement
 * un très léger écart anti-z-fighting pour éviter l'effet de texte flottant.
 *
 * Renderer du texte personnalisable des E31a / E31b.
 *
 * V96 : finition verticale du E31b après test en jeu.
 *
 * - le E31a reste strictement inchangé car son placement est validé ;
 * - le texte du E31b redescend très légèrement pour un centrage plus naturel ;
 * - la taille, la largeur disponible et l'alignement horizontal sont conservés.
 *
 * V95 : réajustement fin de la hauteur des textes après test en jeu.
 *
 * - le texte du E31a remonte légèrement ;
 * - le texte du E31b remonte davantage pour mieux se recentrer dans sa plaque ;
 * - la taille, la largeur disponible et l'alignement horizontal sont conservés.
 *
 * V94 : léger ajustement vertical pour mieux recentrer le texte sur les deux
 * panneaux, sans modifier la logique de taille ni l'alignement horizontal.
 *
 * Les deux panneaux utilisent exclusivement la police caractères L4.
 * Le texte reste sur une seule ligne et se réduit proportionnellement si un
 * nom particulièrement long dépasse la largeur disponible.
 */
public class E31BlockEntityRenderer
        implements BlockEntityRenderer<E31BlockEntity, E31RenderState> {

    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "caracteres_l4"
                    )
            );

    /* E31a : plaque X -4 -> 20, Y 10.84 -> 15, face avant Z = 6. */
    private static final float E31A_TEXT_Y = 0.786F;
    /* V99 : stabilisation du texte devant la face + POLYGON_OFFSET. */
    private static final float E31A_TEXT_Z = 0.1925F;
    private static final float E31A_TEXT_X_OFFSET = 0.0F;
    private static final float E31A_BASE_SCALE = 0.0195F;
    private static final float E31A_MAX_WIDTH = 1.30F;

    /*
     * E31b : la vague occupe la partie gauche de la plaque. Le texte est donc
     * volontairement décalé vers la droite et dispose d'une zone plus courte.
     */
    private static final float E31B_TEXT_Y = 0.812F;
    private static final float E31B_TEXT_Z = 0.1625F;
    private static final float E31B_TEXT_X_OFFSET = 0.18F;
    private static final float E31B_BASE_SCALE = 0.0190F;
    private static final float E31B_MAX_WIDTH = 1.04F;

    public E31BlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public E31RenderState createRenderState() {
        return new E31RenderState();
    }

    @Override
    public void extractRenderState(
            E31BlockEntity blockEntity,
            E31RenderState renderState,
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

        renderState.text = blockEntity.getText();

        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof AbstractE31Block e31Block) {
            renderState.facing = state.getValue(AbstractE31Block.FACING);
            renderState.waterName = e31Block.isWaterName();
        }
    }

    @Override
    public void submit(
            E31RenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        String value = cleanText(renderState.text);
        if (value.isBlank()) {
            return;
        }

        float worldY = renderState.waterName
                ? E31B_TEXT_Y
                : E31A_TEXT_Y;
        float textZ = renderState.waterName
                ? E31B_TEXT_Z
                : E31A_TEXT_Z;
        float textXOffset = renderState.waterName
                ? E31B_TEXT_X_OFFSET
                : E31A_TEXT_X_OFFSET;
        float baseScale = renderState.waterName
                ? E31B_BASE_SCALE
                : E31A_BASE_SCALE;
        float maxWidth = renderState.waterName
                ? E31B_MAX_WIDTH
                : E31A_MAX_WIDTH;

        Font font = Minecraft.getInstance().font;
        FormattedCharSequence text = Component.literal(value)
                .withStyle(Style.EMPTY.withFont(ROAD_FONT_L4))
                .getVisualOrderText();

        int textWidth = font.width(text);
        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(
                baseScale,
                maxWidth / textWidth
        );

        poseStack.pushPose();
        poseStack.translate(0.5F, worldY, 0.5F);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(getFacingRotation(renderState.facing))
        );
        poseStack.translate(textXOffset, 0.0F, textZ);
        poseStack.scale(scale, -scale, scale);

        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.NORMAL,
                renderState.lightCoords,
                0xFFFFFFFF,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
    }

    private static float getFacingRotation(net.minecraft.core.Direction facing) {
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
