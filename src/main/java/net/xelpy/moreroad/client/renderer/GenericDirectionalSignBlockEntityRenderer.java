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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.GenericArrowShape;
import net.xelpy.moreroad.block.custom.GenericDestinationRow;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignBlock;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignGeometry;
import net.xelpy.moreroad.block.custom.GenericRouteCartoucheData;
import net.xelpy.moreroad.block.custom.GenericSignHeader;
import net.xelpy.moreroad.block.custom.GenericSignSymbol;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.GenericDirectionalSignBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer du panneau directionnel modulable générique.
 *
 * Volontairement data-driven : aucune branche par préréglage — tout vient de
 * {@link GenericDirectionalSignData} et {@link GenericDirectionalSignGeometry}.
 * Réutilise les primitives déjà éprouvées de {@link MotorwaySignBlockEntityRenderer}
 * (corps arrondi/listel avec le même rayon de coin que le vrai D31b, texte
 * tracké compatible shaders, support DA31C partagé par tout le mod) plutôt
 * que de les dupliquer — voir les méthodes à visibilité élargie dans cette
 * classe.
 */
public class GenericDirectionalSignBlockEntityRenderer
        implements BlockEntityRenderer<GenericDirectionalSignBlockEntity, GenericDirectionalSignRenderState> {

    private static final float WORLD_SCALE = GenericDirectionalSignGeometry.WORLD_SCALE;
    private static final float PANEL_HALF_DEPTH = (3.0F / 16.0F) / WORLD_SCALE / 2.0F;
    private static final float FRONT_Z = PANEL_HALF_DEPTH;
    private static final float TEXT_Z = FRONT_Z + 0.020F;
    private static final float ICON_Z = FRONT_Z + 0.006F;
    private static final float BASE_TEXT_SCALE = 0.050F;
    /* Même rayon de coin que le vrai D31b (voir MotorwaySignBlockEntityRenderer), pas une valeur générique. */
    private static final float CORNER_RADIUS = MotorwaySignBlockEntityRenderer.TEXTURED_BODY_CORNER_RADIUS_D31B;
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockResolver;

    public GenericDirectionalSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockResolver = context.blockModelResolver();
    }

    @Override
    public GenericDirectionalSignRenderState createRenderState() {
        return new GenericDirectionalSignRenderState();
    }

    @Override
    public AABB getRenderBoundingBox(GenericDirectionalSignBlockEntity blockEntity) {
        GenericDirectionalSignGeometry geometry = GenericDirectionalSignGeometry.forData(blockEntity.getData());
        double horizontalHalfSize = geometry.width() / 2.0 + 0.60;
        double top = geometry.panelBottom() + geometry.height() + 0.30;
        double centerX = blockEntity.getBlockPos().getX() + 0.5;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5;
        return new AABB(
                centerX - horizontalHalfSize, blockEntity.getBlockPos().getY() - 0.20, centerZ - horizontalHalfSize,
                centerX + horizontalHalfSize, blockEntity.getBlockPos().getY() + top, centerZ + horizontalHalfSize
        );
    }

    @Override
    public void extractRenderState(
            GenericDirectionalSignBlockEntity blockEntity,
            GenericDirectionalSignRenderState renderState,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);
        renderState.data = blockEntity.getData();
        BlockState state = blockEntity.getBlockState();
        renderState.facing = state.getBlock() instanceof GenericDirectionalSignBlock
                ? state.getValue(GenericDirectionalSignBlock.FACING)
                : Direction.NORTH;

        /* Même poteau que D31b/D61B/MotorwaySign : le vrai support du mod, pas une boîte procédurale. */
        BlockState poleState = MoreRoadBlocks.SUPPORT_DA31C_POTEAU.get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, renderState.facing);
        this.blockResolver.update(renderState.poleModel, poleState, DISPLAY_CONTEXT);
    }

    @Override
    public void submit(
            GenericDirectionalSignRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        Font font = Minecraft.getInstance().font;
        GenericDirectionalSignData data = state.data == null ? GenericDirectionalSignData.blank() : state.data;
        GenericDirectionalSignGeometry geometry = GenericDirectionalSignGeometry.forData(data);
        int light = state.lightCoords;

        submitPole(state, geometry.supportTop(), poseStack, collector, light);

        List<DeferredText> deferredTexts = new ArrayList<>();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(MotorwaySignBlockEntityRenderer.getFacingRotation(state.facing)));

        poseStack.pushPose();
        poseStack.scale(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        drawPanel(collector, poseStack, font, data, geometry, light, deferredTexts);
        poseStack.popPose();

        for (DeferredText text : deferredTexts) {
            submitDeferredText(collector, poseStack, font, text);
        }

        poseStack.popPose();
    }

    /** Même modèle et même étirement que le poteau DA31C partagé par le reste du mod (voir submitD61CentralSupport). */
    private static void submitPole(
            GenericDirectionalSignRenderState state,
            float supportTop,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(1.004F, supportTop, 1.004F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        state.poleModel.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private void drawPanel(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            GenericDirectionalSignData data,
            GenericDirectionalSignGeometry geometry,
            int light,
            List<DeferredText> deferredTexts
    ) {
        float width = geometry.designWidth();
        float left = -width / 2.0F;
        float right = width / 2.0F;
        float panelBottom = 2.05F;

        float rowsHeight = geometry.enabledRowCount() > 0 ? 0.46F + geometry.lineStep() * geometry.enabledRowCount() : 0.0F;
        boolean hasRowsBand = geometry.enabledRowCount() > 0;
        if (!hasRowsBand && !geometry.headerEnabled()) {
            /* Panneau vierge : un seul petit corps vide, prêt à accueillir du contenu. */
            rowsHeight = 0.46F + geometry.lineStep();
            hasRowsBand = true;
        }

        float rowsTop = panelBottom + rowsHeight;
        float headerBottom = hasRowsBand ? rowsTop + GenericDirectionalSignGeometry.PANEL_GAP : panelBottom;
        float headerTop = headerBottom + (geometry.headerEnabled() ? GenericDirectionalSignGeometry.HEADER_HEIGHT : 0.0F);
        float cartoucheBottomAnchor = geometry.headerEnabled() ? headerTop : (hasRowsBand ? rowsTop : panelBottom);

        if (hasRowsBand) {
            MotorwaySignBlockEntityRenderer.drawPlate(collector, poseStack, left, right, panelBottom, rowsTop, data.background(), light, CORNER_RADIUS);
            drawRows(collector, poseStack, font, data, left, right, panelBottom, rowsTop, geometry.lineStep(), light, deferredTexts);
        }

        if (geometry.headerEnabled()) {
            drawHeader(collector, poseStack, font, data, headerBottom, headerTop, left, right, light, deferredTexts);
        }

        if (geometry.visibleCartoucheCount() > 0) {
            drawCartouches(collector, poseStack, font, data.cartouches(), left, right, cartoucheBottomAnchor, light, deferredTexts);
        }
    }

    private void drawHeader(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            GenericDirectionalSignData data,
            float bottom,
            float top,
            float panelLeft,
            float panelRight,
            int light,
            List<DeferredText> deferredTexts
    ) {
        GenericSignHeader header = data.header();
        MotorwaySignColor color = header.effectiveColor(data.background());
        MotorwaySignBlockEntityRenderer.drawPlate(collector, poseStack, panelLeft, panelRight, bottom, top, color, light, CORNER_RADIUS);

        if (header.text().isBlank()) {
            return;
        }
        float margin = GenericDirectionalSignGeometry.ROW_MARGIN;
        float textLeft = panelLeft + margin;
        float textRight = panelRight - margin;
        float availableWidth = Math.max(0.20F, textRight - textLeft);
        float textWidth = MotorwaySignBlockEntityRenderer.trackedTextWidth(font, header.font(), header.text());
        float scale = textWidth <= 0.0F ? BASE_TEXT_SCALE : Math.min(BASE_TEXT_SCALE, availableWidth / textWidth);
        float centerY = (bottom + top) / 2.0F;
        float centerX = switch (header.alignment()) {
            case LEFT -> textLeft + textWidth * scale / 2.0F;
            case RIGHT -> textRight - textWidth * scale / 2.0F;
            case CENTER -> (textLeft + textRight) / 2.0F;
        };
        deferredTexts.add(new DeferredText(header.text(), centerX, centerY, header.font(), color.getTextArgb(), scale, light));
    }

    private void drawCartouches(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            GenericRouteCartoucheData[] cartouches,
            float panelLeft,
            float panelRight,
            float anchorTop,
            int light,
            List<DeferredText> deferredTexts
    ) {
        float bottom = anchorTop + GenericDirectionalSignGeometry.PANEL_GAP;
        float top = bottom + GenericDirectionalSignGeometry.CARTOUCHE_HEIGHT;
        float gap = GenericDirectionalSignGeometry.PANEL_GAP;

        List<GenericRouteCartoucheData> visible = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        float totalWidth = 0.0F;
        for (GenericRouteCartoucheData cartouche : cartouches) {
            if (!cartouche.isVisible()) {
                continue;
            }
            float textWidth = MotorwaySignBlockEntityRenderer.trackedTextWidth(font, RoadTextFont.L1, cartouche.text());
            float boxWidth = Math.max(1.02F, Math.min(2.20F, textWidth * 0.032F + 0.55F));
            visible.add(cartouche);
            widths.add(boxWidth);
            totalWidth += boxWidth;
        }
        if (visible.isEmpty()) {
            return;
        }
        totalWidth += gap * Math.max(0, visible.size() - 1);
        totalWidth = Math.min(totalWidth, panelRight - panelLeft);

        float x = -totalWidth / 2.0F;
        for (int i = 0; i < visible.size(); i++) {
            GenericRouteCartoucheData cartouche = visible.get(i);
            float boxWidth = widths.get(i);
            float boxLeft = x;
            float boxRight = x + boxWidth;
            MotorwaySignColor color = cartouche.backgroundColor();
            MotorwaySignBlockEntityRenderer.drawPlate(collector, poseStack, boxLeft, boxRight, bottom, top, color, light, CORNER_RADIUS);
            float centerX = (boxLeft + boxRight) / 2.0F;
            float centerY = (bottom + top) / 2.0F;
            float availableWidth = Math.max(0.10F, boxWidth - 0.16F);
            float textWidth = MotorwaySignBlockEntityRenderer.trackedTextWidth(font, RoadTextFont.L1, cartouche.text());
            float scale = textWidth <= 0.0F ? BASE_TEXT_SCALE : Math.min(BASE_TEXT_SCALE, availableWidth / textWidth);
            queueCenteredText(
                    deferredTexts, cartouche.text(), centerX, centerY,
                    RoadTextFont.L1, color.getTextArgb(), scale, light
            );
            x += boxWidth + gap;
        }
    }

    private void drawRows(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            GenericDirectionalSignData data,
            float panelLeft,
            float panelRight,
            float panelBottom,
            float panelTop,
            float lineStep,
            int light,
            List<DeferredText> deferredTexts
    ) {
        List<GenericDestinationRow> enabledRows = new ArrayList<>();
        for (GenericDestinationRow row : data.rows()) {
            /*
             * Une ligne activée mais laissée totalement vide (pas de texte,
             * pas de flèche, pas de symbole) ne doit ni réserver de hauteur,
             * ni créer un grand vide visuel — même filtre que
             * GenericDirectionalSignData.enabledRowCount() (source unique).
             */
            if (row.enabled() && row.hasContent()) {
                enabledRows.add(row);
            }
        }
        if (enabledRows.isEmpty()) {
            return;
        }

        float centerY = (panelBottom + panelTop) / 2.0F;
        float firstY = centerY + (enabledRows.size() - 1) * lineStep / 2.0F;
        float margin = GenericDirectionalSignGeometry.ROW_MARGIN;

        for (int index = 0; index < enabledRows.size(); index++) {
            GenericDestinationRow row = enabledRows.get(index);
            float y = firstY - index * lineStep;

            float leftReserve = 0.0F;
            float rightReserve = 0.0F;
            if (row.arrowEnabled()) {
                if (row.arrowPosition() == D61AArrowPosition.LEFT) {
                    leftReserve += GenericDirectionalSignGeometry.ROW_ICON_RESERVE;
                } else {
                    rightReserve += GenericDirectionalSignGeometry.ROW_ICON_RESERVE;
                }
            }
            if (row.symbolEnabled()) {
                if (row.symbolPosition() == D61AArrowPosition.LEFT) {
                    leftReserve += GenericDirectionalSignGeometry.ROW_ICON_RESERVE;
                } else {
                    rightReserve += GenericDirectionalSignGeometry.ROW_ICON_RESERVE;
                }
            }

            float textLeft = panelLeft + margin + leftReserve;
            float textRight = panelRight - margin - rightReserve;
            float availableWidth = Math.max(0.20F, textRight - textLeft);

            if (!row.text().isBlank()) {
                float textWidth = MotorwaySignBlockEntityRenderer.trackedTextWidth(font, row.font(), row.text());
                float scale = textWidth <= 0.0F ? BASE_TEXT_SCALE : Math.min(BASE_TEXT_SCALE, availableWidth / textWidth);
                float centerX = switch (row.alignment()) {
                    case LEFT -> textLeft + textWidth * scale / 2.0F;
                    case RIGHT -> textRight - textWidth * scale / 2.0F;
                    case CENTER -> (textLeft + textRight) / 2.0F;
                };
                deferredTexts.add(new DeferredText(row.text(), centerX, y, row.font(), data.background().getTextArgb(), scale, light));
            }

            if (row.arrowEnabled()) {
                float iconX = row.arrowPosition() == D61AArrowPosition.LEFT
                        ? panelLeft + margin + GenericDirectionalSignGeometry.ROW_ICON_RESERVE / 2.0F
                        : panelRight - margin - GenericDirectionalSignGeometry.ROW_ICON_RESERVE / 2.0F;
                drawArrowIcon(collector, poseStack, data.background(), row, iconX, y, lineStep, light);
            }
            if (row.symbolEnabled()) {
                float iconX = row.symbolPosition() == D61AArrowPosition.LEFT
                        ? panelLeft + margin + GenericDirectionalSignGeometry.ROW_ICON_RESERVE / 2.0F
                        : panelRight - margin - GenericDirectionalSignGeometry.ROW_ICON_RESERVE / 2.0F;
                drawSymbolIcon(collector, poseStack, row.symbol(), iconX, y, lineStep, light);
            }
        }
    }

    /**
     * Dessin réglementaire réel (voir GenericArrowShape) — jamais une seule
     * flèche tournée artificiellement. Le miroir horizontal éventuel
     * reproduit un vrai reflet réglementaire (D62d), sans jamais déformer
     * le dessin ni changer sa forme.
     */
    private void drawArrowIcon(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            MotorwaySignColor background,
            GenericDestinationRow row,
            float centerX,
            float centerY,
            float lineStep,
            int light
    ) {
        GenericArrowShape shape = row.arrowShape();
        if (shape == GenericArrowShape.NONE) {
            return;
        }
        String colorSuffix = background.isLight() ? "black" : "white";
        Identifier texture = MotorwaySignBlockEntityRenderer.artwork(
                "arrows/" + shape.getSerializedName() + "_" + colorSuffix + ".png"
        );
        float height = lineStep * 0.92F;
        float width = height * shape.aspectRatio();
        submitIcon(collector, poseStack, texture, centerX, centerY, width, height, row.arrowMirrored(), light);
    }

    private void drawSymbolIcon(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            GenericSignSymbol symbol,
            float centerX,
            float centerY,
            float lineStep,
            int light
    ) {
        if (symbol == GenericSignSymbol.NONE) {
            return;
        }
        Identifier texture = switch (symbol) {
            case AUTOROUTE -> MotorwaySignBlockEntityRenderer.texture("autoroute_logo.png");
            case EXIT -> MotorwaySignBlockEntityRenderer.artwork("exit_symbol.png");
            case NONE -> null;
        };
        if (texture == null) {
            return;
        }
        float size = lineStep * 0.92F;
        submitIcon(collector, poseStack, texture, centerX, centerY, size, size, false, light);
    }

    private void submitIcon(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float centerX,
            float centerY,
            float width,
            float height,
            boolean mirrored,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, ICON_Z);
        float halfW = width / 2.0F;
        float halfH = height / 2.0F;
        collector.order(-10).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFrontQuad(pose, consumer, -halfW, halfW, -halfH, halfH, 0.0F, 0xFFFFFFFF, light, mirrored)
        );
        poseStack.popPose();
    }

    private void queueCenteredText(
            List<DeferredText> deferredTexts,
            String value,
            float centerX,
            float centerY,
            RoadTextFont roadFont,
            int color,
            float baseScale,
            int light
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        deferredTexts.add(new DeferredText(value, centerX, centerY, roadFont, color, baseScale, light));
    }

    /**
     * Le texte est soumis APRÈS avoir quitté la mise à l'échelle du panneau
     * (mais toujours dans le repère tourné selon la face du panneau) : même
     * principe que le pipeline texte de MotorwaySignBlockEntityRenderer, qui
     * garde le texte hors de la hiérarchie de transformation du corps 3D pour
     * rester correctement pris en charge par les shaders (Iris/Complementary).
     */
    private void submitDeferredText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            DeferredText text
    ) {
        float textWidth = MotorwaySignBlockEntityRenderer.trackedTextWidth(font, text.roadFont(), text.value());
        if (textWidth <= 0.0F || text.scale() <= 0.0F) {
            return;
        }
        float textScaleWorld = text.scale() * WORLD_SCALE;
        poseStack.pushPose();
        poseStack.translate(text.x() * WORLD_SCALE, text.y() * WORLD_SCALE, TEXT_Z * WORLD_SCALE);
        poseStack.scale(textScaleWorld, -textScaleWorld, textScaleWorld);
        MotorwaySignBlockEntityRenderer.submitCenteredTrackedText(
                collector, poseStack, font, text.value(), text.roadFont(), text.color(), text.light()
        );
        poseStack.popPose();
    }

    private static void addFrontQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            boolean mirrored
    ) {
        float uLeft = mirrored ? 1.0F : 0.0F;
        float uRight = mirrored ? 0.0F : 1.0F;
        addVertex(pose, consumer, left, bottom, z, uLeft, 1, color, light);
        addVertex(pose, consumer, right, bottom, z, uRight, 1, color, light);
        addVertex(pose, consumer, right, top, z, uRight, 0, color, light);
        addVertex(pose, consumer, left, top, z, uLeft, 0, color, light);
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

    private record DeferredText(
            String value,
            float x,
            float y,
            RoadTextFont roadFont,
            int color,
            float scale,
            int light
    ) {
    }
}
