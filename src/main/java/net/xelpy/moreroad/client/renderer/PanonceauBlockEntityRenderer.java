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

    private static final Identifier SOLID_TEXTURE = texture("panel_metal.png");
    private static final Identifier REAR_TEXTURE = texture("panel_back_rounded.png");
    private static final Identifier FRONT_FACE_TEXTURE = texture("panel_face_white.png");
    private static final Identifier TRIANGLE_REAR_TEXTURE = texture("panel_back_triangle.png");
    private static final Identifier HARDWARE_TEXTURE = texture("mounting_metal.png");

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    // Le poteau standard occupe Z = [-1/16 ; +1/16] autour du centre du bloc.
    // La plaque reste juste devant lui, mais elle est désormais beaucoup plus fine.
    private static final float POLE_FRONT_Z = 0.0625F;
    private static final float PANEL_Z_BACK = 0.0815F;
    private static final float PANEL_Z_FRONT = 0.0955F;
    private static final float REAR_SURFACE_Z = PANEL_Z_BACK - 0.0010F;
    private static final float CONTENT_Z = PANEL_Z_FRONT + 0.0018F;
    private static final float TEXT_Z = CONTENT_Z + 0.0020F;

    // Les brides partent du dos de la plaque et pénètrent légèrement dans le poteau :
    // vues de l'arrière, elles apparaissent naturellement de part et d'autre du poteau.
    private static final float HARDWARE_Z_BACK = POLE_FRONT_Z - 0.0105F;
    private static final float HARDWARE_Z_FRONT = PANEL_Z_BACK - 0.0035F;

    private static final float STACK_TOP = 0.965F;
    private static final float STACK_BOTTOM = 0.055F;
    private static final float STACK_GAP = 0.030F;
    private static final float MAX_PANEL_WIDTH = 0.92F;

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

            int baseOrder = i * 6;
            submitPanelBody(panel, state, poseStack, collector, baseOrder);
            submitRearSurface(panel, state, poseStack, collector, baseOrder + 1);
            submitRearHardware(panel, state, poseStack, collector, baseOrder + 2);
            submitFrontFace(panel, state, poseStack, collector, baseOrder + 3);
            submitFrontTexture(panel, state, poseStack, collector, baseOrder + 4);
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

    private static void submitPanelBody(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        float inset = Math.max(0.024F, Math.min(0.032F, Math.min(panel.width(), panel.height()) * 0.09F));
        float left = -panel.width() / 2.0F + inset;
        float right = panel.width() / 2.0F - inset;
        float bottom = panel.bottom() + inset;
        float top = panel.top() - inset;

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_TEXTURE),
                (pose, consumer) -> {
                    if (panel.entry().variant().isTriangular()) {
                        addTriangularPrism(
                                pose,
                                consumer,
                                left,
                                right,
                                bottom,
                                top,
                                PANEL_Z_BACK,
                                PANEL_Z_FRONT,
                                light
                        );
                    } else {
                        addBox(
                                pose,
                                consumer,
                                left,
                                right,
                                bottom,
                                top,
                                PANEL_Z_BACK,
                                PANEL_Z_FRONT,
                                light
                        );
                    }
                }
        );

        poseStack.popPose();
    }

    private static void submitRearSurface(
            PanelLayout panel,
            PanonceauRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int order
    ) {
        float left = -panel.width() / 2.0F;
        float right = panel.width() / 2.0F;

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        Identifier rearTexture = panel.entry().variant().isTriangular()
                ? TRIANGLE_REAR_TEXTURE
                : REAR_TEXTURE;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(rearTexture),
                (pose, consumer) -> addRearFaceQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        panel.bottom(),
                        panel.top(),
                        REAR_SURFACE_Z,
                        light
                )
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
        if (panel.height() < 0.23F) {
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
        if (panel.entry().variant().isTriangular()) {
            return;
        }

        float left = -panel.width() / 2.0F;
        float right = panel.width() / 2.0F;

        poseStack.pushPose();
        orientToFacing(poseStack, state.facing);
        int light = state.lightCoords;

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(FRONT_FACE_TEXTURE),
                (pose, consumer) -> addFaceQuad(
                        pose,
                        consumer,
                        left,
                        right,
                        panel.bottom(),
                        panel.top(),
                        CONTENT_Z - 0.0009F,
                        light
                )
        );

        poseStack.popPose();
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
        if (value == null || value.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        FormattedCharSequence text = formattedText(value);
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
                -font.lineHeight / 2.0F,
                text,
                false,
                Font.DisplayMode.NORMAL,
                state.lightCoords,
                0xFF000000,
                0x00000000,
                0x00000000
        );

        poseStack.popPose();
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
        return Component.literal(value)
                .withStyle(Style.EMPTY.withFont(ROAD_FONT_L1))
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
