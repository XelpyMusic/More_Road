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
import net.minecraft.core.Direction;
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
import net.xelpy.moreroad.block.custom.CartoucheModelBlock;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.DA31CArrowType;
import net.xelpy.moreroad.block.custom.DA31CBlock;
import net.xelpy.moreroad.block.entity.DA31CBlockEntity;

/**
 * Renderer DA31C V11.
 *
 * - cartouches rendus depuis les modèles officiels E41/E47 du mod, à taille
 *   normale et sans passer par les multiparts du panneau ;
 * - association type/texte strictement conservée par emplacement ;
 * - deux flèches indépendantes à partir des PNG fournis par l'utilisateur ;
 * - fond bleu opaque très légèrement devant la plaque sous chaque texte afin
 *   d'empêcher définitivement le décor de traverser les pixels anti-aliasés.
 */
public class DA31CBlockEntityRenderer
        implements BlockEntityRenderer<DA31CBlockEntity, DA31CRenderState> {

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    private static final Identifier SOLID_WHITE_TEXTURE =
            texture("da31c_solid_white.png");
    private static final Identifier ARROW_DOWN_TEXTURE =
            texture("panneau_autoroute_fleche_bas.png");
    private static final Identifier ARROW_LEFT_TEXTURE =
            texture("panneau_autoroute_fleche_gauche.png");
    private static final Identifier ARROW_RIGHT_TEXTURE =
            texture("panneau_autoroute_fleche_droite.png");

    private static final int PANEL_BLUE = 0xFF0000FF;

    private static final float TEXT_Z = 0.1115F;
    private static final float ARROW_Z = 0.1080F;
    private static final float CARTOUCHE_TEXT_Z = 0.2050F;

    private static final float LINE_BASE_SCALE = 0.0275F;
    private static final float LINE_MAX_WIDTH = 2.58F;

    /*
     * Dans le repère du renderer, X négatif correspond visuellement à la
     * gauche du panneau. C'est volontairement le même repère pour le modèle
     * ET son texte afin qu'un cartouche rouge A20 ne puisse plus devenir un
     * cartouche vert A20 après rotation.
     */
    private static final float CARTOUCHE_LEFT_X = -0.405F;
    private static final float CARTOUCHE_RIGHT_X = 0.405F;
    private static final float CARTOUCHE_TOP_X = 0.000F;

    private static final float CARTOUCHE_MODEL_SCALE = 1.00F;
    private static final float PANEL_TOP_Y = 26.0F / 16.0F;
    private static final float CARTOUCHE_GAP = 0.035F;
    private static final float CARTOUCHE_HEIGHT = 5.2F / 16.0F;
    private static final float CARTOUCHE_LOWER_BOTTOM_Y = PANEL_TOP_Y + CARTOUCHE_GAP;
    private static final float CARTOUCHE_TOP_BOTTOM_Y =
            CARTOUCHE_LOWER_BOTTOM_Y + CARTOUCHE_HEIGHT + 0.040F;
    private static final float CARTOUCHE_TEXT_Y_FROM_BOTTOM = 0.132F;
    private static final float CARTOUCHE_BASE_SCALE = 0.0185F;
    private static final float CARTOUCHE_MAX_WIDTH = 0.64F;

    private final BlockModelResolver blockResolver;

    public DA31CBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockResolver = context.blockModelResolver();
    }

    @Override
    public DA31CRenderState createRenderState() {
        return new DA31CRenderState();
    }

    @Override
    public void extractRenderState(
            DA31CBlockEntity blockEntity,
            DA31CRenderState renderState,
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

        renderState.line1 = cleanText(blockEntity.getLine1());
        renderState.line2 = cleanText(blockEntity.getLine2());
        renderState.line3 = cleanText(blockEntity.getLine3());
        renderState.line4 = cleanText(blockEntity.getLine4());

        /* La BlockEntity est la source unique des trois couples type + texte. */
        renderState.cartoucheTopType = blockEntity.getCartoucheTopType();
        renderState.cartoucheTopText = cleanText(blockEntity.getCartoucheTopText());
        renderState.cartoucheLeftType = blockEntity.getCartoucheLeftType();
        renderState.cartoucheLeftText = cleanText(blockEntity.getCartoucheLeftText());
        renderState.cartoucheRightType = blockEntity.getCartoucheRightType();
        renderState.cartoucheRightText = cleanText(blockEntity.getCartoucheRightText());
        renderState.arrowLeftType = blockEntity.getArrowLeftType();
        renderState.arrowRightType = blockEntity.getArrowRightType();

        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof DA31CBlock) {
            renderState.facing = state.getValue(DA31CBlock.FACING);
            renderState.lineCount = state.getValue(DA31CBlock.LINE_COUNT);
        } else {
            renderState.facing = Direction.NORTH;
            renderState.lineCount = 2;
        }

        updateCartoucheModel(renderState.cartoucheTopModel, renderState.cartoucheTopType);
        updateCartoucheModel(renderState.cartoucheLeftModel, renderState.cartoucheLeftType);
        updateCartoucheModel(renderState.cartoucheRightModel, renderState.cartoucheRightType);

        BlockState supportState = MoreRoadBlocks.CARTOUCHE_SUPPORT_MODEL.get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        this.blockResolver.update(
                renderState.cartoucheSupportModel,
                supportState,
                BLOCK_DISPLAY_CONTEXT
        );
    }

    private void updateCartoucheModel(
            net.minecraft.client.renderer.block.BlockModelRenderState model,
            CartoucheType type
    ) {
        CartoucheType safeType = type == null ? CartoucheType.NONE : type;
        BlockState state = MoreRoadBlocks.CARTOUCHE_MODEL.get()
                .defaultBlockState()
                /*
                 * Le renderer DA31C travaille avec +Z vers l'avant avant la
                 * rotation globale. Le modèle SOUTH donne donc la même face
                 * avant que le texte et les PNG.
                 */
                .setValue(CartoucheModelBlock.FACING, Direction.SOUTH)
                .setValue(CartoucheModelBlock.TYPE, safeType);
        this.blockResolver.update(model, state, BLOCK_DISPLAY_CONTEXT);
    }

    @Override
    public void submit(
            DA31CRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(renderState.facing)));

        String[] lines = {
                renderState.line1,
                renderState.line2,
                renderState.line3,
                renderState.line4
        };

        int count = Math.max(1, Math.min(4, renderState.lineCount));
        for (int i = 0; i < count; i++) {
            drawPanelLine(
                    collector,
                    poseStack,
                    font,
                    lines[i],
                    getLineY(count, i),
                    renderState.lightCoords
            );
        }

        submitArrow(
                renderState.arrowLeftType,
                true,
                count,
                renderState.lightCoords,
                poseStack,
                collector
        );
        submitArrow(
                renderState.arrowRightType,
                false,
                count,
                renderState.lightCoords,
                poseStack,
                collector
        );

        submitCartouche(
                renderState.cartoucheTopType,
                renderState.cartoucheTopText,
                renderState.cartoucheTopModel,
                CARTOUCHE_TOP_X,
                CARTOUCHE_TOP_BOTTOM_Y,
                renderState.lightCoords,
                poseStack,
                collector,
                font
        );
        submitCartouche(
                renderState.cartoucheLeftType,
                renderState.cartoucheLeftText,
                renderState.cartoucheLeftModel,
                CARTOUCHE_LEFT_X,
                CARTOUCHE_LOWER_BOTTOM_Y,
                renderState.lightCoords,
                poseStack,
                collector,
                font
        );
        submitCartouche(
                renderState.cartoucheRightType,
                renderState.cartoucheRightText,
                renderState.cartoucheRightModel,
                CARTOUCHE_RIGHT_X,
                CARTOUCHE_LOWER_BOTTOM_Y,
                renderState.lightCoords,
                poseStack,
                collector,
                font
        );

        /* Supports : uniquement si le cartouche correspondant existe. */
        if (isVisible(renderState.cartoucheLeftType)) {
            submitSupport(
                    renderState.cartoucheSupportModel,
                    CARTOUCHE_LEFT_X,
                    PANEL_TOP_Y - 0.020F,
                    CARTOUCHE_LOWER_BOTTOM_Y + 0.020F,
                    renderState.lightCoords,
                    poseStack,
                    collector
            );
        }
        if (isVisible(renderState.cartoucheRightType)) {
            submitSupport(
                    renderState.cartoucheSupportModel,
                    CARTOUCHE_RIGHT_X,
                    PANEL_TOP_Y - 0.020F,
                    CARTOUCHE_LOWER_BOTTOM_Y + 0.020F,
                    renderState.lightCoords,
                    poseStack,
                    collector
            );
        }
        if (isVisible(renderState.cartoucheTopType)) {
            submitSupport(
                    renderState.cartoucheSupportModel,
                    CARTOUCHE_TOP_X,
                    PANEL_TOP_Y - 0.020F,
                    CARTOUCHE_TOP_BOTTOM_Y + 0.020F,
                    renderState.lightCoords,
                    poseStack,
                    collector
            );
        }

        poseStack.popPose();
    }

    private static void submitCartouche(
            CartoucheType type,
            String text,
            net.minecraft.client.renderer.block.BlockModelRenderState model,
            float xShift,
            float bottomY,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Font font
    ) {
        if (!isVisible(type)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(xShift, bottomY, 0.0F);
        poseStack.scale(
                CARTOUCHE_MODEL_SCALE,
                CARTOUCHE_MODEL_SCALE,
                CARTOUCHE_MODEL_SCALE
        );
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        model.submit(
                poseStack,
                collector,
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();

        float textY = bottomY + CARTOUCHE_TEXT_Y_FROM_BOTTOM;
        drawTextWithBacking(
                collector,
                poseStack,
                font,
                text,
                xShift,
                textY,
                CARTOUCHE_TEXT_Z,
                CARTOUCHE_BASE_SCALE,
                CARTOUCHE_MAX_WIDTH,
                getCartoucheTextColor(type),
                getCartoucheBackingColor(type),
                lightCoords
        );
    }

    private static void submitSupport(
            net.minecraft.client.renderer.block.BlockModelRenderState model,
            float xShift,
            float bottomY,
            float topY,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        float height = topY - bottomY;
        if (height <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(xShift, bottomY, 0.0F);
        poseStack.scale(1.0F, height, 1.0F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        model.submit(
                poseStack,
                collector,
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    private static void drawPanelLine(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float y,
            int lightCoords
    ) {
        drawTextWithBacking(
                collector,
                poseStack,
                font,
                value,
                0.0F,
                y,
                TEXT_Z,
                LINE_BASE_SCALE,
                LINE_MAX_WIDTH,
                0xFFFFFFFF,
                PANEL_BLUE,
                lightCoords
        );
    }

    /**
     * Le quad opaque placé juste derrière le texte est volontairement limité
     * à l'emprise réelle de la ligne. Même si le RenderType de la police écrit
     * de la profondeur dans les pixels transparents, le pixel visible derrière
     * reste donc toujours le bleu du panneau et jamais le décor du monde.
     */
    private static void drawTextWithBacking(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float x,
            float y,
            float z,
            float baseScale,
            float maxWidth,
            int textColor,
            int backingColor,
            int lightCoords
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        FormattedCharSequence text = Component.literal(value)
                .withStyle(Style.EMPTY.withFont(ROAD_FONT_L1))
                .getVisualOrderText();

        int textWidth = font.width(text);
        if (textWidth <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWidth / textWidth);
        float worldWidth = textWidth * scale;
        float worldHeight = font.lineHeight * scale;
        float marginX = 0.020F;
        float marginY = 0.012F;

        submitColoredRect(
                x - worldWidth / 2.0F - marginX,
                x + worldWidth / 2.0F + marginX,
                y - worldHeight / 2.0F - marginY,
                y + worldHeight / 2.0F + marginY,
                z - 0.006F,
                backingColor,
                lightCoords,
                poseStack,
                collector
        );

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, -scale, scale);
        collector.submitText(
                poseStack,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.NORMAL,
                lightCoords,
                textColor,
                0x00000000,
                0x00000000
        );
        poseStack.popPose();
    }

    private static float getLineY(int count, int index) {
        return switch (count) {
            case 1 -> 1.145F;
            case 2 -> index == 0 ? 1.315F : 0.955F;
            case 3 -> switch (index) {
                case 0 -> 1.330F;
                case 1 -> 1.050F;
                default -> 0.770F;
            };
            default -> switch (index) {
                case 0 -> 1.335F;
                case 1 -> 1.075F;
                case 2 -> 0.815F;
                default -> 0.555F;
            };
        };
    }

    private static void submitArrow(
            DA31CArrowType type,
            boolean leftSlot,
            int lineCount,
            int lightCoords,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (type == null || type == DA31CArrowType.NONE) {
            return;
        }

        Identifier texture = switch (type) {
            case DOWN -> ARROW_DOWN_TEXTURE;
            case LEFT -> ARROW_LEFT_TEXTURE;
            case RIGHT -> ARROW_RIGHT_TEXTURE;
            case NONE -> null;
        };
        if (texture == null) {
            return;
        }

        float panelBottom = (float) (DA31CBlock.getPanelMinY(lineCount) / 16.0D);
        float bottom = panelBottom + 0.055F;

        float height;
        float width;
        if (type == DA31CArrowType.DOWN) {
            height = 0.365F;
            width = height * (472.0F / 315.0F);
        } else {
            height = 0.455F;
            width = height * (344.0F / 392.0F);
        }

        float centerX = leftSlot ? -0.82F : 0.82F;
        submitTexturedRect(
                texture,
                centerX - width / 2.0F,
                centerX + width / 2.0F,
                bottom,
                bottom + height,
                ARROW_Z,
                lightCoords,
                poseStack,
                collector
        );
    }

    private static void submitColoredRect(
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (right <= left || top <= bottom) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        collector.order(-20).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_WHITE_TEXTURE),
                (pose, consumer) -> addQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        bottom,
                        top,
                        color,
                        light
                )
        );
        poseStack.popPose();
    }

    private static void submitTexturedRect(
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int light,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (right <= left || top <= bottom) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        collector.order(-10).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        bottom,
                        top,
                        0xFFFFFFFF,
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
            int color,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, 0.0F, 0.0F, 1.0F, color, light);
        addVertex(pose, consumer, right, bottom, 0.0F, 1.0F, 1.0F, color, light);
        addVertex(pose, consumer, right, top, 0.0F, 1.0F, 0.0F, color, light);
        addVertex(pose, consumer, left, top, 0.0F, 0.0F, 0.0F, color, light);
    }

    private static void addVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color,
            int light
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static boolean isVisible(CartoucheType type) {
        return type != null && type.isVisible();
    }

    private static int getCartoucheTextColor(CartoucheType type) {
        return type == CartoucheType.E43 || type == CartoucheType.E44
                ? 0xFF000000
                : 0xFFFFFFFF;
    }

    private static int getCartoucheBackingColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> 0xFF35B135;
            case E42 -> 0xFFFF0000;
            case E43 -> 0xFFFFE72C;
            case E44 -> 0xFFFEFEFE;
            case E47 -> 0xFF2A7FFF;
            case NONE -> PANEL_BLUE;
        };
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

    private static Identifier texture(String filename) {
        return Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/block/" + filename
        );
    }
}
