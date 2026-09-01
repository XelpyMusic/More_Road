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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.CartoucheLayout;
import net.xelpy.moreroad.block.custom.CartoucheModelBlock;
import net.xelpy.moreroad.block.custom.MotorwaySignBlock;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.MotorwaySignGraphic;
import net.xelpy.moreroad.block.custom.MotorwaySignGeometry;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.MotorwaySignRole;
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.custom.MotorwaySignStyleProfile;
import net.xelpy.moreroad.block.custom.MotorwaySignSlot;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Renderer paramétrique commun aux panneaux D31 à DA52 livrés en SVG.
 *
 * Une unité Minecraft représente un mètre. Les caractères sombres utilisent
 * une gamme Hc de 0,40 m et les caractères sur fond clair une gamme de
 * 0,32 m. La largeur et la hauteur des plaques sont ensuite calculées depuis
 * les textes, leurs marges, les symboles et les groupes de registres.
 */
public class MotorwaySignBlockEntityRenderer
        implements BlockEntityRenderer<MotorwaySignBlockEntity, MotorwaySignRenderState> {

    /*
     * V11 - les textes principaux sont différés jusqu'à la fin du rendu du
     * BlockEntity. Ils sont alors soumis depuis le repère racine du bloc,
     * exactement comme CartoucheTextRenderer. Cela évite de laisser le texte
     * dans la matrice WORLD_SCALE utilisée pour la géométrie des panneaux,
     * contexte que certains shaders Iris/Complementary ignorent pour le texte.
     */
    private static final ThreadLocal<DeferredTextContext> DEFERRED_TEXT_CONTEXT = new ThreadLocal<>();

    private static final class DeferredTextContext {
        private final Direction facing;
        private final float panelForward;
        private final List<DeferredText> texts = new ArrayList<>();
        private float yOffsetInternal;

        private DeferredTextContext(Direction facing, float panelForward) {
            this.facing = facing == null ? Direction.NORTH : facing;
            this.panelForward = panelForward;
        }
    }

    private record DeferredText(
            String value,
            float xInternal,
            float yInternal,
            RoadTextFont roadFont,
            int color,
            float scaleInternal,
            int light
    ) {
    }

    private static final FontDescription.Resource ROAD_FONT_L1 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
    );
    private static final FontDescription.Resource ROAD_FONT_L4 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
    );
    private static final FontDescription.Resource ROAD_FONT_L2 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l2")
    );

    private static FontDescription.Resource roadFontResource(RoadTextFont font) {
        return switch (font) {
            case L2 -> ROAD_FONT_L2;
            case L4 -> ROAD_FONT_L4;
            case L1, NORMAL -> ROAD_FONT_L1;
        };
    }

    private static final Identifier SOLID_TEXTURE = texture("da31c_solid_white.png");
    private static final Identifier PANEL_METAL_TEXTURE = texture("poteau_block.png");
    private static final Identifier SERVICE_TEXTURE_1 = texture("ce1.png");
    private static final Identifier SERVICE_TEXTURE_2 = texture("ce14.png");
    private static final Identifier SERVICE_TEXTURE_3 = texture("ce15a.png");
    private static final Identifier EXIT_SYMBOL_TEXTURE = artwork("exit_symbol.png");

    private static final Identifier D62C_FRAME = artwork("d62c_frame.png");
    private static final Identifier D62C_ROUTE_LEFT = artwork("d62c_route_left.png");
    private static final Identifier D62C_ROUTE_RIGHT = artwork("d62c_route_right.png");
    private static final Identifier D62C_PANEL_TOP = artwork("d62c_panel_top.png");
    private static final Identifier D62C_PANEL_BOTTOM = artwork("d62c_panel_bottom.png");
    private static final Identifier D62C_GRAPHICS = artwork("d62c_graphics.png");

    private static final Identifier D64_FRAME = artwork("d64_frame.png");
    private static final Identifier D64_ROUTE_LEFT = artwork("d64_route_left.png");
    private static final Identifier D64_ROUTE_RIGHT = artwork("d64_route_right.png");
    private static final Identifier D64_PANEL_TOP = artwork("d64_panel_top.png");
    private static final Identifier D64_PANEL_BOTTOM = artwork("d64_panel_bottom.png");
    private static final Identifier D64_GRAPHICS = artwork("d64_graphics.png");

    private static final Identifier D74A_FRAME = artwork("d74a_frame.png");
    private static final Identifier D74A_ROUTE_LEFT = artwork("d74a_route_left.png");
    private static final Identifier D74A_ROUTE_RIGHT = artwork("d74a_route_right.png");
    private static final Identifier D74A_PANEL_TOP = artwork("d74a_panel_top.png");
    private static final Identifier D74A_PANEL_BOTTOM = artwork("d74a_panel_bottom.png");
    private static final Identifier D74A_GRAPHICS = artwork("d74a_graphics.png");

    private static final Identifier D74B_FRAME = artwork("d74b_frame.png");
    private static final Identifier D74B_PANEL_TOP = artwork("d74b_panel_top.png");
    private static final Identifier D74B_PANEL_BOTTOM = artwork("d74b_panel_bottom.png");
    private static final Identifier D74B_GRAPHICS = artwork("d74b_graphics.png");

    /**
     * Panonceaux CE choisis par le joueur sous les deux registres du D44
     * (voir MotorwaySignServiceIcon) : jusqu'à 2 rangées de 3, tassées vers
     * le haut/la gauche en ignorant les emplacements sur NONE — un choix sur
     * l'emplacement 3 seul occupe donc la 1ʳᵉ case, pas la 4ᵉ. Les données
     * de position (coordonnées ARTWORK) vivent dans
     * MotorwaySignArtworkCatalog ; seule la logique de dessin reste ici avec
     * le reste du moteur de rendu.
     */
    private static void drawD44ServiceRow(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float width,
            float top,
            float height,
            ExactMappedArtwork artwork,
            MotorwaySignServiceIcon[] services,
            int light
    ) {
        int drawn = 0;
        for (MotorwaySignServiceIcon icon : services) {
            if (icon == null || !icon.isVisible()) {
                continue;
            }
            int row = drawn / 3;
            int col = drawn % 3;
            float rowTop = MotorwaySignArtworkCatalog.D44_SERVICE_ROW_TOP
                    + row * (MotorwaySignArtworkCatalog.D44_SERVICE_ICON_SIZE + MotorwaySignArtworkCatalog.D44_SERVICE_ROW_GAP);
            float colLeft = MotorwaySignArtworkCatalog.D44_SERVICE_ICON_LEFT[col];
            float iconTop = sourceY(top, height, rowTop, artwork.sourceHeight());
            float iconBottom = sourceY(
                    top, height, rowTop + MotorwaySignArtworkCatalog.D44_SERVICE_ICON_SIZE, artwork.sourceHeight()
            );
            float iconLeft = sourceX(left, width, colLeft, artwork.sourceWidth());
            float iconRight = sourceX(
                    left, width, colLeft + MotorwaySignArtworkCatalog.D44_SERVICE_ICON_SIZE, artwork.sourceWidth()
            );
            /*
             * Chaque panonceau CE est une plaque à part entière sur le
             * terrain : dos + chants, comme les corps des deux registres
             * (submitTexturedPanelBody), sinon il ressort comme un plan
             * plat sans épaisseur vu de profil.
             */
            submitTexturedPanelBody(
                    collector, poseStack, iconLeft, iconRight, iconBottom, iconTop,
                    BACK_Z, FRONT_Z, light, -30, TEXTURED_BODY_CORNER_RADIUS
            );
            drawServiceTexture(
                    collector, poseStack, texture(icon.getTextureFile()),
                    iconLeft, iconRight, iconBottom, iconTop, light
            );
            drawn++;
        }
    }

    /* DA31C : plaque de 3 pixels, soit 3/16 de bloc après mise à l'échelle. */
    private static final float PANEL_HALF_DEPTH =
            (3.0F / 16.0F) / MotorwaySignGeometry.WORLD_SCALE / 2.0F;
    private static final float FRONT_Z = PANEL_HALF_DEPTH;
    private static final float BACK_Z = -PANEL_HALF_DEPTH;
    private static final float FACE_Z = FRONT_Z + 0.004F;
    /*
     * Le texte reste légèrement devant la face. Le submit lui-même est fait
     * dans un repère monde sans l'échelle parent du panneau (voir drawText),
     * comme le texte des cartouches qui reste correctement pris en charge par
     * Iris/Complementary.
     */
    private static final float TEXT_Z = FRONT_Z + 0.020F;
    private static final float PANEL_GAP = 0.075F;
    private static final float LISTEL = 0.045F;
    private static final float MIN_CORNER_RADIUS = 0.08F;
    private static final float MAX_CORNER_RADIUS = 0.18F;
    private static final int ROUNDED_CORNER_SEGMENTS = 5;
    /*
     * Les panneaux issus des SVG ont des bords transparents arrondis. Le corps
     * 3D doit rester légèrement sous le listel du SVG afin que sa texture métal
     * ne puisse jamais apparaître dans les pixels transparents des coins.
     */
    private static final float TEXTURED_BODY_CORNER_RADIUS = 0.085F;
    /*
     * Signalé sur le D31b (ex.1/ex.2) : le coin carré du corps 3D dépasse
     * légèrement de l'arrondi du cadre (d31b_ex1_frame.png etc.), qui a un
     * rayon visuellement plus généreux que TEXTURED_BODY_CORNER_RADIUS.
     * Rayon dédié pour ces deux préréglages plutôt que de changer la
     * constante globale, qui est déjà réglée pour les autres.
     */
    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer (même arrondi que le vrai D31b). */
    static final float TEXTURED_BODY_CORNER_RADIUS_D31B = 0.115F;

    private static float textureBodyCornerRadius(MotorwaySignPreset preset) {
        return switch (preset) {
            case D31B_EX1, D31B_EX2 -> TEXTURED_BODY_CORNER_RADIUS_D31B;
            default -> TEXTURED_BODY_CORNER_RADIUS;
        };
    }

    /*
     * Signalé trop petit : le cartouche rouge du numéro de route (ex. "N
     * 171") du D31b — exemple 2. Sa taille est celle du dessin dans
     * d31b_ex2_route.png (1024x781 px), sans paramètre de mise à l'échelle —
     * on ne peut donc pas simplement l'agrandir via les données du panneau.
     * Plutôt que de retoucher l'asset, on ne garde que le petit rectangle
     * utile de la texture (rayon du cartouche + marge, mesuré une fois par
     * inspection des pixels non transparents) et on l'étire sur un quad
     * légèrement plus grand, centré au même endroit.
     */
    private static final Identifier D31B_EX2_ROUTE_TEXTURE = artwork("d31b_ex2_route.png");
    private static final float D31B_EX2_ROUTE_CARTOUCHE_ENLARGE = 1.20F;
    private static final float D31B_EX2_ROUTE_U_LEFT = 22.0F / 1024.0F;
    private static final float D31B_EX2_ROUTE_U_RIGHT = 240.0F / 1024.0F;
    private static final float D31B_EX2_ROUTE_V_TOP = 47.0F / 781.0F;
    private static final float D31B_EX2_ROUTE_V_BOTTOM = 189.0F / 781.0F;

    private static boolean isEnlargedRouteCartoucheLayer(MotorwaySignPreset preset, ExactTintedLayer layer) {
        return preset == MotorwaySignPreset.D31B_EX2
                && layer.fixedArgb() == 0
                && layer.texture().equals(D31B_EX2_ROUTE_TEXTURE);
    }

    private static void drawEnlargedRouteCartoucheLayer(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            int order
    ) {
        float worldLeft = left + D31B_EX2_ROUTE_U_LEFT * (right - left);
        float worldRight = left + D31B_EX2_ROUTE_U_RIGHT * (right - left);
        float worldTop = top - D31B_EX2_ROUTE_V_TOP * (top - bottom);
        float worldBottom = top - D31B_EX2_ROUTE_V_BOTTOM * (top - bottom);
        float centerX = (worldLeft + worldRight) / 2.0F;
        float centerY = (worldTop + worldBottom) / 2.0F;
        float halfWidth = (worldRight - worldLeft) / 2.0F * D31B_EX2_ROUTE_CARTOUCHE_ENLARGE;
        float halfHeight = (worldTop - worldBottom) / 2.0F * D31B_EX2_ROUTE_CARTOUCHE_ENLARGE;
        drawArtworkLayerCropped2D(
                collector, poseStack, texture,
                centerX - halfWidth, centerX + halfWidth,
                centerY - halfHeight, centerY + halfHeight,
                D31B_EX2_ROUTE_U_LEFT, D31B_EX2_ROUTE_U_RIGHT,
                D31B_EX2_ROUTE_V_TOP, D31B_EX2_ROUTE_V_BOTTOM,
                z, color, light, order
        );
    }
    /*
     * Le dos est volontairement plus petit que la face avant : le corps 3D
     * forme ainsi un léger biseau vers l'arrière et ne dépasse plus de la
     * silhouette du SVG lorsqu'on regarde le panneau de biais ou par derrière.
     */
    private static final float MIN_PANEL_WIDTH = 2.30F;
    private static final float MAX_PANEL_WIDTH = 6.80F;
    private static final float DARK_TEXT_SCALE = 0.032F;
    private static final float LIGHT_TEXT_SCALE = 0.028F;
    private static final int PANEL_EDGE = 0xFFD7D7D2;
    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer (poteau). */
    static final int SUPPORT_COLOR = 0xFF292929;
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final float D61_CARTOUCHE_HEIGHT = (float) (
            CartoucheLayout.CARTOUCHE_RENDER_HEIGHT / MotorwaySignGeometry.WORLD_SCALE
    );
    /** Cartouches D63c volontairement plus grands que ceux des petites pancartes. */
    private static final float D63C_CARTOUCHE_SCALE = 1.12F;
    private static final float D63C_CARTOUCHE_HEIGHT = D61_CARTOUCHE_HEIGHT
            * (D63C_CARTOUCHE_SCALE / CartoucheLayout.MODEL_SCALE);
    /*
     * Le D63c agrandit le modèle de cartouche de 0,72 à 1,12. Comme le modèle
     * est mis à l'échelle autour du centre du bloc, sa face avant avance de
     * 3/16 * (1,12 - 0,72) = 0,075 bloc. On compense uniquement le D63c afin
     * que la face du cartouche reste exactement dans le même plan que les
     * panneaux, sans modifier les cartouches déjà correctement alignés des
     * autres modèles.
     */
    private static final float D63C_CARTOUCHE_FORWARD_CORRECTION =
            (3.0F / 16.0F) * (D63C_CARTOUCHE_SCALE - CartoucheLayout.MODEL_SCALE);
    /* Aligne la face des cartouches 3D sur celle d'une plaque de 3/16. */
    private static final float CARTOUCHE_MODEL_FORWARD_OFFSET =
            3.0F / 32.0F - 0.135F;
    /* Centre du petit support, placé dans l'épaisseur arrière du cartouche. */
    private static final float CARTOUCHE_SUPPORT_FORWARD_OFFSET =
            CARTOUCHE_MODEL_FORWARD_OFFSET + 0.045F;

    private final BlockModelResolver blockResolver;

    public MotorwaySignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockResolver = context.blockModelResolver();
    }

    @Override
    public MotorwaySignRenderState createRenderState() {
        return new MotorwaySignRenderState();
    }

    @Override
    public AABB getRenderBoundingBox(MotorwaySignBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getBlock() instanceof MotorwaySignBlock
                ? state.getValue(MotorwaySignBlock.FACING)
                : Direction.NORTH;
        boolean mountedOnCrossbar = MotorwaySignBlock.isMountedOnCrossbar(
                blockEntity.getLevel(), blockEntity.getBlockPos(), facing
        );
        if (blockEntity.getPreset() == MotorwaySignPreset.D61B) {
            mountedOnCrossbar = false;
        }
        MotorwaySignGeometry geometry = MotorwaySignGeometry.forComposite(
                blockEntity.getPreset(), blockEntity.getLines(),
                blockEntity.getCustomPanels(), mountedOnCrossbar
        );
        double horizontalHalfSize = geometry.width() / 2.0 + 0.70;
        double depthHalfSize = 0.70;
        double bottom = geometry.mountedOnCrossbar()
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP - geometry.height() - 0.20
                : 0.0;
        if (!geometry.mountedOnCrossbar()) {
            int poleBlocks = connectedD61PoleBlocksBelow(blockEntity);
            bottom = Math.min(
                    bottom,
                    -poleBlocks - (hasConnectedD61FootBelow(blockEntity, poleBlocks) ? 1.0 : 0.0)
            );
        }
        double top = geometry.mountedOnCrossbar()
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP + 0.30
                : geometry.panelBottom() + geometry.height() + 0.30;
        double centerX = blockEntity.getBlockPos().getX() + 0.5;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5;
        double minX = facing.getAxis() == Direction.Axis.Z ? centerX - horizontalHalfSize : centerX - depthHalfSize;
        double maxX = facing.getAxis() == Direction.Axis.Z ? centerX + horizontalHalfSize : centerX + depthHalfSize;
        double minZ = facing.getAxis() == Direction.Axis.X ? centerZ - horizontalHalfSize : centerZ - depthHalfSize;
        double maxZ = facing.getAxis() == Direction.Axis.X ? centerZ + horizontalHalfSize : centerZ + depthHalfSize;
        return new AABB(
                minX, blockEntity.getBlockPos().getY() + bottom - 0.50, minZ,
                maxX, blockEntity.getBlockPos().getY() + top, maxZ
        );
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void extractRenderState(
            MotorwaySignBlockEntity blockEntity,
            MotorwaySignRenderState renderState,
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
        renderState.preset = blockEntity.getPreset();
        for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
            renderState.lines[i] = blockEntity.getLine(i);
        }
        renderState.customMode = blockEntity.isCustomMode();
        for (int index = 0; index < MotorwaySignBlockEntity.MAX_CUSTOM_PANELS; index++) {
            renderState.customPanels[index] = blockEntity.getCustomPanel(index);
        }
        for (int index = 0; index < MotorwaySignServiceIcon.MAX_SLOTS; index++) {
            renderState.services[index] = blockEntity.getService(index);
        }
        BlockState state = blockEntity.getBlockState();
        renderState.facing = state.getBlock() instanceof MotorwaySignBlock
                ? state.getValue(MotorwaySignBlock.FACING)
                : Direction.NORTH;
        renderState.mountedOnCrossbar = MotorwaySignBlock.isMountedOnCrossbar(
                blockEntity.getLevel(), blockEntity.getBlockPos(), renderState.facing
        );
        if (renderState.preset == MotorwaySignPreset.D61B) {
            renderState.mountedOnCrossbar = false;
        }

        for (CartoucheType cartoucheType : CartoucheType.values()) {
            BlockState cartoucheState = MoreRoadBlocks.CARTOUCHE_MODEL.get()
                    .defaultBlockState()
                    .setValue(CartoucheModelBlock.FACING, renderState.facing)
                    .setValue(CartoucheModelBlock.TYPE, cartoucheType);
            this.blockResolver.update(
                    renderState.cartoucheModels[cartoucheType.ordinal()],
                    cartoucheState,
                    BLOCK_DISPLAY_CONTEXT
            );
        }
        BlockState cartoucheSupportState = MoreRoadBlocks.CARTOUCHE_SUPPORT_MODEL.get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, renderState.facing);
        this.blockResolver.update(
                renderState.d61CartoucheSupportModel,
                cartoucheSupportState,
                BLOCK_DISPLAY_CONTEXT
        );

        if (!renderState.mountedOnCrossbar) {
            BlockState poleState = MoreRoadBlocks.SUPPORT_DA31C_POTEAU.get()
                    .defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, renderState.facing);
            this.blockResolver.update(
                    renderState.d61PoleModel, poleState, BLOCK_DISPLAY_CONTEXT
            );
            BlockState footState = MoreRoadBlocks.SUPPORT_DA31C_PIED.get()
                    .defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, renderState.facing);
            this.blockResolver.update(
                    renderState.d61FootModel, footState, BLOCK_DISPLAY_CONTEXT
            );
            if (blockEntity.getLevel() == null) {
                renderState.d61PoleLightCoords = renderState.lightCoords;
                renderState.d61PoleBlocksBelow = 0;
                renderState.d61FootBelow = false;
            } else {
                var poleFaceLightPos = blockEntity.getBlockPos()
                        .below()
                        .relative(renderState.facing);
                int blockLight = blockEntity.getLevel().getBrightness(
                        LightLayer.BLOCK, poleFaceLightPos
                );
                int skyLight = blockEntity.getLevel().getBrightness(
                        LightLayer.SKY, poleFaceLightPos
                );
                renderState.d61PoleLightCoords = (blockLight & 15) << 4
                        | (skyLight & 15) << 20;
                renderState.d61PoleBlocksBelow = connectedD61PoleBlocksBelow(blockEntity);
                renderState.d61FootBelow = hasConnectedD61FootBelow(
                        blockEntity, renderState.d61PoleBlocksBelow
                );
            }
        }
    }

    @Override
    public void submit(
            MotorwaySignRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        Font font = Minecraft.getInstance().font;
        MotorwaySignPreset preset = state.preset == null ? MotorwaySignPreset.FREEFORM : state.preset;
        MotorwaySignStyleProfile style = MotorwaySignStyleProfile.forPreset(preset);

        /*
         * Signalé : avec moins de destinations que le registre principal
         * n'en prévoit (ex. une seule ville sur un registre pour deux), ce
         * registre est redessiné en plus petit (voir drawExactMappedArtwork
         * / drawNormalizedMainDestinationStack). Calculé une seule fois ici
         * (avant même le dessin des registres "extra" ajoutés par
         * l'utilisateur, qui a lieu plus loin) pour être transmis à
         * drawExactMappedArtwork sans le recalculer, et garder le poteau
         * cohérent avec le panneau réduit.
         */
        ExactMappedArtwork earlyArtwork = style.normalizeMainDestinationStack() || usesD31DStyleStacks(preset)
                ? MotorwaySignArtworkCatalog.exactMappedArtwork(preset)
                : null;
        MainDestinationStackInfo mainStackInfo = earlyArtwork != null && style.normalizeMainDestinationStack()
                ? computeMainDestinationStackInfo(preset, state.lines, earlyArtwork)
                : null;
        /*
         * De combien le panneau principal réduit "remonte" par rapport à sa
         * pleine hauteur (registre principal + rien en dessous dans le SVG
         * pour D31b ex.1/ex.2) : utilisé plus loin pour que le décalage qui
         * positionne le panneau au-dessus des registres "extra" tienne
         * compte de cette réduction, sinon un vide apparaît entre les deux
         * malgré le poteau qui, lui, rejoint déjà le nouveau bas du panneau
         * (voir drawExactMappedArtwork).
         */
        float mainStackRise = 0.0F;
        if (mainStackInfo != null && mainStackInfo.shrinks() && earlyArtwork != null) {
            float artworkWidth = earlyArtwork.physicalWidth();
            float artworkHeight = artworkWidth * earlyArtwork.sourceHeight() / earlyArtwork.sourceWidth();
            float remainingHeightBelowDestTop = artworkHeight
                    * (1.0F - mainStackInfo.body().y() / earlyArtwork.sourceHeight());
            float shrunkWorldHeight = style.addedPanelHeight(mainStackInfo.count(), MotorwaySignGraphic.NONE);
            mainStackRise = Math.max(0.0F, remainingHeightBelowDestTop - shrunkWorldHeight);
        }
        /*
         * Signalé : sur le même principe que mainStackRise ci-dessus, mais
         * pour les DEUX registres extensibles du D31d (vert et "destination
         * locale"), qui peuvent chacun grandir OU rétrécir. Le décalage qui
         * positionne le panneau principal au-dessus des registres "extra"
         * doit tenir compte de l'écart NET de hauteur du panneau par
         * rapport à sa taille naturelle — sinon un panneau agrandi (plus de
         * villes) chevauche les registres "extra" en dessous, comme un
         * panneau rétréci laisserait un vide.
         */
        float d31dExtraShift = 0.0F;
        if (usesD31DStyleStacks(preset) && earlyArtwork != null) {
            float earlyArtworkHeight = earlyArtwork.physicalWidth()
                    * earlyArtwork.sourceHeight() / earlyArtwork.sourceWidth();
            D31DStackInfo earlyGreenStack = computeD31DStackInfo(
                    preset, state.lines, style, earlyArtwork, 1, greenStackSlots(preset),
                    greenStackBaseline(preset), 0.0F, earlyArtworkHeight
            );
            D31DStackInfo earlyLocaleStack = computeD31DStackInfo(
                    preset, state.lines, style, earlyArtwork, 2, localeStackSlots(preset),
                    localeStackBaseline(preset), 0.0F, earlyArtworkHeight
            );
            d31dExtraShift = -belowD31DStacksShift(Float.MAX_VALUE, earlyGreenStack, earlyLocaleStack);
        }

        DEFERRED_TEXT_CONTEXT.set(new DeferredTextContext(
                state.facing,
                state.mountedOnCrossbar ? 0.0F : MotorwaySignGeometry.D61B_PANEL_FORWARD
        ));
        boolean freeformStack = preset == MotorwaySignPreset.FREEFORM;
        boolean standaloneStack = preset == MotorwaySignPreset.D61B || freeformStack;
        /*
         * Un dessin figé comme D32a/D44 ne doit jamais réafficher d'anciens
         * registres supplémentaires encore présents dans une sauvegarde.
         */
        MotorwaySignPanelData[] effectiveCustomPanels = standaloneStack || style.allowsExtraPanels()
                ? state.customPanels
                : new MotorwaySignPanelData[0];
        CustomStackLayout customLayout;
        if (preset == MotorwaySignPreset.D61B) {
            customLayout = buildD61BStackLayout(font, effectiveCustomPanels, style);
        } else if (freeformStack) {
            customLayout = buildCustomStackLayout(font, effectiveCustomPanels, true, style);
        } else {
            float presetWidth = MotorwaySignGeometry.forPreset(
                    preset, state.lines, state.mountedOnCrossbar
            ).width() / MotorwaySignGeometry.WORLD_SCALE;
            customLayout = withSharedPanelWidth(
                    buildCustomStackLayout(font, effectiveCustomPanels, false, style),
                    presetWidth
            );
        }
        float customPanelHeight = customPanelStackHeight(customLayout);
        float customTop = 0.0F;
        if (standaloneStack) {
            customTop = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                    : d61PanelBottomInternal(effectiveCustomPanels) + customLayout.totalHeight();
        } else if (!customLayout.panels().isEmpty()) {
            float originalHeight = MotorwaySignGeometry.forPreset(
                    preset, state.lines, state.mountedOnCrossbar
            ).height() / MotorwaySignGeometry.WORLD_SCALE;
            customTop = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                    / MotorwaySignGeometry.WORLD_SCALE - originalHeight - PANEL_GAP
                    : 2.05F + customPanelHeight;
        }

        if (preset == MotorwaySignPreset.D63C) {
            float originalHeight = MotorwaySignGeometry.forPreset(
                    preset, state.lines, state.mountedOnCrossbar
            ).height() / MotorwaySignGeometry.WORLD_SCALE;
            float originalTop = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                    : 2.05F + customPanelHeight
                    + (customLayout.panels().isEmpty() ? 0.0F : PANEL_GAP)
                    + originalHeight;
            submitD63CCartouches(
                    state,
                    originalTop,
                    state.mountedOnCrossbar ? 0.0F : MotorwaySignGeometry.D61B_PANEL_FORWARD,
                    poseStack,
                    collector
            );
        } else {
            MotorwaySignPanelData cartouchePanel = standaloneStack
                    ? (customLayout.panels().isEmpty() ? null : customLayout.panels().getFirst())
                    : firstConfiguredPanel(effectiveCustomPanels);
            if (style.allowsCustomCartouche()
                    && cartouchePanel != null
                    && cartouchePanel.cartoucheType().isVisible()) {
                float cartoucheTop = customTop;
                if (!standaloneStack) {
                    float originalHeight = MotorwaySignGeometry.forPreset(
                            preset, state.lines, state.mountedOnCrossbar
                    ).height() / MotorwaySignGeometry.WORLD_SCALE;
                    /*
                     * Signalé : la cartouche se dessine dans un repère
                     * séparé du panneau principal (avant la mise à l'échelle
                     * commune plus bas), donc son propre calcul de "sommet
                     * du panneau" doit suivre le même écart d31dExtraShift
                     * que originalShift dès qu'un registre "extra" pousse
                     * réellement le panneau principal plus bas — sinon elle
                     * reste plaquée au sommet naturel du D31d pendant que le
                     * panneau descend d'autant. Sans registre "extra", ce
                     * décalage ne s'applique jamais (voir plus bas,
                     * !additions.panels().isEmpty()) : la cartouche doit
                     * alors garder sa formule d'origine, déjà correcte.
                     */
                    float originalTop = state.mountedOnCrossbar
                            ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                            : 2.05F + customPanelHeight
                            + (customLayout.panels().isEmpty() ? 0.0F : PANEL_GAP)
                            + originalHeight
                            + (customLayout.panels().isEmpty() ? 0.0F : d31dExtraShift);
                    cartoucheTop = originalTop + PANEL_GAP + D61_CARTOUCHE_HEIGHT;
                }
                submitCustomCartouche(
                        state,
                        cartouchePanel,
                        cartoucheTop,
                        state.mountedOnCrossbar ? 0.0F : MotorwaySignGeometry.D61B_PANEL_FORWARD,
                        poseStack,
                        collector
                );
            }
        }
        if (!standaloneStack) {
            submitOriginalCartouches(
                    state, preset, font, customLayout, poseStack, collector, mainStackRise
            );
        }

        if (!state.mountedOnCrossbar) {
            MotorwaySignGeometry groundGeometry = MotorwaySignGeometry.forComposite(
                    preset, state.lines, effectiveCustomPanels, false
            );
            submitD61CentralSupport(
                    state, groundGeometry.supportTop(), poseStack, collector
            );
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(state.facing)));
        poseStack.scale(
                MotorwaySignGeometry.WORLD_SCALE,
                MotorwaySignGeometry.WORLD_SCALE,
                MotorwaySignGeometry.WORLD_SCALE
        );
        if (!state.mountedOnCrossbar) {
            poseStack.translate(
                    0.0F, 0.0F,
                    MotorwaySignGeometry.D61B_PANEL_FORWARD
                            / MotorwaySignGeometry.WORLD_SCALE
            );
        }

        if (standaloneStack) {
            drawCustomStack(
                    collector, poseStack, font, customLayout, customTop, state.lightCoords, style, true
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }

        CustomStackLayout additions = customLayout;
        if (!additions.panels().isEmpty()) {
            float originalHeight = MotorwaySignGeometry.forPreset(
                    preset, state.lines, state.mountedOnCrossbar
            ).height() / MotorwaySignGeometry.WORLD_SCALE;
            if (state.mountedOnCrossbar) {
                /*
                 * d31dExtraShift : le haut du D31d est fixe une fois monté
                 * sur traverse, seul son bas varie avec les registres
                 * vert/local — ce bas doit donc être corrigé du même écart
                 * avant de positionner le registre "extra" juste en dessous.
                 */
                float originalBottom = MotorwaySignGeometry.MOUNTED_PANEL_TOP
                        / MotorwaySignGeometry.WORLD_SCALE - originalHeight - d31dExtraShift;
                drawCustomStack(
                        collector, poseStack, font, additions,
                        originalBottom - PANEL_GAP, state.lightCoords, style, false
                );
            } else {
                /*
                 * mainStackRise : le panneau principal réduit (moins de
                 * lignes que prévu) "remonte" son propre bas, mais ce
                 * décalage qui le positionne au-dessus des registres
                 * "extra" ne le savait pas et laissait un vide en dessous.
                 * d31dExtraShift : même idée pour le D31d, dont les
                 * registres vert/local peuvent grandir (chevauchement avec
                 * les registres "extra" sinon) ou rétrécir (vide sinon).
                 */
                float originalShift = customPanelHeight + PANEL_GAP - mainStackRise + d31dExtraShift;
                drawCustomStack(
                        collector, poseStack, font, additions,
                        2.05F + customPanelHeight, state.lightCoords, style, false
                );
                drawAdditionalSupport(
                        collector, poseStack,
                        Math.max(additions.maximumWidth(),
                                MotorwaySignGeometry.forPreset(preset, state.lines, false).width()
                                        / MotorwaySignGeometry.WORLD_SCALE),
                        originalShift + 0.12F, state.lightCoords
                );
                poseStack.translate(0.0F, originalShift, 0.0F);
                addDeferredTextYOffset(originalShift);
            }
        }

        if (preset == MotorwaySignPreset.D62C) {
            drawExactD62C(
                    collector, poseStack, font, state.lines, state.lightCoords, state.mountedOnCrossbar
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }
        if (preset == MotorwaySignPreset.D64) {
            drawExactJunctionWithRoutes(
                    collector, poseStack, font, preset, state.lines, state.lightCoords,
                    D64_FRAME, D64_ROUTE_LEFT, D64_ROUTE_RIGHT,
                    D64_PANEL_TOP, D64_PANEL_BOTTOM, D64_GRAPHICS,
                    5342.0F, 2798.0F,
                    2400.0F, 3849.0F, 510.0F, 1128.0F,
                    2958.0F, 834.0F, 4414.0F, 833.5F, 2670.0F, 2311.5F,
                    state.mountedOnCrossbar
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }
        if (preset == MotorwaySignPreset.D74A) {
            drawExactJunctionWithRoutes(
                    collector, poseStack, font, preset, state.lines, state.lightCoords,
                    D74A_FRAME, D74A_ROUTE_LEFT, D74A_ROUTE_RIGHT,
                    D74A_PANEL_TOP, D74A_PANEL_BOTTOM, D74A_GRAPHICS,
                    5339.0F, 2793.0F,
                    2399.0F, 3848.0F, 508.0F, 1128.0F,
                    2957.0F, 834.0F, 4413.0F, 831.5F, 2668.5F, 2307.5F,
                    state.mountedOnCrossbar
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }
        if (preset == MotorwaySignPreset.D74B) {
            drawExactJunctionWithoutRoutes(
                    collector, poseStack, font, preset, state.lines, state.lightCoords,
                    D74B_FRAME, D74B_PANEL_TOP, D74B_PANEL_BOTTOM, D74B_GRAPHICS,
                    state.mountedOnCrossbar
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }

        ExactMappedArtwork exactArtwork = earlyArtwork != null
                ? earlyArtwork
                : MotorwaySignArtworkCatalog.exactMappedArtwork(preset);
        if (exactArtwork != null) {
            drawExactMappedArtwork(
                    collector, poseStack, font, preset, state.lines, state.lightCoords,
                    exactArtwork, state.services, state.mountedOnCrossbar, mainStackInfo
            );
            poseStack.popPose();
            flushDeferredTexts(state, poseStack, collector);
            return;
        }

        SignLayout layout = buildLayout(font, preset, state.lines);
        if (state.mountedOnCrossbar) {
            // A portique panel is attached by its upper edge to a separately placed
            // crossbar. Keep the complete legacy layout below the attachment block.
            float mountedTextShift =
                    MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                            - layout.overallTop();
            poseStack.translate(0.0F, mountedTextShift, 0.0F);
            addDeferredTextYOffset(mountedTextShift);
            drawCrossbarMounts(
                    collector, poseStack, layout.sharedWidth(),
                    layout.overallBottom(), layout.overallTop(), state.lightCoords
            );
        } else {
            drawSupport(collector, poseStack, layout, state.lightCoords);
        }

        for (PanelLayout panel : layout.panels()) {
            drawPlate(collector, poseStack, panel.left(), panel.right(), panel.bottom(), panel.top(), panel.color(), state.lightCoords);
            drawPanelText(collector, poseStack, font, preset, state.lines, panel, preset.getGraphic(), state.lightCoords);
        }

        for (SmallPlate route : layout.routes()) {
            MotorwaySignSlot slot = preset.getSlot(route.index());
            if (isRoadCartoucheSlot(slot)) {
                continue;
            }
            drawPlate(collector, poseStack, route.left(), route.right(), route.bottom(), route.top(), route.data().color(), state.lightCoords);
            if (isExitNumberSlot(slot)) {
                drawExitNumber(
                        collector, poseStack, font, route.data().text(), route.centerX(), route.centerY(),
                        route.right() - route.left() - 0.18F, route.data().font(), route.data().color().getTextArgb(),
                        0.025F, state.lightCoords
                );
            } else {
                drawText(
                        collector, poseStack, font, route.data().text(), route.centerX(), route.centerY(),
                        route.right() - route.left() - 0.18F, route.data().font(), route.data().color().getTextArgb(),
                        0.025F, state.lightCoords
                );
            }
        }

        if (layout.distance() != null) {
            SmallPlate distance = layout.distance();
            drawPlate(collector, poseStack, distance.left(), distance.right(), distance.bottom(), distance.top(), distance.data().color(), state.lightCoords);
            drawText(
                    collector, poseStack, font, distance.data().text(), distance.centerX(), distance.centerY(),
                    distance.right() - distance.left() - 0.18F, distance.data().font(), distance.data().color().getTextArgb(),
                    0.025F, state.lightCoords
            );
        }

        drawGraphic(collector, poseStack, layout, preset.getGraphic(), state.lightCoords);
        poseStack.popPose();
        flushDeferredTexts(state, poseStack, collector);
    }

    /**
     * D62C fidèle au SVG fourni : deux cartouches, deux registres de hauteurs
     * différentes et deux flèches conservées sans redessin approximatif.
     */
    private static void drawExactD62C(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignLineData[] values,
            int light,
            boolean mountedOnCrossbar
    ) {
        final float left = -2.80F;
        final float right = 2.80F;
        final float height = 4.60F;
        final float top = mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                : 2.05F + height;
        final float bottom = top - height;
        final float width = right - left;

        /* Coordonnées des quatre plaques, directement mesurées dans D62C.svg. */
        float routeLeftX1 = exactX(left, width, 5153.0F);
        float routeLeftX2 = exactX(left, width, 8478.0F);
        float routeRightX1 = exactX(left, width, 8739.0F);
        float routeRightX2 = exactX(left, width, 12064.0F);
        float routeBottom = exactY(top, height, 1664.0F);
        float routeTop = top;

        float topPanelBottom = exactY(top, height, 4874.0F);
        float topPanelTop = exactY(top, height, 1977.0F);
        float bottomPanelBottom = bottom;
        float bottomPanelTop = exactY(top, height, 5236.0F);

        submitTexturedPanelBody(collector, poseStack, left, right, topPanelBottom, topPanelTop,
                BACK_Z, FRONT_Z, light, -30, TEXTURED_BODY_CORNER_RADIUS);
        submitTexturedPanelBody(collector, poseStack, left, right, bottomPanelBottom, bottomPanelTop,
                BACK_Z, FRONT_Z, light, -30, TEXTURED_BODY_CORNER_RADIUS);

        if (mountedOnCrossbar) {
            drawCrossbarMounts(collector, poseStack, width, bottom, top, light);
        }

        MotorwaySignLineData routeLeft = safeLine(values, 0, MotorwaySignPreset.D62C.getSlot(0));
        MotorwaySignLineData routeRight = safeLine(values, 1, MotorwaySignPreset.D62C.getSlot(1));
        MotorwaySignLineData destinationTop = safeLine(values, 2, MotorwaySignPreset.D62C.getSlot(2));
        MotorwaySignLineData destinationBottom1 = safeLine(values, 3, MotorwaySignPreset.D62C.getSlot(3));
        MotorwaySignLineData destinationBottom2 = safeLine(values, 4, MotorwaySignPreset.D62C.getSlot(4));

        /* Silhouettes et graphismes exacts, rendus dans l'ordre du SVG. */
        drawArtworkLayer(collector, poseStack, D62C_FRAME, left, right, bottom, top,
                FRONT_Z + 0.002F, 0xFFFFFFFF, light, -18);
        drawArtworkLayer(collector, poseStack, D62C_PANEL_TOP, left, right, bottom, top,
                FRONT_Z + 0.004F, destinationTop.color().getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, D62C_PANEL_BOTTOM, left, right, bottom, top,
                FRONT_Z + 0.004F, destinationBottom1.color().getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, D62C_GRAPHICS, left, right, bottom, top,
                FRONT_Z + 0.006F, 0xFFFFFFFF, light, -16);

        drawText(collector, poseStack, font, destinationTop.text(),
                0.0F, exactY(top, height, 3425.5F),
                width - 0.70F, destinationTop.font(), destinationTop.color().getTextArgb(), 0.049F, light);
        drawText(collector, poseStack, font, destinationBottom1.text(),
                0.0F, exactY(top, height, 6695.5F),
                width - 0.90F, destinationBottom1.font(), destinationBottom1.color().getTextArgb(), 0.049F, light);
        drawText(collector, poseStack, font, destinationBottom2.text(),
                0.0F, exactY(top, height, 8771.5F),
                width - 0.90F, destinationBottom2.font(), destinationBottom2.color().getTextArgb(), 0.049F, light);
    }

    private static float exactX(float left, float width, float sourceX) {
        return left + width * sourceX / 17219.0F;
    }

    private static float exactY(float top, float height, float sourceY) {
        return top - height * sourceY / 14148.0F;
    }

    /**
     * D64 et D74a : le symbole de bifurcation, les cadres et les cartouches
     * proviennent sans redessin des SVG. Seuls les trois textes restent
     * dynamiques et modifiables dans l'éditeur.
     */
    private static void drawExactJunctionWithRoutes(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            int light,
            Identifier frame,
            Identifier routeLeftTexture,
            Identifier routeRightTexture,
            Identifier panelTop,
            Identifier panelBottom,
            Identifier graphics,
            float sourceWidth,
            float sourceHeight,
            float routeLeftX,
            float routeRightX,
            float routeY,
            float routeWidth,
            float routeLeftTextX,
            float routeLeftTextY,
            float routeRightTextX,
            float routeRightTextY,
            float distanceTextX,
            float distanceTextY,
            boolean mountedOnCrossbar
    ) {
        final float width = sourceWidth / 1000.0F;
        final float height = sourceHeight / 1000.0F;
        final float left = -width / 2.0F;
        final float right = width / 2.0F;
        final float top = mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                : 2.05F + height;
        final float bottom = top - height;

        if (mountedOnCrossbar) {
            drawCrossbarMounts(collector, poseStack, width, bottom, top, light);
        } else {
            drawExactJunctionSupports(collector, poseStack, width, bottom, true, light);
        }
        drawExactJunctionBodies(collector, poseStack, left, right, bottom, top, width, height,
                sourceWidth, sourceHeight, false, light);

        MotorwaySignLineData routeLeft = safeLine(values, 0, preset.getSlot(0));
        MotorwaySignLineData routeRight = safeLine(values, 1, preset.getSlot(1));
        MotorwaySignLineData distance = safeLine(values, 2, preset.getSlot(2));

        drawArtworkLayer(collector, poseStack, frame, left, right, bottom, top,
                FRONT_Z + 0.002F, 0xFFFFFFFF, light, -18);
        drawArtworkLayer(collector, poseStack, panelTop, left, right, bottom, top,
                FRONT_Z + 0.004F, MotorwaySignColor.BLUE.getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, panelBottom, left, right, bottom, top,
                FRONT_Z + 0.004F, distance.color().getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, graphics, left, right, bottom, top,
                FRONT_Z + 0.008F, 0xFFFFFFFF, light, -15);

        drawText(collector, poseStack, font, distance.text(),
                sourceX(left, width, distanceTextX, sourceWidth),
                sourceY(top, height, distanceTextY, sourceHeight),
                width - 0.50F, distance.font(), distance.color().getTextArgb(), 0.049F, light);
    }

    /** D74b : même composition réglementaire, mais sans cartouches de route. */
    private static void drawExactJunctionWithoutRoutes(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            int light,
            Identifier frame,
            Identifier panelTop,
            Identifier panelBottom,
            Identifier graphics,
            boolean mountedOnCrossbar
    ) {
        final float sourceWidth = 3098.0F;
        final float sourceHeight = 2793.0F;
        final float width = sourceWidth / 1000.0F;
        final float height = sourceHeight / 1000.0F;
        final float left = -width / 2.0F;
        final float right = width / 2.0F;
        final float top = mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                : 2.05F + height;
        final float bottom = top - height;

        if (mountedOnCrossbar) {
            drawCrossbarMounts(collector, poseStack, width, bottom, top, light);
        } else {
            drawExactJunctionSupports(collector, poseStack, width, bottom, false, light);
        }
        drawExactJunctionBodies(collector, poseStack, left, right, bottom, top, width, height,
                sourceWidth, sourceHeight, true, light);

        MotorwaySignLineData distance = safeLine(values, 0, preset.getSlot(0));
        drawArtworkLayer(collector, poseStack, frame, left, right, bottom, top,
                FRONT_Z + 0.002F, 0xFFFFFFFF, light, -18);
        drawArtworkLayer(collector, poseStack, panelTop, left, right, bottom, top,
                FRONT_Z + 0.004F, MotorwaySignColor.BLUE.getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, panelBottom, left, right, bottom, top,
                FRONT_Z + 0.004F, distance.color().getArgb(), light, -17);
        drawArtworkLayer(collector, poseStack, graphics, left, right, bottom, top,
                FRONT_Z + 0.006F, 0xFFFFFFFF, light, -16);

        drawText(collector, poseStack, font, distance.text(),
                sourceX(left, width, 1579.0F, sourceWidth),
                sourceY(top, height, 2308.5F, sourceHeight),
                width - 0.45F, distance.font(), distance.color().getTextArgb(), 0.049F, light);
    }

    private static void drawExactJunctionBodies(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float width,
            float height,
            float sourceWidth,
            float sourceHeight,
            boolean includeTopBody,
            int light
    ) {
        if (includeTopBody) {
            submitTexturedPanelBody(collector, poseStack, left, right,
                    sourceY(top, height, 1662.0F, sourceHeight),
                    sourceY(top, height, 8.0F, sourceHeight),
                    BACK_Z, FRONT_Z, light, -30, TEXTURED_BODY_CORNER_RADIUS);
        }
        submitTexturedPanelBody(collector, poseStack, left, right,
                sourceY(top, height, sourceHeight - 5.0F, sourceHeight),
                sourceY(top, height, 1837.0F, sourceHeight),
                BACK_Z, FRONT_Z, light, -30, TEXTURED_BODY_CORNER_RADIUS);
    }

    private static void drawExactJunctionSupports(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float width,
            float panelBottom,
            boolean doublePost,
            int light
    ) {
        /* Support central DA31C commun rendu une seule fois dans submit(). */
    }

    /**
     * Deux bras arrière reprennent la géométrie de fixation du DA31C et
     * pénètrent légèrement dans la traverse placée derrière le panneau. Ils
     * rendent la liaison continue, même lorsque la caméra est exactement de
     * profil.
     */
    private static void drawCrossbarMounts(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float panelWidth,
            float panelBottom,
            float panelTop,
            int light
    ) {
        float offset = Math.min(panelWidth * 0.31F, panelWidth / 2.0F - 0.16F);
        float preferredY = 0.72F / MotorwaySignGeometry.WORLD_SCALE;
        float mountY = clamp(preferredY, panelBottom + 0.13F, panelTop - 0.13F);
        float armHalfHeight = 0.065F;
        float armBack = -1.28F;

        drawCrossbarMount(
                collector, poseStack, -offset, mountY, armHalfHeight, armBack, light
        );
        drawCrossbarMount(
                collector, poseStack, offset, mountY, armHalfHeight, armBack, light
        );
    }

    private static void drawCrossbarMount(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float x,
            float y,
            float armHalfHeight,
            float armBack,
            int light
    ) {
        submitBox(
                collector, poseStack,
                x - 0.065F, x + 0.065F,
                y - armHalfHeight, y + armHalfHeight,
                armBack, BACK_Z,
                SUPPORT_COLOR, light, -36
        );
        submitBox(
                collector, poseStack,
                x - 0.115F, x + 0.115F,
                y - 0.115F, y + 0.115F,
                armBack - 0.06F, armBack + 0.22F,
                SUPPORT_COLOR, light, -36
        );
    }

    private static float sourceX(float left, float width, float sourceX, float sourceWidth) {
        return left + width * sourceX / sourceWidth;
    }

    private static float sourceY(float top, float height, float sourceY, float sourceHeight) {
        return top - height * sourceY / sourceHeight;
    }

    private static void drawExactMappedArtwork(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            int light,
            ExactMappedArtwork artwork,
            MotorwaySignServiceIcon[] services,
            boolean mountedOnCrossbar,
            MainDestinationStackInfo mainStackInfo
    ) {
        MotorwaySignStyleProfile style = MotorwaySignStyleProfile.forPreset(preset);
        float width = artwork.physicalWidth();
        float height = width * artwork.sourceHeight() / artwork.sourceWidth();
        float left = -width / 2.0F;
        float right = width / 2.0F;
        float top = mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                : 2.05F + height;
        float bottom = top - height;

        boolean shrinkMainStack = mainStackInfo != null && mainStackInfo.shrinks();
        /*
         * Hauteur réellement utile du registre principal réduit, en unités
         * monde (mêmes unités que top/height/bottom ci-dessus) : la même
         * formule que la hauteur des registres "extra" ajoutés par
         * l'utilisateur (addedPanelHeight), pour que les deux aient une
         * apparence cohérente une fois réduits au même nombre de lignes.
         */
        float shrunkWorldHeight = shrinkMainStack
                ? style.addedPanelHeight(mainStackInfo.count(), MotorwaySignGraphic.NONE)
                : 0.0F;
        float destTopWorld = shrinkMainStack
                ? sourceY(top, height, mainStackInfo.body().y(), artwork.sourceHeight())
                : 0.0F;
        /*
         * Signalé : le registre vert (destination principale) ET le
         * registre "destination locale" du D31d doivent tous les deux
         * pouvoir compter de 1 à 3 villes (le second en compte 2 par
         * défaut, dessin d'origine), le panneau s'agrandissant ou se
         * réduisant d'autant à chacun — comme le registre principal de
         * D31b — exemple 2. Tant qu'un registre a son nombre de villes
         * "naturel" (1 pour le vert, 2 pour "destination locale"), rien ne
         * change : c'est le dessin d'origine, calé au pixel près.
         */
        D31DStackInfo greenStack = computeD31DStackInfo(
                preset, values, style, artwork, 1, greenStackSlots(preset), greenStackBaseline(preset), top, height
        );
        D31DStackInfo localeStack = computeD31DStackInfo(
                preset, values, style, artwork, 2, localeStackSlots(preset), localeStackBaseline(preset), top, height
        );
        /*
         * Signalé : le poteau/support restait attaché à la pleine hauteur
         * du SVG même quand le registre principal est redessiné en plus
         * petit, laissant un vide entre le bas du panneau et le poteau (et
         * donc aussi avec un éventuel registre "extra" en dessous, dont la
         * position ne bouge pas). Le poteau doit rejoindre le nouveau bas,
         * plus haut, du panneau réduit — ou, à l'inverse, suivre le panneau
         * agrandi (registres du D31d) plus bas.
         */
        float effectiveBottom = shrinkMainStack
                ? destTopWorld - shrunkWorldHeight
                : bottom + belowD31DStacksShift(Float.MAX_VALUE, greenStack, localeStack);

        if (!mountedOnCrossbar) {
            drawExactJunctionSupports(collector, poseStack, width, effectiveBottom, artwork.doublePost(), light);
        } else {
            drawCrossbarMounts(collector, poseStack, width, effectiveBottom, top, light);
        }
        /*
         * Signalé : chaque tentative de combler l'interstice entre deux
         * registres empilés (volume 3D étiré, pièce carrée séparée, plaque
         * plate) a fini par introduire son propre artefact visible (trou,
         * décroché, ligne grise). Retour à l'état d'origine : chaque
         * registre est un corps 3D arrondi indépendant, sans rien entre eux.
         */
        int roadCartoucheBodies = standaloneRoadCartoucheCount(preset, artwork);
        for (int bodyIndex = roadCartoucheBodies; bodyIndex < artwork.bodies().length; bodyIndex++) {
            ExactBody body = artwork.bodies()[bodyIndex];
            if ((stackDiffers(greenStack) && body == greenStack.body())
                    || (stackDiffers(localeStack) && body == localeStack.body())) {
                /* Remplacé par la plaque générique du registre agrandi/réduit, dessinée plus bas. */
                continue;
            }
            boolean isShrunkMainBody = shrinkMainStack && body == mainStackInfo.body();
            float belowShift = belowD31DStacksShift(body.y(), greenStack, localeStack);
            float bodyLeft = sourceX(left, width, body.x(), artwork.sourceWidth());
            float bodyRight = sourceX(left, width, body.x() + body.width(), artwork.sourceWidth());
            float bodyTop = sourceY(top, height, body.y(), artwork.sourceHeight()) + belowShift;
            float bodyBottom = isShrunkMainBody
                    ? destTopWorld - shrunkWorldHeight
                    : sourceY(top, height, body.y() + body.height(), artwork.sourceHeight()) + belowShift;
            submitTexturedPanelBody(collector, poseStack, bodyLeft, bodyRight, bodyBottom, bodyTop,
                    BACK_Z, FRONT_Z, light, -30, textureBodyCornerRadius(preset));
        }
        if (stackDiffers(greenStack)) {
            drawD31DStackPlate(collector, poseStack, preset, values, style, artwork,
                    left, width, top, height, greenStack, light);
        }
        if (stackDiffers(localeStack)) {
            drawD31DStackPlate(collector, poseStack, preset, values, style, artwork,
                    left, width, top, height, localeStack, light, greenStack);
        }

        int exactFrameTint = 0xFFFFFFFF;
        if (preset == MotorwaySignPreset.D32A) {
            /*
             * D32a blanc : listel sombre. D32a bleu : listel blanc.
             * Le cadre est un masque blanc, teinté avec la même couleur de
             * contraste que le texte et la flèche.
             */
            exactFrameTint = safeLine(values, 0, preset.getSlot(0)).color().getTextArgb();
        }
        if (shrinkMainStack) {
            float destTopSourceV = mainStackInfo.body().y() / artwork.sourceHeight();
            drawArtworkLayerCroppedV(collector, poseStack, artwork.frame(), left, right, destTopWorld, top,
                    0.0F, destTopSourceV, FRONT_Z + 0.002F, exactFrameTint, light, -18);
            /*
             * Signalé : sur ce registre principal redessiné en plus petit, le
             * coin restait plus pointu que les autres registres du même
             * panneau (ex. D31b ex.2). Cause : le cadre est ici tronqué à une
             * nouvelle limite basse qui ne correspond à aucun coin arrondi du
             * dessin d'origine (coupe droite), contrairement au corps 3D qui,
             * lui, reste bien arrondi. Même correctif que ci-dessous pour le
             * registre plein format : un fond à la bonne couleur de listel,
             * arrondi avec le même rayon que le corps, posé par-dessus cette
             * coupe pour retrouver un coin cohérent avec les autres registres.
             */
            ExactBody mainBody = mainStackInfo.body();
            float mainBodyLeft = sourceX(left, width, mainBody.x(), artwork.sourceWidth());
            float mainBodyRight = sourceX(left, width, mainBody.x() + mainBody.width(), artwork.sourceWidth());
            submitRoundedFace(collector, poseStack, mainBodyLeft, mainBodyRight,
                    destTopWorld - shrunkWorldHeight, destTopWorld,
                    FRONT_Z + 0.0035F, panelBorderColor(mainStackInfo.lines()[0].color()), light, -18,
                    textureBodyCornerRadius(preset));
        } else {
            drawFullCanvasLayerWithD31DShift(collector, poseStack, artwork.frame(), left, right, bottom, top,
                    artwork.sourceHeight(), greenStack, localeStack,
                    FRONT_Z + 0.002F, exactFrameTint, light, -18);
        }
        for (int layerIndex = 0; layerIndex < artwork.layers().length; layerIndex++) {
            ExactTintedLayer layer = artwork.layers()[layerIndex];
            if (layer.fixedArgb() == 0
                    && isStandaloneRoadCartoucheSlot(
                    preset, artwork, layer.slotIndex()
            )) {
                continue;
            }
            MotorwaySignSlot layerSlot = preset.getSlot(layer.slotIndex());
            if (shrinkMainStack
                    && (layerSlot.role() == MotorwaySignRole.DESTINATION
                    || layerSlot.role() == MotorwaySignRole.INFO)) {
                /* Registre principal redessiné en plus petit plus bas : son ancien calque plein format est ignoré. */
                continue;
            }
            if (layer.fixedArgb() == 0
                    && ((stackDiffers(greenStack) && layer.slotIndex() == greenStackSlots(preset)[0])
                    || (stackDiffers(localeStack) && layer.slotIndex() == localeStackSlots(preset)[0]))) {
                /* Registre redessiné en plus grand/petit (voir la plaque générique ci-dessus) : son calque teinté d'origine est ignoré. */
                continue;
            }
            MotorwaySignLineData data = safeLine(values, layer.slotIndex(), layerSlot);
            int layerColor;
            if (layer.fixedArgb() != 0) {
                layerColor = layer.fixedArgb();
            } else if (isRoadCartoucheSlot(layerSlot)) {
                MotorwaySignColor visualColor = MotorwaySignStyleProfile.visualRoadCartoucheColor(data.color());
                layerColor = visualColor.getArgb();
            } else {
                layerColor = data.color().getArgb();
            }
            if (layer.fixedArgb() == 0
                    && (layerSlot.role() == MotorwaySignRole.DESTINATION || layerSlot.role() == MotorwaySignRole.INFO)) {
                /*
                 * Signalé : en bleu/vert, le listel autour de ce registre ne
                 * ressort pas blanc. Le calque teinté (ex. d31d_panel_top.png)
                 * ne couvre que l'intérieur (bords transparents) ; le listel
                 * dépend donc entièrement de ce que le cadre dessine dessous,
                 * qui n'est pas garanti pour toutes les couleurs. On pose ici
                 * un fond à la bonne couleur de listel (même règle que
                 * drawPlate : blanc pour bleu/vert, noir pour blanc) sur toute
                 * l'emprise du registre, juste sous le calque teinté : son
                 * intérieur opaque le recouvre, seul le listel transparent le
                 * laisse apparaître.
                 */
                ExactBody colorBody = findBodyForSlot(preset, artwork, layer.slotIndex());
                if (colorBody != null) {
                    float colorBodyBelowShift = belowD31DStacksShift(colorBody.y(), greenStack, localeStack);
                    float colorBodyLeft = sourceX(left, width, colorBody.x(), artwork.sourceWidth());
                    float colorBodyRight = sourceX(left, width, colorBody.x() + colorBody.width(), artwork.sourceWidth());
                    float colorBodyTop = sourceY(top, height, colorBody.y(), artwork.sourceHeight()) + colorBodyBelowShift;
                    float colorBodyBottom = sourceY(top, height, colorBody.y() + colorBody.height(), artwork.sourceHeight())
                            + colorBodyBelowShift;
                    submitRoundedFace(collector, poseStack, colorBodyLeft, colorBodyRight, colorBodyBottom, colorBodyTop,
                            FRONT_Z + 0.0035F, panelBorderColor(data.color()), light, -18,
                            textureBodyCornerRadius(preset));
                }
            }
            if (isEnlargedRouteCartoucheLayer(preset, layer)) {
                drawEnlargedRouteCartoucheLayer(collector, poseStack, layer.texture(), left, right, bottom, top,
                        FRONT_Z + 0.004F + layerIndex * 0.0005F, layerColor, light, -17 + layerIndex);
            } else {
                drawFullCanvasLayerWithD31DShift(collector, poseStack, layer.texture(), left, right, bottom, top,
                        artwork.sourceHeight(), greenStack, localeStack,
                        FRONT_Z + 0.004F + layerIndex * 0.0005F, layerColor, light, -17 + layerIndex);
            }
        }
        overlayWhiteExactPanelBodies(
                collector, poseStack, preset, values, artwork,
                left, width, top, height, light,
                shrinkMainStack ? mainStackInfo.body() : null,
                greenStack, localeStack
        );
        redrawExactRoadCartoucheLayers(
                collector, poseStack, preset, values, artwork,
                left, right, bottom, top, light
        );
        if (preset == MotorwaySignPreset.D63C) {
            /*
             * Le fond du registre inférieur est directement inclus dans le
             * calque de cadre du SVG D63c, contrairement aux autres
             * registres. On le recouvre donc par une face dynamique en
             * conservant le listel et les côtés du modèle d'origine.
             */
            MotorwaySignLineData bottomPanel = safeLine(
                    values, 3, preset.getSlot(3)
            );
            float panelTop = sourceY(
                    top, height, 7530.0F, artwork.sourceHeight()
            );
            float panelBottom = sourceY(
                    top, height, 9935.0F, artwork.sourceHeight()
            );
            float roundedRadius = panelCornerRadius(left, right, panelBottom, panelTop);
            submitRoundedFace(
                    collector, poseStack,
                    left, right,
                    panelBottom, panelTop,
                    FRONT_Z + 0.0065F,
                    MotorwaySignColor.WHITE.getArgb(), light, -14,
                    roundedRadius
            );
            float innerLeft = left + LISTEL;
            float innerRight = right - LISTEL;
            float innerBottom = panelBottom + LISTEL;
            float innerTop = panelTop - LISTEL;
            if (innerRight > innerLeft && innerTop > innerBottom) {
                submitRoundedFace(
                        collector, poseStack,
                        innerLeft, innerRight,
                        innerBottom, innerTop,
                        FRONT_Z + 0.0070F,
                        bottomPanel.color().getArgb(), light, -13,
                        clamp(
                                roundedRadius - LISTEL,
                                0.0F,
                                panelCornerRadius(innerLeft, innerRight, innerBottom, innerTop)
                        )
                );
            }
        }
        if (artwork.graphics() != null) {
            /*
             * Le D32a unique réutilise un masque blanc : flèche sombre sur
             * fond blanc, flèche blanche sur fond bleu.
             */
            int graphicsTint = preset == MotorwaySignPreset.D32A
                    ? exactFrameTint
                    : 0xFFFFFFFF;
            drawFullCanvasLayerWithD31DShift(collector, poseStack, artwork.graphics(), left, right, bottom, top,
                    artwork.sourceHeight(), greenStack, localeStack,
                    FRONT_Z + 0.008F, graphicsTint, light, -15);
        }
        if (preset == MotorwaySignPreset.D44) {
            drawD44ServiceRow(collector, poseStack, left, width, top, height, artwork, services, light);
        }

        for (ExactTextPlacement placement : artwork.texts()) {
            MotorwaySignLineData data = safeLine(values, placement.slotIndex(), preset.getSlot(placement.slotIndex()));
            MotorwaySignSlot slot = preset.getSlot(placement.slotIndex());
            if (isStandaloneRoadCartoucheSlot(
                    preset, artwork, placement.slotIndex()
            )) {
                continue;
            }
            float x = sourceX(left, width, placement.x(), artwork.sourceWidth());
            float y = sourceY(top, height, placement.y(), artwork.sourceHeight());
            float maximumWidth = width * placement.maximumWidth() / artwork.sourceWidth();
            float scale = width * placement.sourceHeight() / artwork.sourceWidth() / font.lineHeight;
            if (preset == MotorwaySignPreset.D44) {
                /*
                 * Le centre mesuré sur le SVG correspond au centre optique du
                 * texte d'origine (majuscules/chiffres sans descendante),
                 * alors que Minecraft centre sur la hauteur de ligne complète
                 * (avec la réserve de descendante) : le texte ressort donc
                 * systématiquement un peu trop haut. Léger recalage empirique.
                 */
                y -= scale * font.lineHeight * 0.10F;
                if (placement.slotIndex() == 1) {
                    /* Distance : le calcul brut ressort trop petit face à la photo de référence. */
                    scale *= 1.30F;
                }
            }
            if ((preset == MotorwaySignPreset.D31B_EX1 || preset == MotorwaySignPreset.D31B_EX2)
                    && slot.role() == MotorwaySignRole.ROUTE) {
                /*
                 * Signalé trop haut dans son cartouche (ex. "A 20" du D31b —
                 * exemple 2) : même recalage optique que le D44 ci-dessus,
                 * pour la même raison (texte tout en majuscules, sans
                 * descendante, centré par Minecraft sur la hauteur de ligne
                 * complète qui en réserve une).
                 */
                y -= scale * font.lineHeight * 0.10F;
            }
            if (preset == MotorwaySignPreset.D31E && slot.role() == MotorwaySignRole.ROUTE) {
                /*
                 * Signalé "D 1" encore trop haut malgré le même recalage que
                 * D31b ci-dessus (0,10) : la cartouche du D31e a des
                 * proportions différentes (plus haute), un supplément plus
                 * marqué est nécessaire pour recentrer visuellement.
                 */
                y -= scale * font.lineHeight * 0.20F;
            }
            if (preset == MotorwaySignPreset.D63C
                    && (placement.slotIndex() == 2 || placement.slotIndex() == 3)) {
                float pixelWidth = trackedTextWidth(font, data.font(), data.text());
                float availableWidth = Math.max(0.20F, width - 0.64F);
                float actualScale = pixelWidth <= 0
                        ? scale
                        : Math.min(scale, availableWidth / pixelWidth);
                x = left + 0.32F + pixelWidth * actualScale / 2.0F;
                maximumWidth = availableWidth;
            }
            if (preset == MotorwaySignPreset.D32A) {
                /*
                 * Les deux lignes du D32a réel sont en caractères L4 et
                 * alignées sur la même marge gauche, jamais centrées mot par
                 * mot (ce qui décalait TULLE/BRIVE différemment).
                 */
                float leftX = sourceX(left, width, 700.0F, artwork.sourceWidth());
                float availableWidth = width * 6500.0F / artwork.sourceWidth();
                drawLeftAlignedText(
                        collector, poseStack, font, data.text(),
                        leftX, y, availableWidth, RoadTextFont.L4,
                        data.color().getTextArgb(), scale, light
                );
                continue;
            }
            if ((preset == MotorwaySignPreset.D44)
                    && placement.slotIndex() == 2) {
                /*
                 * Nom du village étape : toujours aligné à gauche du registre
                 * (comme sur le panneau réel), jamais centré — un centrage
                 * décale le mot différemment selon sa longueur et l'écarte
                 * de la marge gauche mesurée sur le SVG (id79 : x=347).
                 */
                float leftX = sourceX(left, width, 347.0F, artwork.sourceWidth());
                drawLeftAlignedText(collector, poseStack, font, data.text(),
                        leftX, y, maximumWidth, data.font(), data.color().getTextArgb(), scale, light);
                continue;
            }
            if (usesD31DStyleStacks(preset) && slot.role() == MotorwaySignRole.DESTINATION) {
                if ((stackDiffers(greenStack) && slot.panelGroup() == 0)
                        || (stackDiffers(localeStack) && slot.panelGroup() == 1)) {
                    /* Registre redessiné en plus grand/petit : dessiné séparément, voir drawD31DStackText. */
                    continue;
                }
                /*
                 * Signalé centré alors que le vrai panneau aligne les
                 * destinations à gauche, collées près du bord comme sur les
                 * autres panneaux (ex. D31b ex.1/ex.2, marge ~0,13 bloc).
                 * Bord gauche commun à tous les registres (pas le bord
                 * propre à chaque placement, qui varie avec sa largeur max
                 * et désalignerait les lignes entre elles). Vaut pour le
                 * D31d ET le D31e (demande explicite : même alignement que
                 * les autres panneaux, pas de traitement centré à part).
                 *
                 * Un registre à son nombre de villes "naturel" (non
                 * redessiné séparément) suit quand même le décalage vers le
                 * bas d'un AUTRE registre du même panneau qui, lui, serait
                 * agrandi/réduit — sinon il resterait à sa position
                 * d'origine pendant que le reste du panneau bouge.
                 */
                if (preset == MotorwaySignPreset.D31E) {
                    /*
                     * Signalé "SONJA" trop haut dans son registre à 1 seule
                     * ville (nombre "naturel", dessin d'origine non
                     * redessiné) : même recalage optique que D44/D31b plus
                     * haut (texte tout en majuscules, sans descendante).
                     * Pas appliqué au D31d : ses propres placements ont déjà
                     * été recalés empiriquement (retours précédents "trop
                     * haut") et absorbent donc déjà ce biais.
                     */
                    y -= scale * font.lineHeight * 0.15F;
                }
                y += belowD31DStacksShift(placement.y(), greenStack, localeStack);
                float leftX = d31dStackLeftX(left, width, artwork);
                drawLeftAlignedText(collector, poseStack, font, data.text(),
                        leftX, y, maximumWidth, data.font(), data.color().getTextArgb(), scale, light);
                continue;
            }
            if (style.normalizeMainDestinationStack()
                    && (slot.role() == MotorwaySignRole.DESTINATION
                    || slot.role() == MotorwaySignRole.INFO)) {
                continue;
            }
            /*
             * Les cartouches routiers intégrés directement au dessin du panneau
             * utilisent toujours les caractères L1, quelle que soit leur couleur.
             * Les cartouches 3D séparés sont rendus par le chemin dédié plus bas
             * et ne passent pas par cette branche.
             */
            RoadTextFont effectiveFont = preset == MotorwaySignPreset.D32A
                    ? RoadTextFont.L4
                    : slot.role() == MotorwaySignRole.ROUTE
                    ? RoadTextFont.L1
                    : data.font();
            if (isExitNumberSlot(slot)) {
                drawExitNumber(collector, poseStack, font, data.text(), x, y, maximumWidth,
                        effectiveFont, data.color().getTextArgb(), scale, light);
            } else {
                drawText(collector, poseStack, font, data.text(), x, y, maximumWidth,
                        effectiveFont, data.color().getTextArgb(), scale, light);
            }
        }
        if (style.normalizeMainDestinationStack()) {
            drawNormalizedMainDestinationStack(
                    collector, poseStack, font, mainStackInfo, artwork,
                    left, right, top, height, light, style,
                    textureBodyCornerRadius(preset)
            );
        }
        if (stackDiffers(greenStack)) {
            drawD31DStackText(collector, poseStack, font, preset, values, style, artwork,
                    left, width, top, height, greenStack, light);
        }
        if (stackDiffers(localeStack)) {
            drawD31DStackText(collector, poseStack, font, preset, values, style, artwork,
                    left, width, top, height, localeStack, light, greenStack);
        }
    }

    /**
     * Les masques SVG utilisent historiquement un listel blanc. Lorsqu'un
     * registre est passé en blanc, on redessine seulement son corps avec le
     * listel noir réglementaire. Les registres bleus/verts conservent ainsi
     * leur listel blanc d'origine. La correspondance corps/couleur est déduite
     * des placements de texte présents dans le même rectangle source, ce qui
     * évite une table de corrections par panneau.
     */
    private static void overlayWhiteExactPanelBodies(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            ExactMappedArtwork artwork,
            float left,
            float width,
            float top,
            float height,
            int light,
            ExactBody excludedBody,
            D31DStackInfo greenStack,
            D31DStackInfo localeStack
    ) {
        int standaloneRoadBodies = standaloneRoadCartoucheCount(preset, artwork);
        for (int bodyIndex = standaloneRoadBodies; bodyIndex < artwork.bodies().length; bodyIndex++) {
            ExactBody body = artwork.bodies()[bodyIndex];
            if (body == excludedBody
                    || (stackDiffers(greenStack) && body == greenStack.body())
                    || (stackDiffers(localeStack) && body == localeStack.body())) {
                /* Registre principal redessiné en plus petit/grand plus bas (voir drawNormalizedMainDestinationStack / les plaques du D31d). */
                continue;
            }
            MotorwaySignColor color = exactBodyColor(preset, values, artwork, body);
            if (color != MotorwaySignColor.WHITE) {
                continue;
            }
            float belowShift = belowD31DStacksShift(body.y(), greenStack, localeStack);
            float bodyLeft = sourceX(left, width, body.x(), artwork.sourceWidth());
            float bodyRight = sourceX(left, width, body.x() + body.width(), artwork.sourceWidth());
            float bodyTop = sourceY(top, height, body.y(), artwork.sourceHeight()) + belowShift;
            float bodyBottom = sourceY(top, height, body.y() + body.height(), artwork.sourceHeight()) + belowShift;
            drawPlate(
                    collector, poseStack,
                    bodyLeft, bodyRight, bodyBottom, bodyTop,
                    MotorwaySignColor.WHITE, light
            );
        }
    }

    /** Slots du registre vert extensible du D31d (1 = ville d'origine, 2/3/4 = villes optionnelles). */
    private static final int[] D31D_GREEN_STACK_SLOTS = {1, 2, 3, 4};
    /** Slots du registre "destination locale" extensible du D31d (5/6 = villes d'origine, 7 = ville optionnelle). */
    private static final int[] D31D_LOCALE_STACK_SLOTS = {5, 6, 7};
    /** Slots du registre vert extensible du D31e (1 = ville d'origine, 2/3/4 = villes optionnelles). */
    private static final int[] D31E_GREEN_STACK_SLOTS = {1, 2, 3, 4};
    /** Slots du registre "destination locale" extensible du D31e (5 = ville d'origine, 6/7/8 = villes optionnelles). */
    private static final int[] D31E_LOCALE_STACK_SLOTS = {5, 6, 7, 8};

    /**
     * Signalé : même mécanisme d'agrandissement/réduction que le D31d pour
     * le D31e (registres vert et "destination locale"), dont seul le
     * nombre de villes "naturel" (baseline) du second registre diffère (1
     * pour le D31e, contre 2 pour le D31d, qui a un dessin d'origine à 2
     * villes locales) — d'où ces petits accesseurs par préréglage plutôt
     * que de dupliquer tout le mécanisme pour le D31e.
     */
    private static boolean usesD31DStyleStacks(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D31D || preset == MotorwaySignPreset.D31E;
    }

    private static int[] greenStackSlots(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D31E ? D31E_GREEN_STACK_SLOTS : D31D_GREEN_STACK_SLOTS;
    }

    private static int[] localeStackSlots(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D31E ? D31E_LOCALE_STACK_SLOTS : D31D_LOCALE_STACK_SLOTS;
    }

    private static int localeStackBaseline(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D31E ? 1 : 2;
    }

    /**
     * Signalé : sur le D31e, le registre vert mesuré sur le SVG (dessin
     * d'origine) est nettement plus haut que nécessaire pour une seule
     * ville — sa police y est plus grosse que celle du registre local, d'où
     * une boîte proportionnellement plus grande même à 1 ligne. Contraire à
     * la demande ("réduire la boîte pour 1 ville, quitte à diverger de la
     * mesure SVG"). En fixant sa baseline à 0 (jamais atteignable, puisque
     * countFilledLeadingLines retombe au minimum à 1 dès qu'une ville est
     * tapée), ce registre "diffère" toujours dès qu'il contient du texte et
     * utilise systématiquement la plaque générique compacte — jamais le
     * dessin d'origine surdimensionné. Le D31d n'a pas ce problème (sa
     * boîte verte est déjà correctement proportionnée à 1 ville) : baseline
     * 1 inchangée pour lui.
     */
    private static int greenStackBaseline(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D31E ? 0 : 1;
    }

    /**
     * Marge gauche du texte empilé (villes 2 et suivantes, ou toute ville
     * d'un registre agrandi/réduit) : le D31d a sa propre constante mesurée
     * sur son SVG (voir plus haut, 300 en unités source) ; le D31e n'a pas
     * cette tare, donc on reprend la marge générique du mod (déjà utilisée
     * par tous les registres "extra" et le registre principal de D31b —
     * exemple 2) plutôt qu'un nombre inventé sans mesure de référence.
     */
    private static float d31dStackLeftX(float left, float width, ExactMappedArtwork artwork) {
        /*
         * Signalé : pas assez collé à gauche par rapport aux autres
         * panneaux avec la marge générique (addedLeftMargin, ~0,32 bloc,
         * pensée pour les registres "extra" plus étroits). Les deux corps
         * SVG du D31d et du D31e démarrent tous les deux à x proche de 0
         * (pas de recul propre au corps) avec une largeur totale du même
         * ordre de grandeur (~12500-13400 unités source) : la même marge en
         * unités source (300, mesurée sur le D31d) donne donc une marge
         * relative très proche sur le D31e — pas besoin d'une constante
         * séparée mesurée sur son propre SVG.
         */
        return sourceX(left, width, 300.0F, artwork.sourceWidth());
    }

    /**
     * Un registre extensible du D31d (vert ou "destination locale"), avec
     * son nombre de villes réellement rempli et l'écart de hauteur monde
     * (positif si agrandi, négatif si réduit) par rapport à son nombre de
     * villes "naturel" (baseline : 1 pour le vert, 2 pour "destination
     * locale") — celui du dessin d'origine, calé au pixel près.
     */
    private record D31DStackInfo(ExactBody body, int count, int baseline, float shift, float bottomSourceY, int[] slots) {
    }

    private static boolean stackDiffers(D31DStackInfo stack) {
        return stack != null && stack.count() != stack.baseline();
    }

    /**
     * Calcule le nombre de villes réellement renseignées pour un registre
     * extensible du D31d (en s'arrêtant à la première ligne vide, une ligne
     * laissée vide ne "libère" pas une place plus loin dans la liste), et
     * l'écart de hauteur qui en résulte. Si AUCUNE ville n'est renseignée
     * (registre pas encore touché par l'utilisateur), on retombe sur le
     * nombre "naturel" du dessin d'origine plutôt que sur 0, pour ne rien
     * changer avant que l'utilisateur n'ait effectivement tapé quelque
     * chose dans ce registre.
     */
    private static D31DStackInfo computeD31DStackInfo(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            MotorwaySignStyleProfile style,
            ExactMappedArtwork artwork,
            int bodyIndex,
            int[] slots,
            int baseline,
            float top,
            float height
    ) {
        if (!usesD31DStyleStacks(preset)) {
            return null;
        }
        ExactBody body = artwork.bodies()[bodyIndex];
        int count = 0;
        for (int slotIndex : slots) {
            MotorwaySignLineData data = safeLine(values, slotIndex, preset.getSlot(slotIndex));
            if (data.text() == null || data.text().isBlank()) {
                break;
            }
            count++;
        }
        if (count == 0) {
            count = baseline;
        }
        float shift = 0.0F;
        if (count != baseline) {
            float naturalTop = sourceY(top, height, body.y(), artwork.sourceHeight());
            float naturalBottom = sourceY(top, height, body.y() + body.height(), artwork.sourceHeight());
            float customHeight = style.addedPanelHeight(count, MotorwaySignGraphic.NONE);
            shift = customHeight - (naturalTop - naturalBottom);
        }
        return new D31DStackInfo(body, count, baseline, shift, body.y() + body.height(), slots);
    }

    /**
     * Décalage monde cumulé (vers le bas si négatif) à appliquer à tout ce
     * qui, dans le dessin d'origine, se trouve au niveau ou en dessous du
     * bas d'un registre extensible du D31d agrandi/réduit — additionné sur
     * les deux registres (vert, puis "destination locale") qui précèdent
     * la position donnée. Nul dès qu'aucun registre concerné n'est en jeu
     * (autre préréglage, ou nombre de villes "naturel" partout).
     */
    private static float belowD31DStacksShift(float sourceYValue, D31DStackInfo... stacks) {
        float shift = 0.0F;
        for (D31DStackInfo stack : stacks) {
            if (stackDiffers(stack) && sourceYValue >= stack.bottomSourceY() - 0.5F) {
                shift -= stack.shift();
            }
        }
        return shift;
    }

    private static ExactTextPlacement findPlacementForSlot(ExactMappedArtwork artwork, int slotIndex) {
        for (ExactTextPlacement placement : artwork.texts()) {
            if (placement.slotIndex() == slotIndex) {
                return placement;
            }
        }
        return null;
    }

    /**
     * Comme drawArtworkLayer, mais scinde le calque en segments quand un ou
     * deux registres extensibles du D31d sont agrandis/réduits : chaque
     * segment reste à sa place ou suit le décalage cumulé des registres qui
     * le précèdent, et le segment couvrant un registre remplacé par sa
     * propre plaque générique est purement omis — sans quoi un calque
     * "pleine toile" (cadre, graphics, calque teinté "destination
     * locale"...) resterait figé à sa position d'origine, ou continuerait
     * d'afficher un registre qui n'existe plus sous cette forme.
     */
    private static void drawFullCanvasLayerWithD31DShift(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float sourceHeight,
            D31DStackInfo greenStack,
            D31DStackInfo localeStack,
            float z,
            int color,
            int light,
            int order
    ) {
        boolean greenDiffers = stackDiffers(greenStack);
        boolean localeDiffers = stackDiffers(localeStack);
        if (!greenDiffers && !localeDiffers) {
            drawArtworkLayer(collector, poseStack, texture, left, right, bottom, top, z, color, light, order);
            return;
        }
        float cursorV = 0.0F;
        float cursorShift = 0.0F;
        if (greenDiffers) {
            float greenTopV = greenStack.body().y() / sourceHeight;
            drawD31DCanvasSegment(collector, poseStack, texture, left, right, bottom, top,
                    cursorV, greenTopV, cursorShift, z, color, light, order);
            cursorV = greenStack.bottomSourceY() / sourceHeight;
            cursorShift -= greenStack.shift();
        }
        if (localeDiffers) {
            float localeTopV = localeStack.body().y() / sourceHeight;
            drawD31DCanvasSegment(collector, poseStack, texture, left, right, bottom, top,
                    cursorV, localeTopV, cursorShift, z, color, light, order);
            /* Rien après : "destination locale" est le dernier registre du dessin d'origine. */
        } else {
            drawD31DCanvasSegment(collector, poseStack, texture, left, right, bottom, top,
                    cursorV, 1.0F, cursorShift, z, color, light, order);
        }
    }

    private static void drawD31DCanvasSegment(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float vTop,
            float vBottom,
            float shift,
            float z,
            int color,
            int light,
            int order
    ) {
        if (vBottom <= vTop) {
            return;
        }
        float worldTop = top - (top - bottom) * vTop + shift;
        float worldBottom = top - (top - bottom) * vBottom + shift;
        drawArtworkLayerCroppedV(collector, poseStack, texture, left, right, worldBottom, worldTop,
                vTop, vBottom, z, color, light, order);
    }

    /**
     * Dessine la plaque générique (corps 3D + listel + fond) d'un registre
     * extensible du D31d agrandi/réduit, à la place de son dessin exact
     * d'origine — même rayon de coin que les autres registres du panneau,
     * couleur de la première ville du registre (seule avec un sélecteur de
     * couleur dans l'éditeur). aboveStacks : les registres au-dessus de
     * celui-ci dont l'écart de hauteur doit décaler sa position (mais pas
     * son propre écart, qui ne fait que changer sa hauteur).
     */
    private static void drawD31DStackPlate(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            MotorwaySignStyleProfile style,
            ExactMappedArtwork artwork,
            float left,
            float width,
            float top,
            float height,
            D31DStackInfo stack,
            int light,
            D31DStackInfo... aboveStacks
    ) {
        float aboveShift = belowD31DStacksShift(stack.body().y(), aboveStacks);
        float plateTop = sourceY(top, height, stack.body().y(), artwork.sourceHeight()) + aboveShift;
        float plateBottom = plateTop - style.addedPanelHeight(stack.count(), MotorwaySignGraphic.NONE);
        float plateLeft = sourceX(left, width, stack.body().x(), artwork.sourceWidth());
        float plateRight = sourceX(left, width, stack.body().x() + stack.body().width(), artwork.sourceWidth());
        drawPlate(collector, poseStack, plateLeft, plateRight, plateBottom, plateTop,
                safeLine(values, stack.slots()[0], preset.getSlot(stack.slots()[0])).color(),
                light, textureBodyCornerRadius(preset));
    }

    /**
     * Dessine les 1 à 3 villes d'un registre extensible du D31d agrandi ou
     * réduit, avec le même style (police, marge gauche, couleur/listel) que
     * le dessin d'origine — même échelle de texte et même pas de ligne que
     * les autres registres à plusieurs lignes du mod (registres "extra",
     * registre principal de D31b — exemple 2), pour un espacement cohérent
     * au lieu de la taille "pleine ligne unique" du dessin d'origine, bien
     * trop grande une fois plusieurs lignes empilées.
     */
    private static void drawD31DStackText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            MotorwaySignStyleProfile style,
            ExactMappedArtwork artwork,
            float left,
            float width,
            float top,
            float height,
            D31DStackInfo stack,
            int light,
            D31DStackInfo... aboveStacks
    ) {
        ExactTextPlacement basePlacement = findPlacementForSlot(artwork, stack.slots()[0]);
        if (basePlacement == null) {
            return;
        }
        float aboveShift = belowD31DStacksShift(stack.body().y(), aboveStacks);
        float plateTop = sourceY(top, height, stack.body().y(), artwork.sourceHeight()) + aboveShift;
        float plateBottom = plateTop - style.addedPanelHeight(stack.count(), MotorwaySignGraphic.NONE);
        float maximumWidth = width * basePlacement.maximumWidth() / artwork.sourceWidth();
        float lineStep = style.addedLineStep();
        float scale = style.addedTextScale();
        float centerY = (plateTop + plateBottom) / 2.0F;
        /*
         * Signalé encore un peu trop haut dans sa plaque malgré l'offset
         * optique standard (style.addedOpticalYOffset ci-dessous, partagé
         * avec le reste du panneau) : léger supplément propre à ce bloc de
         * texte empilé, pour redescendre encore un peu l'ensemble.
         */
        float extraDownwardNudge = -0.05F;
        float y = centerY + (stack.count() - 1) * lineStep / 2.0F
                + style.addedOpticalYOffset() + extraDownwardNudge;
        /*
         * Toutes les villes de ce registre partagent la même plaque, donc la
         * même couleur de fond : la couleur choisie pour la première ville
         * (seule avec un sélecteur de couleur dans l'éditeur) fait foi pour
         * le texte de toutes.
         */
        int textArgb = safeLine(values, stack.slots()[0], preset.getSlot(stack.slots()[0])).color().getTextArgb();
        /*
         * Signalé : le D31e doit s'aligner à gauche comme les autres
         * panneaux (D31d compris), pas rester centré — même marge générique
         * que les registres "extra" (voir d31dStackLeftX).
         */
        float leftX = d31dStackLeftX(left, width, artwork);
        for (int index = 0; index < stack.count(); index++) {
            int slotIndex = stack.slots()[index];
            MotorwaySignLineData data = safeLine(values, slotIndex, preset.getSlot(slotIndex));
            float lineY = y - index * lineStep;
            drawLeftAlignedText(collector, poseStack, font, data.text(),
                    leftX, lineY, maximumWidth, data.font(), textArgb, scale, light);
        }
    }

    /** Retrouve le corps SVG (registre) contenant le placement de texte d'un champ donné. */
    private static ExactBody findBodyForSlot(
            MotorwaySignPreset preset,
            ExactMappedArtwork artwork,
            int slotIndex
    ) {
        Float placementY = null;
        for (ExactTextPlacement placement : artwork.texts()) {
            if (placement.slotIndex() == slotIndex) {
                placementY = placement.y();
                break;
            }
        }
        if (placementY == null) {
            return null;
        }
        for (ExactBody body : artwork.bodies()) {
            if (placementY >= body.y() - 2.0F && placementY <= body.y() + body.height() + 2.0F) {
                return body;
            }
        }
        return null;
    }

    private static MotorwaySignColor exactBodyColor(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            ExactMappedArtwork artwork,
            ExactBody body
    ) {
        MotorwaySignColor best = null;
        int bestPriority = -1;
        float minY = body.y() - 2.0F;
        float maxY = body.y() + body.height() + 2.0F;
        for (ExactTextPlacement placement : artwork.texts()) {
            if (placement.y() < minY || placement.y() > maxY
                    || placement.slotIndex() < 0 || placement.slotIndex() >= preset.getSlotCount()) {
                continue;
            }
            MotorwaySignSlot slot = preset.getSlot(placement.slotIndex());
            int priority = switch (slot.role()) {
                case DESTINATION, INFO -> 3;
                case DISTANCE -> 2;
                case ROUTE -> 1;
            };
            if (priority > bestPriority) {
                bestPriority = priority;
                best = safeLine(values, placement.slotIndex(), slot).color();
            }
        }
        return best;
    }

    private static void redrawExactRoadCartoucheLayers(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            ExactMappedArtwork artwork,
            float left,
            float right,
            float bottom,
            float top,
            int light
    ) {
        for (int layerIndex = 0; layerIndex < artwork.layers().length; layerIndex++) {
            ExactTintedLayer layer = artwork.layers()[layerIndex];
            if (layer.fixedArgb() != 0
                    || layer.slotIndex() < 0 || layer.slotIndex() >= preset.getSlotCount()) {
                continue;
            }
            MotorwaySignSlot slot = preset.getSlot(layer.slotIndex());
            if (!isRoadCartoucheSlot(slot)
                    || isStandaloneRoadCartoucheSlot(preset, artwork, layer.slotIndex())) {
                continue;
            }
            MotorwaySignLineData data = safeLine(values, layer.slotIndex(), slot);
            MotorwaySignColor visualColor = MotorwaySignStyleProfile.visualRoadCartoucheColor(data.color());
            if (isEnlargedRouteCartoucheLayer(preset, layer)) {
                drawEnlargedRouteCartoucheLayer(
                        collector, poseStack, layer.texture(), left, right, bottom, top,
                        FRONT_Z + 0.010F + layerIndex * 0.0005F,
                        visualColor.getArgb(), light, -12 + layerIndex
                );
            } else {
                drawArtworkLayer(
                        collector, poseStack, layer.texture(), left, right, bottom, top,
                        FRONT_Z + 0.010F + layerIndex * 0.0005F,
                        visualColor.getArgb(), light, -12 + layerIndex
                );
            }
        }
    }

    /*
     * Types ExactMappedArtwork/ExactTintedLayer/ExactTextPlacement/ExactBody
     * et leurs constructeurs (layer/fixedLayer/text/body) : voir
     * MotorwaySignArtworkCatalog, qui regroupe désormais toutes les données
     * par panneau séparément du moteur de rendu.
     */

    private static float panelCornerRadius(
            float left,
            float right,
            float bottom,
            float top
    ) {
        float width = Math.max(0.0F, right - left);
        float height = Math.max(0.0F, top - bottom);
        if (width <= 0.0F || height <= 0.0F) {
            return 0.0F;
        }
        return clamp(
                Math.min(width, height) * 0.18F,
                MIN_CORNER_RADIUS,
                Math.min(MAX_CORNER_RADIUS, Math.min(width, height) / 2.0F)
        );
    }

    private static RoundedPath roundedPath(
            float left,
            float right,
            float bottom,
            float top,
            float radius
    ) {
        float clampedRadius = clamp(
                radius,
                0.0F,
                Math.min(right - left, top - bottom) / 2.0F
        );
        if (clampedRadius <= 0.001F) {
            return new RoundedPath(
                    new float[]{left, right, right, left},
                    new float[]{top, top, bottom, bottom}
            );
        }
        int segments = Math.max(1, ROUNDED_CORNER_SEGMENTS);
        int pointCount = 4 * segments + 1;
        float[] xs = new float[pointCount];
        float[] ys = new float[pointCount];
        int index = 0;
        index = appendArc(xs, ys, index, left + clampedRadius, top - clampedRadius,
                clampedRadius, 180.0F, 90.0F, segments, true);
        index = appendArc(xs, ys, index, right - clampedRadius, top - clampedRadius,
                clampedRadius, 90.0F, 0.0F, segments, false);
        index = appendArc(xs, ys, index, right - clampedRadius, bottom + clampedRadius,
                clampedRadius, 0.0F, -90.0F, segments, false);
        appendArc(xs, ys, index, left + clampedRadius, bottom + clampedRadius,
                clampedRadius, -90.0F, -180.0F, segments, false);
        return new RoundedPath(xs, ys);
    }

    private static int appendArc(
            float[] xs,
            float[] ys,
            int startIndex,
            float centerX,
            float centerY,
            float radius,
            float startAngle,
            float endAngle,
            int segments,
            boolean includeStart
    ) {
        int firstStep = includeStart ? 0 : 1;
        int index = startIndex;
        for (int step = firstStep; step <= segments; step++) {
            float progress = step / (float) segments;
            double radians = Math.toRadians(startAngle + (endAngle - startAngle) * progress);
            xs[index] = centerX + (float) Math.cos(radians) * radius;
            ys[index] = centerY + (float) Math.sin(radians) * radius;
            index++;
        }
        return index;
    }

    private static void submitRoundedFace(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            int order,
            float radius
    ) {
        RoundedPath path = roundedPath(left, right, bottom, top, radius);
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_TEXTURE),
                (pose, consumer) -> addPolygonFace(
                        pose, consumer,
                        path.xs(), path.ys(),
                        left, right, bottom, top,
                        z, color, light,
                        0.0F, 0.0F, 1.0F,
                        true
                )
        );
    }

    private static void submitTexturedPanelBody(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int light,
            int order,
            float cornerRadius
    ) {
        float width = right - left;
        float height = top - bottom;
        float minimumSize = Math.min(width, height);
        if (minimumSize <= 0.001F) {
            return;
        }

        /*
         * Les plaques issues des textures/SVG doivent avoir exactement la
         * même emprise X/Y que leur face texturée. Une réduction du corps
         * faisait dépasser le listel/texture tout autour du panneau, surtout
         * visible de profil. Le corps reprend donc maintenant les limites
         * exactes du registre.
         *
         * Leur arrondi est volontairement plus petit que celui des pancartes
         * générées : les SVG réglementaires ont un rayon de coin proche de
         * 0,08 bloc à cette échelle. Cela évite à la fois le coin carré qui
         * ressort derrière la transparence ET la texture qui flotte au-delà
         * du modèle.
         */
        float bodyLeft = left;
        float bodyRight = right;
        float bodyBottom = bottom;
        float bodyTop = top;

        float radius = Math.min(
                cornerRadius,
                Math.min(bodyRight - bodyLeft, bodyTop - bodyBottom) / 2.0F
        );
        RoundedPath path = roundedPath(
                bodyLeft, bodyRight, bodyBottom, bodyTop, radius
        );

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(PANEL_METAL_TEXTURE),
                (pose, consumer) -> {
                    /* Dos : même silhouette que l'avant, sans déformation. */
                    addPolygonFace(
                            pose, consumer,
                            path.xs(), path.ys(),
                            bodyLeft, bodyRight, bodyBottom, bodyTop,
                            back, 0xFFFFFFFF, light,
                            0.0F, 0.0F, -1.0F,
                            false
                    );

                    /* Chant droit/arrondi : extrusion parfaitement parallèle. */
                    for (int index = 0; index < path.xs().length; index++) {
                        int next = (index + 1) % path.xs().length;
                        float x1 = path.xs()[index];
                        float y1 = path.ys()[index];
                        float x2 = path.xs()[next];
                        float y2 = path.ys()[next];
                        float dx = x2 - x1;
                        float dy = y2 - y1;
                        float length = (float) Math.sqrt(dx * dx + dy * dy);
                        float nx = length <= 0.0001F ? 0.0F : -dy / length;
                        float ny = length <= 0.0001F ? 0.0F : dx / length;
                        addFace(
                                pose, consumer,
                                x1, y1, back,
                                x2, y2, back,
                                x2, y2, front,
                                x1, y1, front,
                                0xFFFFFFFF, light,
                                nx, ny, 0.0F
                        );
                    }
                }
        );
    }

    private static void submitRoundedBody(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int color,
            int light,
            int order
    ) {
        float radius = panelCornerRadius(left, right, bottom, top);
        if (radius <= 0.001F) {
            submitBox(collector, poseStack, left, right, bottom, top, back, front, color, light, order);
            return;
        }

        boolean panelBody = color == PANEL_EDGE;
        boolean metalBody = panelBody || color == SUPPORT_COLOR;
        Identifier boxTexture = metalBody ? PANEL_METAL_TEXTURE : SOLID_TEXTURE;
        int boxColor = metalBody ? 0xFFFFFFFF : color;
        RoundedPath path = roundedPath(left, right, bottom, top, radius);

        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(boxTexture),
                (pose, consumer) -> {
                    addPolygonFace(
                            pose, consumer,
                            path.xs(), path.ys(),
                            left, right, bottom, top,
                            back, boxColor, light,
                            0.0F, 0.0F, -1.0F,
                            false
                    );
                    for (int index = 0; index < path.xs().length; index++) {
                        int next = (index + 1) % path.xs().length;
                        float x1 = path.xs()[index];
                        float y1 = path.ys()[index];
                        float x2 = path.xs()[next];
                        float y2 = path.ys()[next];
                        float dx = x2 - x1;
                        float dy = y2 - y1;
                        float length = (float) Math.sqrt(dx * dx + dy * dy);
                        float nx = length <= 0.0001F ? 0.0F : -dy / length;
                        float ny = length <= 0.0001F ? 0.0F : dx / length;
                        addFace(
                                pose, consumer,
                                x1, y1, back,
                                x2, y2, back,
                                x2, y2, front,
                                x1, y1, front,
                                boxColor, light,
                                nx, ny, 0.0F
                        );
                    }
                }
        );
    }

    private static void addPolygonFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float[] xs,
            float[] ys,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            float normalX,
            float normalY,
            float normalZ,
            boolean clockwise
    ) {
        if (xs.length < 3 || ys.length < 3 || xs.length != ys.length) {
            return;
        }
        float centerX = 0.0F;
        float centerY = 0.0F;
        for (int index = 0; index < xs.length; index++) {
            centerX += xs[index];
            centerY += ys[index];
        }
        centerX /= xs.length;
        centerY /= ys.length;

        for (int index = 0; index < xs.length; index++) {
            int next = (index + 1) % xs.length;
            if (clockwise) {
                addVertex(pose, consumer, centerX, centerY, z,
                        uvX(centerX, left, right), uvY(centerY, bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[index], ys[index], z,
                        uvX(xs[index], left, right), uvY(ys[index], bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[next], ys[next], z,
                        uvX(xs[next], left, right), uvY(ys[next], bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[next], ys[next], z,
                        uvX(xs[next], left, right), uvY(ys[next], bottom, top),
                        color, light, normalX, normalY, normalZ);
            } else {
                addVertex(pose, consumer, centerX, centerY, z,
                        uvX(centerX, left, right), uvY(centerY, bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[next], ys[next], z,
                        uvX(xs[next], left, right), uvY(ys[next], bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[index], ys[index], z,
                        uvX(xs[index], left, right), uvY(ys[index], bottom, top),
                        color, light, normalX, normalY, normalZ);
                addVertex(pose, consumer, xs[index], ys[index], z,
                        uvX(xs[index], left, right), uvY(ys[index], bottom, top),
                        color, light, normalX, normalY, normalZ);
            }
        }
    }

    private static float uvX(float x, float left, float right) {
        float width = right - left;
        return Math.abs(width) <= 0.0001F ? 0.5F : (x - left) / width;
    }

    private static float uvY(float y, float bottom, float top) {
        float height = top - bottom;
        return Math.abs(height) <= 0.0001F ? 0.5F : (top - y) / height;
    }

    private record RoundedPath(float[] xs, float[] ys) {
    }

    private static SignLayout buildLayout(
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values
    ) {
        TreeMap<Integer, List<Integer>> groups = new TreeMap<>();
        List<Integer> routes = new ArrayList<>();
        int distanceIndex = -1;

        for (int i = 0; i < preset.getSlotCount(); i++) {
            MotorwaySignSlot slot = preset.getSlot(i);
            if (slot.role() == MotorwaySignRole.ROUTE) {
                routes.add(i);
            } else if (slot.role() == MotorwaySignRole.DISTANCE) {
                distanceIndex = i;
            } else {
                groups.computeIfAbsent(Math.max(0, slot.panelGroup()), ignored -> new ArrayList<>()).add(i);
            }
        }

        float graphicReserve = sideGraphicReserve(preset.getGraphic());
        float sharedWidth = MIN_PANEL_WIDTH;
        for (List<Integer> indices : groups.descendingMap().values()) {
            for (int index : indices) {
                MotorwaySignLineData line = safeLine(values, index, preset.getSlot(index));
                sharedWidth = Math.max(sharedWidth, renderedTextWidth(font, line) + 0.72F + graphicReserve);
            }
        }
        if (groups.isEmpty()) {
            sharedWidth = 2.20F + graphicReserve;
        }
        /*
         * Largeur fixe dérivée du SVG pour les modèles du Groupe B, lue
         * depuis la MÊME source que MotorwaySignGeometry (hitbox) afin que
         * le panneau réellement dessiné ne diverge jamais de sa boîte
         * englobante. Le texte trop long reste géré par le rétrécissement
         * automatique existant de drawText (scale = min(baseScale,
         * maxWidth/width)), donc rien ne déborde ni ne se coupe.
         */
        float fixedWidth = MotorwaySignGeometry.fixedWidthMeters(preset);
        if (fixedWidth > 0.0F) {
            sharedWidth = fixedWidth;
        }
        sharedWidth = clamp(sharedWidth, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);

        float bottom = 2.05F;
        SmallPlate distance = null;
        if (distanceIndex >= 0) {
            MotorwaySignLineData data = safeLine(values, distanceIndex, preset.getSlot(distanceIndex));
            float width = clamp(renderedTextWidth(font, data) + 0.42F, 1.35F, 2.65F);
            distance = new SmallPlate(-width / 2.0F, width / 2.0F, bottom, bottom + 0.56F, distanceIndex, data);
            bottom = distance.top() + PANEL_GAP;
        }

        List<PanelLayout> panels = new ArrayList<>();
        for (List<Integer> indices : groups.values()) {
            MotorwaySignLineData first = safeLine(values, indices.getFirst(), preset.getSlot(indices.getFirst()));
            float lineStep = first.color().isLight() ? 0.39F : 0.45F;
            float height = 0.46F + lineStep * indices.size();
            if (panels.isEmpty() && usesBottomArrow(preset.getGraphic())) {
                height += 0.50F;
            }
            PanelLayout panel = new PanelLayout(
                    -sharedWidth / 2.0F,
                    sharedWidth / 2.0F,
                    bottom,
                    bottom + height,
                    first.color(),
                    List.copyOf(indices)
            );
            panels.add(panel);
            bottom = panel.top() + PANEL_GAP;
        }

        if (panels.isEmpty()) {
            MotorwaySignColor color = MotorwaySignColor.WHITE;
            PanelLayout symbolPanel = new PanelLayout(
                    -sharedWidth / 2.0F, sharedWidth / 2.0F, bottom, bottom + 0.90F,
                    color, List.of()
            );
            panels.add(symbolPanel);
            bottom = symbolPanel.top() + PANEL_GAP;
        }

        List<SmallPlate> routePlates = new ArrayList<>();
        if (!routes.isEmpty()) {
            float[] widths = new float[routes.size()];
            float totalWidth = 0.0F;
            for (int i = 0; i < routes.size(); i++) {
                int index = routes.get(i);
                MotorwaySignLineData data = safeLine(values, index, preset.getSlot(index));
                widths[i] = clamp(renderedTextWidth(font, data) + 0.38F, 1.02F, 2.20F);
                totalWidth += widths[i];
            }
            totalWidth += PANEL_GAP * Math.max(0, routes.size() - 1);
            float x = -totalWidth / 2.0F;
            for (int i = 0; i < routes.size(); i++) {
                int index = routes.get(i);
                MotorwaySignLineData data = safeLine(values, index, preset.getSlot(index));
                routePlates.add(new SmallPlate(x, x + widths[i], bottom, bottom + 0.55F, index, data));
                x += widths[i] + PANEL_GAP;
            }
            bottom += 0.55F;
        }

        float overallBottom = distance != null ? distance.bottom() : panels.getFirst().bottom();
        float overallTop = Math.max(bottom, panels.getLast().top());
        return new SignLayout(List.copyOf(panels), List.copyOf(routePlates), distance, sharedWidth, overallBottom, overallTop);
    }

    private static CustomStackLayout buildCustomStackLayout(
            Font font,
            MotorwaySignPanelData[] configuredPanels,
            boolean includeCartoucheOnlyPanel,
            MotorwaySignStyleProfile style
    ) {
        List<MotorwaySignPanelData> panels = new ArrayList<>();
        if (configuredPanels != null) {
            for (int configuredIndex = 0; configuredIndex < configuredPanels.length; configuredIndex++) {
                MotorwaySignPanelData panel = configuredPanels[configuredIndex];
                if (panel != null && panel.enabled()) {
                    /*
                     * Signalé : un registre "extra" activé (case cochée)
                     * mais dont on n'a encore rien tapé (ni texte, ni
                     * cartouche) ressortait quand même comme une pancarte
                     * vide — un rectangle blanc sans rien dedans. Aucun
                     * préréglage n'a de raison d'afficher un registre
                     * réellement vide : on l'ignore ici, quel que soit
                     * includeCartoucheOnlyPanel (qui ne tranche que le cas
                     * "cartouche seule, sans texte", pas "rien du tout").
                     */
                    if (!panel.hasPanelContent() && !panel.cartoucheType().isVisible()) {
                        continue;
                    }
                    if (!includeCartoucheOnlyPanel
                            && panel.cartoucheType().isVisible()
                            && !panel.hasPanelContent()) {
                        continue;
                    }
                    MotorwaySignPanelData normalized = withStyleProfile(panel, style);
                    panels.add(configuredIndex == 0
                            ? normalized
                            : withoutCartouche(normalized));
                }
            }
        }

        float[] widths = new float[panels.size()];
        float[] heights = new float[panels.size()];
        float[] cartoucheWidths = new float[panels.size()];
        float maximumWidth = 2.30F;
        float totalHeight = 0.0F;
        for (int index = 0; index < panels.size(); index++) {
            MotorwaySignPanelData panel = panels.get(index);
            float textWidth = 0.0F;
            for (int lineIndex = 0; lineIndex < panel.lineCount(); lineIndex++) {
                MotorwaySignLineData line = new MotorwaySignLineData(
                        panel.line(lineIndex), panel.font(lineIndex), panel.background()
                );
                MotorwaySignLineData distance = new MotorwaySignLineData(
                        panel.distance(lineIndex), panel.font(lineIndex), panel.background()
                );
                textWidth = Math.max(
                        textWidth,
                        renderedTextWidth(font, line)
                                + (panel.distance(lineIndex).isBlank()
                                ? 0.0F
                                : renderedTextWidth(font, distance) + 0.28F)
                );
            }
            widths[index] = clamp(
                    textWidth + 0.72F + sideGraphicReserve(panel.graphic()),
                    MIN_PANEL_WIDTH,
                    MAX_PANEL_WIDTH
            );
            heights[index] = style.addedPanelHeight(panel.lineCount(), panel.graphic());
            maximumWidth = Math.max(maximumWidth, widths[index]);
            totalHeight += heights[index];

            if (panel.cartoucheType().isVisible()) {
                MotorwaySignColor cartoucheColor = cartoucheColor(panel.cartoucheType());
                MotorwaySignLineData cartoucheLine = new MotorwaySignLineData(
                        panel.cartoucheText(), RoadTextFont.L1, cartoucheColor
                );
                cartoucheWidths[index] = clamp(
                        renderedTextWidth(font, cartoucheLine) + 0.52F,
                        1.02F,
                        Math.max(1.02F, widths[index])
                );
                totalHeight += D61_CARTOUCHE_HEIGHT + PANEL_GAP;
                maximumWidth = Math.max(maximumWidth, cartoucheWidths[index]);
            }
            if (index + 1 < panels.size()) {
                totalHeight += PANEL_GAP;
            }
        }
        return new CustomStackLayout(
                List.copyOf(panels), widths, heights, cartoucheWidths,
                maximumWidth, totalHeight
        );
    }

    private static CustomStackLayout buildD61BStackLayout(
            Font font,
            MotorwaySignPanelData[] configuredPanels,
            MotorwaySignStyleProfile style
    ) {
        List<MotorwaySignPanelData> enabled = new ArrayList<>();
        CartoucheType topCartouche = CartoucheType.NONE;
        String topCartoucheText = "";
        if (configuredPanels != null && configuredPanels.length > 0
                && configuredPanels[0] != null) {
            topCartouche = configuredPanels[0].cartoucheType();
            topCartoucheText = configuredPanels[0].cartoucheText();
        }
        if (configuredPanels != null) {
            for (MotorwaySignPanelData panel : configuredPanels) {
                if (panel != null && panel.enabled()) {
                    enabled.add(new MotorwaySignPanelData(
                            true, panel.lineCount(),
                            panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                            panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                            panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                            MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
                    ));
                }
            }
        }
        if (enabled.isEmpty()) {
            enabled.add(new MotorwaySignPanelData(
                    true, 1,
                    "", "", "", "", "", "", "", "",
                    RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1,
                    MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
            ));
        }
        MotorwaySignPanelData first = enabled.getFirst();
        enabled.set(0, new MotorwaySignPanelData(
                first.enabled(), first.lineCount(),
                first.line1(), first.line2(), first.line3(), first.line4(),
                first.distance1(), first.distance2(), first.distance3(), first.distance4(),
                first.line1Font(), first.line2Font(), first.line3Font(), first.line4Font(),
                MotorwaySignColor.BLUE, topCartouche, topCartoucheText, MotorwaySignGraphic.NONE
        ));
        CustomStackLayout layout = buildCustomStackLayout(
                font, enabled.toArray(MotorwaySignPanelData[]::new), true, style
        );
        float[] sharedWidths = layout.widths().clone();
        java.util.Arrays.fill(sharedWidths, 6.20F);
        return new CustomStackLayout(
                layout.panels(), sharedWidths, layout.heights(), layout.cartoucheWidths(),
                6.20F, layout.totalHeight()
        );
    }

    /**
     * Les pancartes ajoutées appartiennent au même ensemble physique que le
     * SVG choisi. Elles reprennent donc toutes sa largeur, même lorsque leur
     * texte est vide ou très court. Le texte se réduit déjà automatiquement
     * dans cette largeur, comme sur les panneaux D21/D61.
     */
    private static CustomStackLayout withSharedPanelWidth(
            CustomStackLayout layout,
            float presetWidth
    ) {
        if (layout.panels().isEmpty()) {
            return layout;
        }
        float sharedWidth = clamp(presetWidth, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        float[] widths = layout.widths().clone();
        java.util.Arrays.fill(widths, sharedWidth);
        return new CustomStackLayout(
                layout.panels(), widths, layout.heights(), layout.cartoucheWidths(),
                sharedWidth, layout.totalHeight()
        );
    }

    /** Hauteur des seules pancartes : le cartouche global reste au sommet de l'ensemble. */
    private static float customPanelStackHeight(CustomStackLayout layout) {
        float height = 0.0F;
        for (int index = 0; index < layout.heights().length; index++) {
            if (index > 0) {
                height += PANEL_GAP;
            }
            height += layout.heights()[index];
        }
        return height;
    }

    /** Le cartouche appartient au registre principal, même si aucune pancarte ajoutée n'est active. */
    private static MotorwaySignPanelData firstConfiguredPanel(MotorwaySignPanelData[] panels) {
        if (panels == null || panels.length == 0 || panels[0] == null) {
            return null;
        }
        return panels[0];
    }

    private static float d61PanelBottomInternal(MotorwaySignPanelData[] panels) {
        int enabledCount = 0;
        if (panels != null) {
            for (MotorwaySignPanelData panel : panels) {
                if (panel != null && panel.enabled()) {
                    enabledCount++;
                }
            }
        }
        float worldBottom = enabledCount <= 1
                ? MotorwaySignGeometry.D61B_SINGLE_PANEL_BOTTOM
                : MotorwaySignGeometry.D61B_PANEL_BOTTOM;
        return worldBottom / MotorwaySignGeometry.WORLD_SCALE;
    }

    private static void drawCustomStack(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            CustomStackLayout layout,
            float top,
            int light,
            MotorwaySignStyleProfile style,
            boolean inlineCartouche
    ) {
        float cursorTop = top;
        for (int index = 0; index < layout.panels().size(); index++) {
            MotorwaySignPanelData data = withStyleProfile(layout.panels().get(index), style);
            if (inlineCartouche && data.cartoucheType().isVisible()) {
                float cartoucheTop = cursorTop;
                float cartoucheBottom = cartoucheTop - D61_CARTOUCHE_HEIGHT;
                cursorTop = cartoucheBottom - PANEL_GAP;
            }

            float panelTop = cursorTop;
            float panelBottom = panelTop - layout.heights()[index];
            float width = layout.widths()[index];
            PanelLayout panelLayout = new PanelLayout(
                    -width / 2.0F, width / 2.0F,
                    panelBottom, panelTop, data.background(), List.of(0)
            );
            drawPlate(
                    collector, poseStack, panelLayout.left(), panelLayout.right(),
                    panelLayout.bottom(), panelLayout.top(), panelLayout.color(), light
            );
            drawCustomPanelText(collector, poseStack, font, data, panelLayout, light, style);
            drawGraphic(
                    collector, poseStack,
                    new SignLayout(
                            List.of(panelLayout), List.of(), null,
                            width, panelBottom, panelTop
                    ),
                    data.graphic(), light
            );
            cursorTop = panelBottom - PANEL_GAP;
        }
    }

    private static void drawCustomPanelText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPanelData data,
            PanelLayout panel,
            int light,
            MotorwaySignStyleProfile style
    ) {
        final float textScale = style.addedTextScale();
        final float lineStep = style.addedLineStep();
        final float distanceGap = style.addedDistanceGap();
        float leftMargin = style.addedLeftMargin();
        float rightMargin = style.addedRightMargin();
        float reserve = sideGraphicReserve(data.graphic());
        float graphicOffset = textCenterOffset(data.graphic(), reserve);
        if (graphicOffset > 0.0F) {
            leftMargin += reserve;
        } else if (graphicOffset < 0.0F) {
            rightMargin += reserve;
        }

        /*
         * Signalé trop serré sur un registre à plusieurs villes de longueurs
         * très différentes (ex. "LA GRIGONNAIS" / "BLAIN") : chaque ligne
         * calculait sa propre échelle indépendamment des autres, donc une
         * ville courte s'affichait nettement plus grande qu'une ville
         * longue au même pas de ligne fixe — l'écart PARAISSAIT plus serré
         * pour la ligne agrandie, sans que le pas de ligne lui-même ait
         * changé. Une seule échelle, partagée par toutes les lignes du
         * registre (la plus contrainte des lignes), règle ça à la racine :
         * toutes les villes du même registre ont désormais la même taille.
         */
        float[] cityPixelsPerLine = new float[data.lineCount()];
        float sharedScale = textScale;
        for (int index = 0; index < data.lineCount(); index++) {
            float cityPixels = trackedTextWidth(font, data.font(index), data.line(index));
            cityPixelsPerLine[index] = cityPixels;
            if (cityPixels <= 0.0F) {
                continue;
            }
            float distanceWidth = data.distance(index).isBlank()
                    ? 0.0F
                    : trackedTextWidth(font, data.font(index), data.distance(index)) * textScale;
            float cityRight = panel.right() - rightMargin
                    - (distanceWidth > 0.0F ? distanceWidth + distanceGap : 0.0F);
            float maximumWidth = Math.max(0.20F, cityRight - (panel.left() + leftMargin));
            sharedScale = Math.min(sharedScale, maximumWidth / cityPixels);
        }

        float y = panel.centerY() + (data.lineCount() - 1) * lineStep / 2.0F
                + style.addedOpticalYOffset();
        if (usesBottomArrow(data.graphic())) {
            y += 0.22F;
        }

        for (int index = 0; index < data.lineCount(); index++) {
            float lineY = y - index * lineStep;
            float distancePixels = trackedTextWidth(font, data.font(index), data.distance(index));
            float distanceWidth = data.distance(index).isBlank()
                    ? 0.0F
                    : distancePixels * sharedScale;
            if (distanceWidth > 0.0F) {
                drawText(
                        collector, poseStack, font, data.distance(index),
                        panel.right() - rightMargin - distanceWidth / 2.0F, lineY,
                        distanceWidth + 0.002F, data.font(index),
                        panel.color().getTextArgb(), sharedScale, light
                );
            }

            float cityPixels = cityPixelsPerLine[index];
            if (cityPixels <= 0) {
                continue;
            }
            float cityLeft = panel.left() + leftMargin;
            float actualWidth = cityPixels * sharedScale;
            drawText(
                    collector, poseStack, font, data.line(index),
                    cityLeft + actualWidth / 2.0F, lineY, actualWidth + 0.002F,
                    data.font(index), panel.color().getTextArgb(), sharedScale, light
            );
        }
    }

    /**
     * Nombre de destinations RÉELLEMENT renseignées (à partir du haut) et
     * corps SVG concerné, pour le registre principal des préréglages à
     * hauteur normalisée (D31b ex.1/ex.2). Extrait à part pour être calculé
     * une fois, avant le dessin du cadre : le cadre et les calques doivent
     * savoir si ce registre va être redessiné en plus petit AVANT d'être
     * eux-mêmes dessinés, pas seulement au moment où le texte s'affiche.
     */
    private record MainDestinationStackInfo(
            ExactBody body,
            int available,
            int count,
            MotorwaySignLineData[] lines
    ) {
        boolean shrinks() {
            return this.count < this.available;
        }
    }

    private static MainDestinationStackInfo computeMainDestinationStackInfo(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            ExactMappedArtwork artwork
    ) {
        List<ExactTextPlacement> placements = new ArrayList<>();
        for (ExactTextPlacement placement : artwork.texts()) {
            if (placement.slotIndex() < 0 || placement.slotIndex() >= preset.getSlotCount()) {
                continue;
            }
            MotorwaySignRole role = preset.getSlot(placement.slotIndex()).role();
            if (role == MotorwaySignRole.DESTINATION || role == MotorwaySignRole.INFO) {
                placements.add(placement);
            }
        }
        placements.sort(java.util.Comparator.comparingDouble(ExactTextPlacement::y));
        if (placements.isEmpty()) {
            return null;
        }

        ExactBody targetBody = null;
        for (ExactBody body : artwork.bodies()) {
            boolean containsAll = true;
            for (ExactTextPlacement placement : placements) {
                if (placement.y() < body.y() - 2.0F
                        || placement.y() > body.y() + body.height() + 2.0F) {
                    containsAll = false;
                    break;
                }
            }
            if (containsAll) {
                targetBody = body;
                break;
            }
        }
        if (targetBody == null) {
            return null;
        }

        int available = Math.min(4, placements.size());
        /*
         * Nombre de lignes RÉELLEMENT renseignées (en partant du haut), pas
         * le nombre total de registres du préréglage : sinon une destination
         * laissée vide ne referme pas l'espace qu'elle occupait et ne se
         * retrouve jamais recentrée avec les lignes voisines, contrairement
         * aux vrais panneaux qui peuvent avoir 1, 2 ou 3 destinations.
         */
        int count = 0;
        while (count < available) {
            MotorwaySignLineData candidate = safeLine(
                    values, placements.get(count).slotIndex(), preset.getSlot(placements.get(count).slotIndex())
            );
            if (candidate.text() == null || candidate.text().isBlank()) {
                break;
            }
            count++;
        }
        if (count == 0) {
            return null;
        }
        MotorwaySignLineData[] lines = new MotorwaySignLineData[count];
        for (int index = 0; index < count; index++) {
            ExactTextPlacement placement = placements.get(index);
            lines[index] = safeLine(
                    values, placement.slotIndex(), preset.getSlot(placement.slotIndex())
            );
        }
        return new MainDestinationStackInfo(targetBody, available, count, lines);
    }

    private static void drawNormalizedMainDestinationStack(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MainDestinationStackInfo info,
            ExactMappedArtwork artwork,
            float left,
            float right,
            float top,
            float height,
            int light,
            MotorwaySignStyleProfile style,
            float bodyCornerRadius
    ) {
        if (info == null) {
            return;
        }
        MotorwaySignLineData[] lines = info.lines();
        int count = info.count();
        MotorwaySignLineData first = lines[0];
        MotorwaySignPanelData panelData = new MotorwaySignPanelData(
                true, count,
                count > 0 ? lines[0].text() : "",
                count > 1 ? lines[1].text() : "",
                count > 2 ? lines[2].text() : "",
                count > 3 ? lines[3].text() : "",
                "", "", "", "",
                count > 0 ? lines[0].font() : RoadTextFont.L1,
                count > 1 ? lines[1].font() : RoadTextFont.L1,
                count > 2 ? lines[2].font() : RoadTextFont.L1,
                count > 3 ? lines[3].font() : RoadTextFont.L1,
                first.color(), CartoucheType.NONE, "", MotorwaySignGraphic.NONE
        );
        boolean shrinks = info.shrinks();
        float panelTop = sourceY(top, height, info.body().y(), artwork.sourceHeight());
        float panelBottom = shrinks
                ? panelTop - style.addedPanelHeight(count, MotorwaySignGraphic.NONE)
                : sourceY(top, height, info.body().y() + info.body().height(), artwork.sourceHeight());
        float physicalWidth = right - left;
        float bodyLeft = sourceX(left, physicalWidth, info.body().x(), artwork.sourceWidth());
        float bodyRight = sourceX(left, physicalWidth, info.body().x() + info.body().width(), artwork.sourceWidth());
        if (shrinks) {
            /*
             * Signalé : avec une seule ville sur un registre prévu pour
             * plusieurs, le registre restait à sa taille pleine (mesurée
             * sur le SVG), avec beaucoup de vide autour du texte. Le cadre
             * (drawExactMappedArtwork) et les calques de cette zone ne sont
             * plus dessinés dans ce cas — cette plaque, à la taille
             * réellement utile, dessine elle-même son propre listel/bordure
             * (même style que les registres "Registre N" ajoutés par
             * l'utilisateur), pour qu'il n'y ait pas de trou visuel.
             */
            drawPlate(collector, poseStack, bodyLeft, bodyRight, panelBottom, panelTop, first.color(), light,
                    bodyCornerRadius);
        }
        PanelLayout panelLayout = new PanelLayout(
                bodyLeft, bodyRight, panelBottom, panelTop, first.color(), List.of()
        );
        drawCustomPanelText(collector, poseStack, font, panelData, panelLayout, light, style);
    }


    private static MotorwaySignPanelData withStyleProfile(
            MotorwaySignPanelData panel,
            MotorwaySignStyleProfile style
    ) {
        MotorwaySignColor background = style.sanitizeCustomBackground(panel.background());
        boolean keepDistances = style.allowsCustomDistances();
        CartoucheType cartoucheType = style.allowsCustomCartouche()
                ? panel.cartoucheType()
                : CartoucheType.NONE;
        String cartoucheText = style.allowsCustomCartouche() ? panel.cartoucheText() : "";
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                keepDistances ? panel.distance1() : "",
                keepDistances ? panel.distance2() : "",
                keepDistances ? panel.distance3() : "",
                keepDistances ? panel.distance4() : "",
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                background, cartoucheType, cartoucheText, panel.graphic()
        );
    }


    private static void drawAdditionalSupport(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float width,
            float supportTop,
            int light
    ) {
        drawSupport(
                collector,
                poseStack,
                new SignLayout(List.of(), List.of(), null, width, supportTop - 0.12F, supportTop),
                light
        );
    }

    private static void submitD61CentralSupport(
            MotorwaySignRenderState state,
            float supportTop,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        /* Même modèle et même calcul d'éclairage que le poteau DA31C inférieur. */
        /*
         * On recouvre également les blocs de poteau connectés en dessous.
         * Le raccord et la colonne utilisent alors strictement le même rendu,
         * ce qui supprime la couture d'occlusion entre BER et modèle du monde.
         */
        float supportBottom = -Math.max(0.01F, state.d61PoleBlocksBelow);
        poseStack.pushPose();
        poseStack.translate(0.5F, supportBottom, 0.5F);
        poseStack.scale(1.004F, supportTop - supportBottom, 1.004F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        state.d61PoleModel.submit(
                poseStack, collector, state.d61PoleLightCoords,
                OverlayTexture.NO_OVERLAY, 0
        );
        poseStack.popPose();

        if (state.d61FootBelow) {
            poseStack.pushPose();
            poseStack.translate(
                    0.5F,
                    -state.d61PoleBlocksBelow - 1.0F,
                    0.5F
            );
            poseStack.scale(1.004F, 1.0F, 1.004F);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            state.d61FootModel.submit(
                    poseStack, collector, state.d61PoleLightCoords,
                    OverlayTexture.NO_OVERLAY, 0
            );
            poseStack.popPose();
        }
    }

    private static int connectedD61PoleBlocksBelow(MotorwaySignBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) {
            return 0;
        }
        int count = 0;
        BlockPos cursor = blockEntity.getBlockPos().below();
        while (count < 32 && blockEntity.getLevel().getBlockState(cursor).is(
                MoreRoadBlocks.SUPPORT_DA31C_POTEAU.get()
        )) {
            count++;
            cursor = cursor.below();
        }
        return count;
    }

    private static boolean hasConnectedD61FootBelow(
            MotorwaySignBlockEntity blockEntity,
            int poleBlocks
    ) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockState(
                        blockEntity.getBlockPos().below(poleBlocks + 1)
                ).is(MoreRoadBlocks.SUPPORT_DA31C_PIED.get());
    }

    /** Remplace les cartouches routiers intégrés aux SVG par les modèles 3D communs. */
    private static void submitOriginalCartouches(
            MotorwaySignRenderState state,
            MotorwaySignPreset preset,
            Font font,
            CustomStackLayout customLayout,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float mainStackRise
    ) {
        float originalShift = !state.mountedOnCrossbar && !customLayout.panels().isEmpty()
                ? customPanelStackHeight(customLayout) + PANEL_GAP - mainStackRise
                : 0.0F;
        float panelForward = state.mountedOnCrossbar
                ? 0.0F
                : MotorwaySignGeometry.D61B_PANEL_FORWARD;

        ExactMappedArtwork artwork = MotorwaySignArtworkCatalog.exactMappedArtwork(preset);
        if (artwork != null) {
            float width = artwork.physicalWidth();
            float height = width * artwork.sourceHeight() / artwork.sourceWidth();
            float left = -width / 2.0F;
            float top = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                    / MotorwaySignGeometry.WORLD_SCALE
                    : 2.05F + height + originalShift;
            for (ExactTextPlacement placement : artwork.texts()) {
                MotorwaySignSlot slot = preset.getSlot(placement.slotIndex());
                if (!isStandaloneRoadCartoucheSlot(
                        preset, artwork, placement.slotIndex()
                )) {
                    continue;
                }
                submitOriginalCartoucheAtInternal(
                        state,
                        safeLine(state.lines, placement.slotIndex(), slot),
                        sourceX(left, width, placement.x(), artwork.sourceWidth()),
                        sourceY(top, height, placement.y(), artwork.sourceHeight()),
                        panelForward,
                        poseStack,
                        collector
                );
            }
            return;
        }

        if (preset == MotorwaySignPreset.D62C) {
            float width = 5.60F;
            float height = 4.60F;
            float left = -width / 2.0F;
            float top = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                    / MotorwaySignGeometry.WORLD_SCALE
                    : 2.05F + height + originalShift;
            submitOriginalCartoucheAtInternal(
                    state, safeLine(state.lines, 0, preset.getSlot(0)),
                    exactX(left, width, 6816.5F), exactY(top, height, 838.5F),
                    panelForward, poseStack, collector
            );
            submitOriginalCartoucheAtInternal(
                    state, safeLine(state.lines, 1, preset.getSlot(1)),
                    exactX(left, width, 10402.0F), exactY(top, height, 832.5F),
                    panelForward, poseStack, collector
            );
            return;
        }

        if (preset == MotorwaySignPreset.D64 || preset == MotorwaySignPreset.D74A) {
            float sourceWidth = preset == MotorwaySignPreset.D64 ? 5342.0F : 5339.0F;
            float sourceHeight = preset == MotorwaySignPreset.D64 ? 2798.0F : 2793.0F;
            float width = sourceWidth / 1000.0F;
            float height = sourceHeight / 1000.0F;
            float left = -width / 2.0F;
            float top = state.mountedOnCrossbar
                    ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                    / MotorwaySignGeometry.WORLD_SCALE
                    : 2.05F + height + originalShift;
            float leftTextX = preset == MotorwaySignPreset.D64 ? 2958.0F : 2957.0F;
            float leftTextY = 834.0F;
            float rightTextX = preset == MotorwaySignPreset.D64 ? 4414.0F : 4413.0F;
            float rightTextY = preset == MotorwaySignPreset.D64 ? 833.5F : 831.5F;
            submitOriginalCartoucheAtInternal(
                    state, safeLine(state.lines, 0, preset.getSlot(0)),
                    sourceX(left, width, leftTextX, sourceWidth),
                    sourceY(top, height, leftTextY, sourceHeight),
                    panelForward, poseStack, collector
            );
            submitOriginalCartoucheAtInternal(
                    state, safeLine(state.lines, 1, preset.getSlot(1)),
                    sourceX(left, width, rightTextX, sourceWidth),
                    sourceY(top, height, rightTextY, sourceHeight),
                    panelForward, poseStack, collector
            );
            return;
        }

        SignLayout layout = buildLayout(font, preset, state.lines);
        float layoutShift = state.mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                / MotorwaySignGeometry.WORLD_SCALE - layout.overallTop()
                : originalShift;
        for (SmallPlate route : layout.routes()) {
            MotorwaySignSlot slot = preset.getSlot(route.index());
            if (!isRoadCartoucheSlot(slot)) {
                continue;
            }
            submitOriginalCartoucheAtInternal(
                    state, route.data(), route.centerX(), route.centerY() + layoutShift,
                    panelForward, poseStack, collector
            );
        }
    }

    private static void submitOriginalCartoucheAtInternal(
            MotorwaySignRenderState state,
            MotorwaySignLineData data,
            float lateralInternal,
            float centerYInternal,
            float panelForward,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        CartoucheType type = cartoucheTypeForColor(data.color());
        float cartoucheBottom = (centerYInternal - D61_CARTOUCHE_HEIGHT / 2.0F)
                * MotorwaySignGeometry.WORLD_SCALE;
        submitCartoucheModel(
                state, type, data.text(), cartoucheBottom,
                cartoucheBottom - 0.05F, panelForward,
                lateralInternal * MotorwaySignGeometry.WORLD_SCALE,
                poseStack, collector
        );
    }

    /**
     * D63c : jusqu'à deux grands cartouches, centrés s'il n'y en a qu'un et
     * répartis symétriquement s'ils sont tous les deux actifs.
     */
    private static void submitD63CCartouches(
            MotorwaySignRenderState state,
            float originalTopInternal,
            float panelForward,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        MotorwaySignPanelData first = state.customPanels.length > 0
                ? state.customPanels[0] : null;
        MotorwaySignPanelData second = state.customPanels.length > 1
                ? state.customPanels[1] : null;
        boolean firstVisible = first != null && first.cartoucheType().isVisible();
        boolean secondVisible = second != null && second.cartoucheType().isVisible();
        if (!firstVisible && !secondVisible) {
            return;
        }

        float cartoucheTopInternal = originalTopInternal + PANEL_GAP + D63C_CARTOUCHE_HEIGHT;
        float cartoucheBottom = (cartoucheTopInternal - D63C_CARTOUCHE_HEIGHT)
                * MotorwaySignGeometry.WORLD_SCALE;
        float panelTop = originalTopInternal * MotorwaySignGeometry.WORLD_SCALE;
        float lateral = firstVisible && secondVisible ? 0.48F : 0.0F;
        float alignedPanelForward = panelForward - D63C_CARTOUCHE_FORWARD_CORRECTION;

        if (firstVisible) {
            submitCartoucheModelScaled(
                    state, first.cartoucheType(), first.cartoucheText(),
                    cartoucheBottom, panelTop, alignedPanelForward,
                    secondVisible ? -lateral : 0.0F,
                    D63C_CARTOUCHE_SCALE,
                    poseStack, collector
            );
        }
        if (secondVisible) {
            submitCartoucheModelScaled(
                    state, second.cartoucheType(), second.cartoucheText(),
                    cartoucheBottom, panelTop, alignedPanelForward,
                    firstVisible ? lateral : 0.0F,
                    D63C_CARTOUCHE_SCALE,
                    poseStack, collector
            );
        }
    }

    private static void submitCustomCartouche(
            MotorwaySignRenderState state,
            MotorwaySignPanelData panel,
            float top,
            float panelForward,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        CartoucheType type = panel.cartoucheType() == null
                ? CartoucheType.NONE
                : panel.cartoucheType();
        if (!type.isVisible()) {
            return;
        }
        float cartoucheBottomInternal = top - D61_CARTOUCHE_HEIGHT;
        float cartoucheBottom = cartoucheBottomInternal * MotorwaySignGeometry.WORLD_SCALE;
        float panelTop = (cartoucheBottomInternal - PANEL_GAP)
                * MotorwaySignGeometry.WORLD_SCALE;
        submitCartoucheModel(
                state, type, panel.cartoucheText(), cartoucheBottom,
                panelTop, panelForward, 0.0F, poseStack, collector
        );
    }

    private static void submitCartoucheModel(
            MotorwaySignRenderState state,
            CartoucheType type,
            String text,
            float cartoucheBottom,
            float panelTop,
            float panelForward,
            float lateral,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitCartoucheModelScaled(
                state, type, text, cartoucheBottom, panelTop, panelForward,
                lateral, CartoucheLayout.MODEL_SCALE, poseStack, collector
        );
    }

    private static void submitCartoucheModelScaled(
            MotorwaySignRenderState state,
            CartoucheType type,
            String text,
            float cartoucheBottom,
            float panelTop,
            float panelForward,
            float lateral,
            float modelScale,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (type == null || !type.isVisible()) {
            return;
        }
        float directionX = state.facing.getStepX();
        float directionZ = state.facing.getStepZ();
        float lateralX = switch (state.facing) {
            case SOUTH -> lateral;
            case NORTH -> -lateral;
            default -> 0.0F;
        };
        float lateralZ = switch (state.facing) {
            case WEST -> lateral;
            case EAST -> -lateral;
            default -> 0.0F;
        };
        float cartoucheForward = panelForward + CARTOUCHE_MODEL_FORWARD_OFFSET;
        float supportForward = panelForward + CARTOUCHE_SUPPORT_FORWARD_OFFSET;

        float supportBottom = panelTop - 0.05F;
        float supportTop = cartoucheBottom
                + (float) CartoucheLayout.CARTOUCHE_RENDER_HEIGHT
                * (modelScale / CartoucheLayout.MODEL_SCALE);
        poseStack.pushPose();
        poseStack.translate(
                lateralX + directionX * supportForward,
                supportBottom,
                lateralZ + directionZ * supportForward
        );
        poseStack.scale(
                1.0F,
                Math.max(0.01F, supportTop - supportBottom),
                1.0F
        );
        state.d61CartoucheSupportModel.submit(
                poseStack, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(
                lateralX + directionX * cartoucheForward,
                0.0F,
                lateralZ + directionZ * cartoucheForward
        );
        poseStack.pushPose();
        poseStack.translate(0.5F, cartoucheBottom, 0.5F);
        poseStack.scale(
                modelScale,
                modelScale,
                modelScale
        );
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        state.cartoucheModels[type.ordinal()].submit(
                poseStack, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0
        );
        poseStack.popPose();

        CartoucheTextRenderer.submit(
                text, type, cartoucheBottom,
                modelScale, state.facing,
                state.lightCoords, poseStack, collector
        );
        poseStack.popPose();
    }

    private static CartoucheType cartoucheTypeForColor(MotorwaySignColor color) {
        return switch (color == null ? MotorwaySignColor.BLUE : color) {
            case GREEN -> CartoucheType.E41_45;
            case RED -> CartoucheType.E42;
            case YELLOW -> CartoucheType.E43;
            case WHITE -> CartoucheType.E44;
            case BLUE, METROPOLITAN_BLUE -> CartoucheType.E47;
            case BLACK, BROWN -> CartoucheType.E44;
        };
    }

    private static boolean isRoadCartoucheSlot(MotorwaySignSlot slot) {
        return slot != null
                && slot.role() == MotorwaySignRole.ROUTE
                && !isExitNumberSlot(slot);
    }

    /**
     * Dans les SVG, un cartouche réellement séparé possède son propre corps
     * étroit au début de la liste. Les numéros intégrés à une grande
     * pancarte restent dans le SVG afin de ne pas casser sa silhouette.
     */
    private static int standaloneRoadCartoucheCount(
            MotorwaySignPreset preset,
            ExactMappedArtwork artwork
    ) {
        int roadSlots = 0;
        for (int slotIndex = 0; slotIndex < preset.getSlotCount(); slotIndex++) {
            if (isRoadCartoucheSlot(preset.getSlot(slotIndex))) {
                roadSlots++;
            }
        }
        int count = 0;
        int maximum = Math.min(roadSlots, artwork.bodies().length);
        while (count < maximum) {
            ExactBody body = artwork.bodies()[count];
            if (body.y() > 2.0F || body.width() >= artwork.sourceWidth() * 0.75F) {
                break;
            }
            count++;
        }
        return count;
    }

    private static boolean isStandaloneRoadCartoucheSlot(
            MotorwaySignPreset preset,
            ExactMappedArtwork artwork,
            int targetSlotIndex
    ) {
        int standaloneCount = standaloneRoadCartoucheCount(preset, artwork);
        int roadOrdinal = 0;
        for (int slotIndex = 0; slotIndex < preset.getSlotCount(); slotIndex++) {
            if (!isRoadCartoucheSlot(preset.getSlot(slotIndex))) {
                continue;
            }
            if (slotIndex == targetSlotIndex) {
                return roadOrdinal < standaloneCount;
            }
            roadOrdinal++;
        }
        return false;
    }

    private static MotorwaySignColor cartoucheColor(CartoucheType type) {
        return switch (type == null ? CartoucheType.NONE : type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E44 -> MotorwaySignColor.WHITE;
            case E47 -> MotorwaySignColor.METROPOLITAN_BLUE;
            default -> MotorwaySignColor.BLUE;
        };
    }

    private static void drawSupport(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            SignLayout layout,
            int light
    ) {
        /* Support central DA31C commun rendu une seule fois dans submit(). */
    }

    /*
     * Visibilité élargie (non private) : réutilisée telle quelle par
     * GenericDirectionalSignBlockEntityRenderer (corps arrondi + listel du
     * panneau directionnel modulable générique), sans dupliquer ce dessin.
     */
    static void drawPlate(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            MotorwaySignColor color,
            int light
    ) {
        drawPlate(collector, poseStack, left, right, bottom, top, color, light,
                panelCornerRadius(left, right, bottom, top));
    }

    /**
     * Variante avec rayon de coin imposé : utilisée quand cette plaque doit
     * se raccorder exactement à un autre corps 3D (ex. le registre principal
     * réduit d'un panneau exact-mappé), pour éviter que les deux rayons
     * indépendants ne dessinent des chants légèrement décalés — visible en
     * biais ou de dos comme un fin bourrelet de texture qui dépasse.
     *
     * Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer.
     */
    static void drawPlate(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            MotorwaySignColor color,
            int light,
            float cornerRadius
    ) {
        float radius = cornerRadius;
        if (radius <= 0.001F) {
            submitBox(collector, poseStack, left, right, bottom, top, BACK_Z, FRONT_Z, PANEL_EDGE, light, -20);
            submitQuad(collector, poseStack,
                    left, right, bottom, top,
                    FACE_Z, panelBorderColor(color), light, -16);
            submitQuad(collector, poseStack,
                    left + LISTEL, right - LISTEL, bottom + LISTEL, top - LISTEL,
                    FACE_Z + 0.001F, color.getArgb(), light, -15);
            return;
        }

        submitRoundedBody(
                collector, poseStack,
                left, right, bottom, top,
                BACK_Z, FRONT_Z, PANEL_EDGE, light, -20
        );
        submitRoundedFace(
                collector, poseStack,
                left, right, bottom, top,
                FACE_Z, panelBorderColor(color), light, -16,
                radius
        );

        float innerLeft = left + LISTEL;
        float innerRight = right - LISTEL;
        float innerBottom = bottom + LISTEL;
        float innerTop = top - LISTEL;
        if (innerRight > innerLeft && innerTop > innerBottom) {
            float innerRadius = clamp(
                    radius - LISTEL,
                    0.0F,
                    panelCornerRadius(innerLeft, innerRight, innerBottom, innerTop)
            );
            submitRoundedFace(
                    collector, poseStack,
                    innerLeft, innerRight, innerBottom, innerTop,
                    FACE_Z + 0.001F, color.getArgb(), light, -15,
                    innerRadius
            );
        }
    }

    private static int panelBorderColor(MotorwaySignColor color) {
        return switch (color) {
            case BLUE, GREEN -> MotorwaySignColor.WHITE.getArgb();
            case WHITE -> MotorwaySignColor.BLACK.getArgb();
            default -> PANEL_EDGE;
        };
    }

    private static MotorwaySignPanelData withoutCartouche(MotorwaySignPanelData panel) {
        if (!panel.cartoucheType().isVisible() && panel.cartoucheText().isBlank()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                panel.background(), CartoucheType.NONE, "", panel.graphic()
        );
    }

    private static void drawPanelText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            PanelLayout panel,
            MotorwaySignGraphic graphic,
            int light
    ) {
        if (panel.indices().isEmpty()) {
            return;
        }
        float lineStep = panel.color().isLight() ? 0.39F : 0.45F;
        float contentHeight = lineStep * panel.indices().size();
        float y = panel.centerY() + contentHeight / 2.0F - lineStep / 2.0F;
        if (usesBottomArrow(graphic)) {
            y += 0.23F;
        }

        float reserve = sideGraphicReserve(graphic);
        float x = textCenterOffset(graphic, reserve);
        float maxWidth = panel.width() - 0.58F - reserve;
        for (int index : panel.indices()) {
            MotorwaySignLineData data = safeLine(values, index, preset.getSlot(index));
            drawText(
                    collector, poseStack, font, data.text(), x, y, maxWidth,
                    data.font(), panel.color().getTextArgb(),
                    panel.color().isLight() ? LIGHT_TEXT_SCALE : DARK_TEXT_SCALE,
                    light
            );
            y -= lineStep;
        }
    }

    private static void drawGraphic(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            SignLayout layout,
            MotorwaySignGraphic graphic,
            int light
    ) {
        if (graphic == MotorwaySignGraphic.NONE || layout.panels().isEmpty()) {
            return;
        }
        PanelLayout panel = layout.panels().getFirst();
        int color = panel.color().getTextArgb();
        float size = Math.min(0.72F, panel.height() * 0.48F);

        switch (graphic) {
            case DOWN -> drawArrow(collector, poseStack, 0.0F, panel.bottom() + 0.31F, 0.0F, -1.0F, 0.48F, color, light);
            case DOWN_DOUBLE -> {
                drawArrow(collector, poseStack, -panel.width() * 0.25F, panel.bottom() + 0.31F, 0.0F, -1.0F, 0.48F, color, light);
                drawArrow(collector, poseStack, panel.width() * 0.25F, panel.bottom() + 0.31F, 0.0F, -1.0F, 0.48F, color, light);
            }
            case DIAGONAL_RIGHT -> drawArrow(
                    collector, poseStack, panel.right() - 0.43F, panel.centerY(), 0.70F, -0.70F, size, color, light
            );
            case EXIT -> drawOverlayTexture(
                    collector, poseStack, EXIT_SYMBOL_TEXTURE,
                    panel.right() - 0.83F, panel.right() - 0.16F,
                    panel.centerY() - 0.38F, panel.centerY() + 0.38F,
                    color, light
            );
            case DIAGONAL_LEFT -> drawArrow(
                    collector, poseStack, panel.left() + 0.43F, panel.centerY(), -0.70F, -0.70F, size, color, light
            );
            case SCHEMATIC_RIGHT -> drawSchematic(collector, poseStack, panel, true, color, light);
            case SCHEMATIC_LEFT -> drawSchematic(collector, poseStack, panel, false, color, light);
            case JUNCTION -> drawJunction(collector, poseStack, panel, color, light);
            case SERVICES -> drawServices(collector, poseStack, panel, light);
            case MOTORWAY -> drawServiceTexture(collector, poseStack, texture("autoroute_logo.png"),
                    panel.left() + 0.16F, panel.left() + 0.78F, panel.centerY() - 0.31F, panel.centerY() + 0.31F, light);
            case EXIT_LIST -> drawExitList(collector, poseStack, panel, color, light);
            default -> {
            }
        }
    }

    private static void drawSchematic(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            PanelLayout panel,
            boolean branchRight,
            int color,
            int light
    ) {
        float x = branchRight ? panel.right() - 0.48F : panel.left() + 0.48F;
        float direction = branchRight ? 1.0F : -1.0F;
        float bottom = panel.bottom() + 0.22F;
        float top = panel.top() - 0.20F;
        submitBar(collector, poseStack, x, bottom, x, top, 0.075F, color, light, -8);
        float joinY = panel.centerY();
        submitBar(collector, poseStack, x, joinY, x + direction * 0.38F, joinY + 0.35F, 0.075F, color, light, -8);
        drawArrow(collector, poseStack, x, top - 0.06F, 0.0F, 1.0F, 0.30F, color, light);
        drawArrow(collector, poseStack, x + direction * 0.38F, joinY + 0.35F, direction, 0.85F, 0.28F, color, light);
    }

    private static void drawJunction(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            PanelLayout panel,
            int color,
            int light
    ) {
        float bottom = panel.bottom() + 0.20F;
        float join = panel.centerY() - 0.08F;
        float top = panel.top() - 0.18F;
        submitBar(collector, poseStack, 0.0F, bottom, 0.0F, join, 0.085F, color, light, -8);
        submitBar(collector, poseStack, 0.0F, join, -0.48F, top - 0.10F, 0.085F, color, light, -8);
        submitBar(collector, poseStack, 0.0F, join, 0.48F, top - 0.10F, 0.085F, color, light, -8);
        drawArrow(collector, poseStack, -0.48F, top - 0.08F, -0.55F, 0.84F, 0.26F, color, light);
        drawArrow(collector, poseStack, 0.48F, top - 0.08F, 0.55F, 0.84F, 0.26F, color, light);
    }

    private static void drawServices(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            PanelLayout panel,
            int light
    ) {
        /*
         * Le SVG de référence (D45/D46/D47 ; D44 a désormais son propre
         * ExactMappedArtwork, voir D44_ARTWORK) ne montre qu'UN seul petit
         * pictogramme, placé devant la 2e ligne (celle en italique type
         * "village étape"), pas une colonne de 3 icônes couvrant toute la
         * hauteur du panneau. On aligne donc l'icône sur la position
         * verticale réelle de cette 2e ligne, avec la même formule que
         * drawPanelText() pour rester cohérent.
         */
        float lineStep = panel.color().isLight() ? 0.39F : 0.45F;
        float size = Math.min(0.34F, lineStep * 0.88F);
        float left = panel.left() + 0.13F;
        float secondLineY = panel.centerY() - lineStep / 2.0F;
        drawServiceTexture(collector, poseStack, SERVICE_TEXTURE_1,
                left, left + size,
                secondLineY - size / 2.0F, secondLineY + size / 2.0F, light);
    }

    private static void drawExitList(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            PanelLayout panel,
            int color,
            int light
    ) {
        float x = panel.left() + 0.24F;
        float y = panel.top() - 0.26F;
        for (int i = 0; i < Math.max(1, panel.indices().size() - 1); i++) {
            submitBar(collector, poseStack, x, y - 0.12F, x, y + 0.12F, 0.045F, color, light, -8);
            submitBar(collector, poseStack, x, y, x + 0.16F, y, 0.045F, color, light, -8);
            y -= 0.38F;
        }
    }

    private static void drawArrow(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float centerX,
            float centerY,
            float dx,
            float dy,
            float length,
            int color,
            int light
    ) {
        float norm = (float) Math.sqrt(dx * dx + dy * dy);
        if (norm <= 0.0001F) {
            return;
        }
        dx /= norm;
        dy /= norm;
        float startX = centerX - dx * length * 0.42F;
        float startY = centerY - dy * length * 0.42F;
        float tipX = centerX + dx * length * 0.48F;
        float tipY = centerY + dy * length * 0.48F;
        submitBar(collector, poseStack, startX, startY, tipX, tipY, 0.080F, color, light, -7);

        float arm = length * 0.32F;
        float px = -dy;
        float py = dx;
        float backX = tipX - dx * arm;
        float backY = tipY - dy * arm;
        submitBar(collector, poseStack, tipX, tipY, backX + px * arm * 0.62F, backY + py * arm * 0.62F, 0.080F, color, light, -7);
        submitBar(collector, poseStack, tipX, tipY, backX - px * arm * 0.62F, backY - py * arm * 0.62F, 0.080F, color, light, -7);
    }

    private static int widestStyledLineWidth(Font font, String value, RoadTextFont roadFont) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String[] lines = value.strip().split("\\R");
        int widest = 0;
        for (String line : lines) {
            String cleaned = line.strip();
            if (!cleaned.isEmpty()) {
                widest = Math.max(widest, font.width(styled(cleaned, roadFont)));
            }
        }
        if (widest == 0) {
            widest = font.width(styled(value.strip(), roadFont));
        }
        return widest;
    }

    private static void drawLeftAlignedText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float leftX,
            float y,
            float maxWidth,
            RoadTextFont roadFont,
            int color,
            float baseScale,
            int light
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        for (String rawLine : value.strip().split("\\R")) {
            String cleaned = rawLine.strip();
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        float widest = 0.0F;
        for (String line : lines) {
            widest = Math.max(widest, trackedTextWidth(font, roadFont, line));
        }
        if (widest <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWidth / widest);
        if (scale <= 0.0F) {
            return;
        }

        float lineAdvance = font.lineHeight * scale;
        float firstCenterY = y - ((lines.size() - 1) * lineAdvance) / 2.0F;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            float lineWidth = trackedTextWidth(font, roadFont, line);
            if (lineWidth <= 0) {
                continue;
            }
            float centerX = leftX + lineWidth * scale / 2.0F;
            float centerY = firstCenterY + index * lineAdvance;
            drawTextLine(collector, poseStack, font, line, centerX, centerY,
                    roadFont, color, scale, light);
        }
    }

    private static void drawTextLine(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String cleaned,
            float x,
            float y,
            RoadTextFont roadFont,
            int color,
            float scale,
            int light
    ) {
        FormattedCharSequence sequence = styled(cleaned, roadFont);
        int width = font.width(sequence);
        if (width <= 0 || scale <= 0.0F) {
            return;
        }

        DeferredTextContext context = DEFERRED_TEXT_CONTEXT.get();
        if (context != null) {
            context.texts.add(new DeferredText(
                    cleaned,
                    x,
                    y + context.yOffsetInternal,
                    roadFont,
                    color,
                    scale,
                    light
            ));
            return;
        }

        float worldScale = MotorwaySignGeometry.WORLD_SCALE;
        float textScaleWorld = scale * worldScale;
        poseStack.pushPose();
        poseStack.translate(
                x * worldScale,
                y * worldScale,
                TEXT_Z * worldScale
        );
        poseStack.scale(textScaleWorld, -textScaleWorld, textScaleWorld);
        submitCenteredTrackedText(collector, poseStack, font, cleaned, roadFont, color, light);
        poseStack.popPose();
    }

    /**
     * Espacement des lettres réel (chaque lettre dessinée et positionnée
     * individuellement, avec un petit écart fixe), comme sur le D21A/D61A :
     * sans lui, les lettres de cette police routière se touchent presque,
     * ce qui les rend difficiles à lire une fois agrandies à la taille du
     * panneau. Dessine dans un poseStack déjà positionné/mis à l'échelle
     * pour que (0,0) soit le centre du texte, comme le faisait l'appel
     * collector.submitText(..., -width/2, -lineHeight/2, ...) remplacé ici.
     */
    private static final float LETTER_TRACKING_PIXELS = 1.2F;

    /**
     * Largeur EXACTE de ce que submitCenteredTrackedText va dessiner (chasse
     * de chaque lettre + écarts de tracking), à utiliser partout où une
     * largeur de texte sert à calculer une échelle ou une position — sinon
     * l'échelle reste basée sur l'ancienne largeur (plus étroite), et le
     * texte réellement dessiné (plus large à cause du tracking) déborde du
     * cadre prévu.
     */
    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer. */
    static float trackedTextWidth(Font font, RoadTextFont roadFont, String value) {
        String safeValue = value == null ? "" : value;
        boolean tracked = RoadTextFont.usesRegulatoryLetterSpacing(roadFont);
        if (!tracked) {
            return font.width(styled(safeValue, roadFont));
        }
        int[] codePoints = safeValue.codePoints().toArray();
        if (codePoints.length <= 1) {
            return font.width(styled(safeValue, roadFont));
        }
        FontDescription.Resource resource = roadFontResource(roadFont);
        float total = LETTER_TRACKING_PIXELS * (codePoints.length - 1);
        for (int codePoint : codePoints) {
            FormattedCharSequence single = Component.literal(new String(Character.toChars(codePoint)))
                    .withStyle(Style.EMPTY.withFont(resource))
                    .getVisualOrderText();
            total += font.getSplitter().stringWidth(single);
        }
        return total;
    }

    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer. */
    static void submitCenteredTrackedText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            RoadTextFont roadFont,
            int color,
            int light
    ) {
        String safeValue = value == null ? "" : value;
        boolean tracked = RoadTextFont.usesRegulatoryLetterSpacing(roadFont);
        int[] codePoints = tracked ? safeValue.codePoints().toArray() : null;
        if (!tracked || codePoints.length <= 1) {
            FormattedCharSequence sequence = styled(safeValue, roadFont);
            int width = font.width(sequence);
            collector.submitText(
                    poseStack, -width / 2.0F, -font.lineHeight / 2.0F, sequence, false,
                    Font.DisplayMode.NORMAL, light, color, 0x00000000, 0x00000000
            );
            return;
        }

        FontDescription.Resource resource = roadFontResource(roadFont);
        float[] charWidths = new float[codePoints.length];
        FormattedCharSequence[] sequences = new FormattedCharSequence[codePoints.length];
        for (int index = 0; index < codePoints.length; index++) {
            sequences[index] = Component.literal(new String(Character.toChars(codePoints[index])))
                    .withStyle(Style.EMPTY.withFont(resource))
                    .getVisualOrderText();
            charWidths[index] = font.getSplitter().stringWidth(sequences[index]);
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

        float cursor = -totalWidth / 2.0F;
        float textY = -font.lineHeight / 2.0F;
        for (int index = 0; index < codePoints.length; index++) {
            collector.submitText(
                    poseStack, cursor, textY, sequences[index], false,
                    Font.DisplayMode.NORMAL, light, color, 0x00000000, 0x00000000
            );
            if (index < advances.length) {
                cursor += advances[index];
            }
        }
    }

    private static void drawText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float x,
            float y,
            float maxWidth,
            RoadTextFont roadFont,
            int color,
            float baseScale,
            int light
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        String cleaned = value.strip();
        float width = trackedTextWidth(font, roadFont, cleaned);
        if (width <= 0) {
            return;
        }

        float scale = Math.min(baseScale, maxWidth / width);
        if (scale <= 0.0F) {
            return;
        }

        /*
         * Les textes D/DA sont collectés puis envoyés après la géométrie du
         * panneau. Le rendu final passe par CartoucheTextRenderer afin de
         * partager strictement le même chemin entityCutout que les textes des
         * cartouches, qui sont visibles avec Iris/Complementary.
         */
        drawTextLine(collector, poseStack, font, cleaned, x, y,
                roadFont, color, scale, light);
    }

    private static void addDeferredTextYOffset(float deltaInternal) {
        DeferredTextContext context = DEFERRED_TEXT_CONTEXT.get();
        if (context != null) {
            context.yOffsetInternal += deltaInternal;
        }
    }

    /**
     * Rendu texte compatible shaders calé sur le fonctionnement de More Road V9.0.
     *
     * Dans le JAR V9.0 (Minecraft 26.2 / NeoForge 26.2), les textes D21/D61 qui
     * fonctionnent avec Complementary sont soumis avec Font.DisplayMode.NORMAL
     * depuis le PoseStack RACINE du BlockEntityRenderer. Ils ne passent jamais
     * dans une matrice globale réduite comme WORLD_SCALE.
     *
     * Le renderer autoroutier paramétrique garde WORLD_SCALE pour sa géométrie,
     * mais les textes sont donc différés puis reconstruits ici en unités monde,
     * après le popPose() de cette géométrie. On retrouve exactement la forme de
     * matrice du renderer V9.0 : centre bloc -> rotation -> position face ->
     * petite échelle de police -> submitText NORMAL.
     */
    private static void flushDeferredTexts(
            MotorwaySignRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        DeferredTextContext context = DEFERRED_TEXT_CONTEXT.get();
        DEFERRED_TEXT_CONTEXT.remove();
        if (context == null || context.texts.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        float worldScale = MotorwaySignGeometry.WORLD_SCALE;
        float textDepthWorld = context.panelForward + TEXT_Z * worldScale;

        for (DeferredText deferred : context.texts) {
            FormattedCharSequence sequence = styled(
                    deferred.value(),
                    deferred.roadFont()
            );
            int width = font.width(sequence);
            if (width <= 0) {
                continue;
            }

            float textScaleWorld = deferred.scaleInternal() * worldScale;
            if (textScaleWorld <= 0.0F) {
                continue;
            }

            poseStack.pushPose();

            // Même repère racine que les anciens D21A/D61A de More Road V9.0.
            poseStack.translate(
                    0.5F,
                    deferred.yInternal() * worldScale,
                    0.5F
            );
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(getFacingRotation(context.facing))
            );
            poseStack.translate(
                    deferred.xInternal() * worldScale,
                    0.0F,
                    textDepthWorld
            );
            poseStack.scale(
                    textScaleWorld,
                    -textScaleWorld,
                    textScaleWorld
            );

            submitCenteredTrackedText(
                    collector, poseStack, font, deferred.value(), deferred.roadFont(),
                    deferred.color(), deferred.light()
            );

            poseStack.popPose();
        }
    }

    private static boolean isExitNumberSlot(MotorwaySignSlot slot) {
        return slot.role() == MotorwaySignRole.ROUTE
                && slot.label().toLowerCase(Locale.ROOT).contains("sortie");
    }

    private static String exitNumber(String value) {
        if (value == null) {
            return "";
        }
        String stripped = value.strip();
        String upper = stripped.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SORTIE")) {
            stripped = stripped.substring(Math.min(6, stripped.length())).strip();
        }
        return stripped;
    }

    /** Cartouche de sortie : symbole réglementaire conservé à gauche du numéro modifiable. */
    private static void drawExitNumber(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String value,
            float x,
            float y,
            float maxWidth,
            RoadTextFont roadFont,
            int color,
            float baseScale,
            int light
    ) {
        String number = exitNumber(value);
        if (number.isBlank()) {
            return;
        }
        float pixelWidth = trackedTextWidth(font, roadFont, number);
        if (pixelWidth <= 0) {
            return;
        }

        float nominalHeight = font.lineHeight * baseScale;
        float iconWidth = nominalHeight * 1.06F;
        float gap = nominalHeight * 0.16F;
        float scale = Math.min(baseScale, (maxWidth - iconWidth - gap) / pixelWidth);
        if (scale <= 0.0F) {
            return;
        }
        float textWidth = pixelWidth * scale;
        float totalWidth = iconWidth + gap + textWidth;
        float iconCenter = x - totalWidth / 2.0F + iconWidth / 2.0F;
        float textCenter = x + totalWidth / 2.0F - textWidth / 2.0F;
        float iconHeight = nominalHeight * 0.88F;
        float iconRenderWidth = iconHeight * 1.20F;
        drawOverlayTexture(
                collector,
                poseStack,
                EXIT_SYMBOL_TEXTURE,
                iconCenter - iconRenderWidth / 2.0F,
                iconCenter + iconRenderWidth / 2.0F,
                y - iconHeight / 2.0F,
                y + iconHeight / 2.0F,
                color,
                light
        );
        drawText(collector, poseStack, font, number, textCenter,
                y - nominalHeight * 0.06F, textWidth + 0.001F,
                roadFont, color, scale, light);
    }

    private static void drawOverlayTexture(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            int color,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, TEXT_Z - 0.001F);
        collector.order(-6).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFrontQuad(pose, consumer, left, right, bottom, top, 0.0F, color, light)
        );
        poseStack.popPose();
    }

    private static void drawServiceTexture(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, TEXT_Z - 0.001F);
        collector.order(-9).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFrontQuad(pose, consumer, left, right, bottom, top, 0.0F, 0xFFFFFFFF, light)
        );
        poseStack.popPose();
    }

    private static void drawArtworkLayer(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            int order
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFrontQuad(
                        pose, consumer, left, right, bottom, top, 0.0F, color, light
                )
        );
        poseStack.popPose();
    }

    /**
     * Variante de drawArtworkLayer qui n'échantillonne qu'une bande
     * verticale de la texture (vTop/vBottom, 0 = haut de l'image, 1 = bas),
     * étirée sur un quad monde plus petit — utilisée pour ne garder que la
     * partie du cadre AU-DESSUS d'un registre redessiné en plus petit
     * (drawNormalizedMainDestinationStack), sans afficher l'ancien registre
     * pleine taille en dessous.
     */
    private static void drawArtworkLayerCroppedV(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float left,
            float right,
            float worldBottom,
            float worldTop,
            float vTop,
            float vBottom,
            float z,
            int color,
            int light,
            int order
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addFrontQuadUv(
                        pose, consumer, left, right, worldBottom, worldTop, 0.0F, vTop, vBottom, color, light
                )
        );
        poseStack.popPose();
    }

    /**
     * Variante 2D de drawArtworkLayerCroppedV : ne garde qu'un petit
     * rectangle [uLeft, uRight] x [vTop, vBottom] de la texture, étiré sur un
     * quad monde indépendant de la position/taille d'origine — utilisée pour
     * agrandir en place un motif ponctuel (ex. le cartouche de numéro de
     * route) sans reprendre le calque en entier.
     */
    private static void drawArtworkLayerCropped2D(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Identifier texture,
            float worldLeft,
            float worldRight,
            float worldBottom,
            float worldTop,
            float uLeft,
            float uRight,
            float vTop,
            float vBottom,
            float z,
            int color,
            int light,
            int order
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> addQuadUv(
                        pose, consumer, worldLeft, worldRight, worldBottom, worldTop, 0.0F,
                        uLeft, uRight, vTop, vBottom, color, light
                )
        );
        poseStack.popPose();
    }

    private static void submitBar(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            int color,
            int light,
            int order
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }
        float px = -dy / length * thickness / 2.0F;
        float py = dx / length * thickness / 2.0F;
        submitFlatQuad(
                collector, poseStack,
                x1 + px, y1 + py,
                x2 + px, y2 + py,
                x2 - px, y2 - py,
                x1 - px, y1 - py,
                TEXT_Z - 0.001F, color, light, order
        );
    }

    private static void submitQuad(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int color,
            int light,
            int order
    ) {
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_TEXTURE),
                (pose, consumer) -> addFrontQuad(pose, consumer, left, right, bottom, top, z, color, light)
        );
    }

    private static void submitFlatQuad(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            float z,
            int color,
            int light,
            int order
    ) {
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SOLID_TEXTURE),
                (pose, consumer) -> {
                    addVertex(pose, consumer, x1, y1, z, 0.0F, 1.0F, color, light, 0.0F, 0.0F, 1.0F);
                    addVertex(pose, consumer, x2, y2, z, 1.0F, 1.0F, color, light, 0.0F, 0.0F, 1.0F);
                    addVertex(pose, consumer, x3, y3, z, 1.0F, 0.0F, color, light, 0.0F, 0.0F, 1.0F);
                    addVertex(pose, consumer, x4, y4, z, 0.0F, 0.0F, color, light, 0.0F, 0.0F, 1.0F);
                }
        );
    }

    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer (poteau). */
    static void submitBox(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            float back,
            float front,
            int color,
            int light,
            int order
    ) {
        boolean panelBody = color == PANEL_EDGE;
        boolean metalBody = panelBody || color == SUPPORT_COLOR;
        Identifier boxTexture = metalBody ? PANEL_METAL_TEXTURE : SOLID_TEXTURE;
        int boxColor = metalBody ? 0xFFFFFFFF : color;
        collector.order(order).submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(boxTexture),
                (pose, consumer) -> {
                    addFrontQuad(pose, consumer, left, right, bottom, top, front, boxColor, light);
                    addVertex(pose, consumer, right, bottom, back, 0, 1, boxColor, light, 0, 0, -1);
                    addVertex(pose, consumer, left, bottom, back, 1, 1, boxColor, light, 0, 0, -1);
                    addVertex(pose, consumer, left, top, back, 1, 0, boxColor, light, 0, 0, -1);
                    addVertex(pose, consumer, right, top, back, 0, 0, boxColor, light, 0, 0, -1);

                    addFace(pose, consumer, left, bottom, back, left, bottom, front, left, top, front, left, top, back, boxColor, light, -1, 0, 0);
                    addFace(pose, consumer, right, bottom, front, right, bottom, back, right, top, back, right, top, front, boxColor, light, 1, 0, 0);
                    addFace(pose, consumer, left, top, front, right, top, front, right, top, back, left, top, back, boxColor, light, 0, 1, 0);
                    addFace(pose, consumer, left, bottom, back, right, bottom, back, right, bottom, front, left, bottom, front, boxColor, light, 0, -1, 0);
                }
        );
        if (panelBody) {
            collector.order(order + 1).submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityCutout(SOLID_TEXTURE),
                    (pose, consumer) -> addFrontQuad(
                            pose, consumer, left, right, bottom, top,
                            front + 0.0005F, PANEL_EDGE, light
                    )
            );
        }
    }

    private static void addFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int color,
            int light,
            float nx, float ny, float nz
    ) {
        addVertex(pose, consumer, x1, y1, z1, 0, 1, color, light, nx, ny, nz);
        addVertex(pose, consumer, x2, y2, z2, 1, 1, color, light, nx, ny, nz);
        addVertex(pose, consumer, x3, y3, z3, 1, 0, color, light, nx, ny, nz);
        addVertex(pose, consumer, x4, y4, z4, 0, 0, color, light, nx, ny, nz);
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
            int light
    ) {
        addVertex(pose, consumer, left, bottom, z, 0, 1, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, bottom, z, 1, 1, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, top, z, 1, 0, color, light, 0, 0, 1);
        addVertex(pose, consumer, left, top, z, 0, 0, color, light, 0, 0, 1);
    }

    /** Comme addFrontQuad, mais n'échantillonne que la bande [vTop, vBottom] de la texture (0 = haut, 1 = bas). */
    private static void addFrontQuadUv(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            float vTop,
            float vBottom,
            int color,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, z, 0, vBottom, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, bottom, z, 1, vBottom, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, top, z, 1, vTop, color, light, 0, 0, 1);
        addVertex(pose, consumer, left, top, z, 0, vTop, color, light, 0, 0, 1);
    }

    /** Comme addFrontQuadUv, mais recadre aussi horizontalement ([uLeft, uRight]). */
    private static void addQuadUv(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            float uLeft,
            float uRight,
            float vTop,
            float vBottom,
            int color,
            int light
    ) {
        addVertex(pose, consumer, left, bottom, z, uLeft, vBottom, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, bottom, z, uRight, vBottom, color, light, 0, 0, 1);
        addVertex(pose, consumer, right, top, z, uRight, vTop, color, light, 0, 0, 1);
        addVertex(pose, consumer, left, top, z, uLeft, vTop, color, light, 0, 0, 1);
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
            int light,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static MotorwaySignLineData safeLine(
            MotorwaySignLineData[] values,
            int index,
            MotorwaySignSlot fallback
    ) {
        return values != null && index >= 0 && index < values.length && values[index] != null
                ? values[index]
                : MotorwaySignLineData.blankForSlot(fallback);
    }

    private static float renderedTextWidth(Font font, MotorwaySignLineData line) {
        float pixels = trackedTextWidth(font, line.font(), line.text());
        float scale = line.color().isLight() ? LIGHT_TEXT_SCALE : DARK_TEXT_SCALE;
        return Math.max(0.0F, pixels * scale);
    }

    private static FormattedCharSequence styled(String value, RoadTextFont font) {
        return Component.literal(value == null ? "" : value)
                .withStyle(Style.EMPTY.withFont(roadFontResource(font)))
                .getVisualOrderText();
    }

    private static float sideGraphicReserve(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case DIAGONAL_LEFT, DIAGONAL_RIGHT, EXIT -> 0.82F;
            case SCHEMATIC_LEFT, SCHEMATIC_RIGHT -> 1.02F;
            case SERVICES, MOTORWAY -> 0.55F;
            case EXIT_LIST -> 0.36F;
            default -> 0.0F;
        };
    }

    private static float textCenterOffset(MotorwaySignGraphic graphic, float reserve) {
        return switch (graphic) {
            case DIAGONAL_RIGHT, SCHEMATIC_RIGHT, EXIT -> -reserve / 2.0F;
            case DIAGONAL_LEFT, SCHEMATIC_LEFT -> reserve / 2.0F;
            case SERVICES, MOTORWAY, EXIT_LIST -> reserve / 2.0F;
            default -> 0.0F;
        };
    }

    private static boolean usesBottomArrow(MotorwaySignGraphic graphic) {
        return graphic == MotorwaySignGraphic.DOWN || graphic == MotorwaySignGraphic.DOWN_DOUBLE;
    }

    /* Visibilité élargie : réutilisée par GenericDirectionalSignBlockEntityRenderer. */
    static float getFacingRotation(Direction facing) {
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /* Package-private (pas private) : MotorwaySignArtworkCatalog les réutilise pour ses propres identifiants. */
    static Identifier texture(String filename) {
        return Identifier.fromNamespaceAndPath(MoreRoad.MODID, "textures/block/" + filename);
    }

    static Identifier artwork(String filename) {
        return Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/block/motorway_sign/" + filename
        );
    }

    private record PanelLayout(
            float left,
            float right,
            float bottom,
            float top,
            MotorwaySignColor color,
            List<Integer> indices
    ) {
        float width() {
            return this.right - this.left;
        }

        float height() {
            return this.top - this.bottom;
        }

        float centerY() {
            return (this.bottom + this.top) / 2.0F;
        }
    }

    private record SmallPlate(
            float left,
            float right,
            float bottom,
            float top,
            int index,
            MotorwaySignLineData data
    ) {
        float centerX() {
            return (this.left + this.right) / 2.0F;
        }

        float centerY() {
            return (this.bottom + this.top) / 2.0F;
        }
    }

    private record SignLayout(
            List<PanelLayout> panels,
            List<SmallPlate> routes,
            SmallPlate distance,
            float sharedWidth,
            float overallBottom,
            float overallTop
    ) {
    }

    private record CustomStackLayout(
            List<MotorwaySignPanelData> panels,
            float[] widths,
            float[] heights,
            float[] cartoucheWidths,
            float maximumWidth,
            float totalHeight
    ) {
    }
}
