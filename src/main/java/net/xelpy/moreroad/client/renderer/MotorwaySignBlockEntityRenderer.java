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

    private static final ExactMappedArtwork D61B_ARTWORK = new ExactMappedArtwork(
            15788.0F, 7879.0F, 6.20F, artwork("d61b_frame.png"), null,
            new ExactTintedLayer[]{
                    layer("d61b_route.png", 0), layer("d61b_panel_top.png", 1), layer("d61b_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 7889.5F, 832.5F, 3000.0F, 1095.0F),
                    text(1, 4900.0F, 3164.0F, 9000.0F, 1366.0F),
                    text(2, 13500.0F, 3164.0F, 3000.0F, 1366.0F),
                    text(3, 4900.0F, 6429.5F, 9000.0F, 1367.0F),
                    text(4, 13500.0F, 6429.5F, 3000.0F, 1367.0F)
            },
            new ExactBody[]{
                    body(6229.0F, 0.0F, 3332.0F, 1669.0F),
                    body(0.0F, 1714.0F, 15788.0F, 2902.0F),
                    body(0.0F, 4976.0F, 15788.0F, 2903.0F)
            }, true
    );

    private static final ExactMappedArtwork D62A_ARTWORK = new ExactMappedArtwork(
            11537.0F, 10619.0F, 5.60F, artwork("d62a_frame.png"), null,
            new ExactTintedLayer[]{
                    layer("d62a_route.png", 0), layer("d62a_panel_top.png", 1), layer("d62a_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 5771.0F, 831.5F, 3000.0F, 1067.0F),
                    text(1, 5767.5F, 3760.0F, 10300.0F, 1360.0F),
                    text(2, 5767.5F, 5850.0F, 10300.0F, 1360.0F),
                    text(3, 5770.5F, 9466.5F, 10500.0F, 1185.0F)
            },
            new ExactBody[]{
                    body(4107.0F, 0.0F, 3326.0F, 1665.0F),
                    body(0.0F, 1974.0F, 11537.0F, 5889.0F),
                    body(0.0F, 8220.0F, 11537.0F, 2399.0F)
            }, true
    );

    private static final ExactMappedArtwork D62B_ARTWORK = new ExactMappedArtwork(
            13203.0F, 8129.0F, 5.80F, artwork("d62b_frame.png"), null,
            new ExactTintedLayer[]{
                    layer("d62b_route_left.png", 0), layer("d62b_route_right.png", 1),
                    layer("d62b_panel_top.png", 2), layer("d62b_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 4810.0F, 838.5F, 3000.0F, 1079.0F),
                    text(1, 8398.0F, 832.5F, 3000.0F, 1067.0F),
                    text(2, 6611.0F, 3420.5F, 12000.0F, 1359.0F),
                    text(3, 3995.5F, 6677.5F, 7600.0F, 1331.0F)
            },
            new ExactBody[]{
                    body(3149.0F, 0.0F, 3326.0F, 1663.0F),
                    body(6735.0F, 0.0F, 3325.0F, 1663.0F),
                    body(0.0F, 1975.0F, 13203.0F, 2901.0F),
                    body(0.0F, 5228.0F, 13203.0F, 2901.0F)
            }, true
    );

    private static final ExactMappedArtwork D62D_TOP_ARTWORK = new ExactMappedArtwork(
            16204.0F, 12987.0F, 5.80F, artwork("d62d_top_frame.png"), artwork("d62d_top_graphics.png"),
            new ExactTintedLayer[]{
                    layer("d62d_top_route_left.png", 0), layer("d62d_top_route_right.png", 1),
                    layer("d62d_top_panel_top.png", 2), layer("d62d_top_panel_bottom.png", 4)
            },
            new ExactTextPlacement[]{
                    text(0, 6803.0F, 843.5F, 3000.0F, 1083.0F),
                    text(1, 9902.5F, 836.5F, 2100.0F, 1069.0F),
                    text(2, 8102.0F, 3150.0F, 14500.0F, 1369.0F),
                    text(3, 8102.0F, 5000.0F, 14500.0F, 1369.0F),
                    text(4, 8109.0F, 7801.5F, 10000.0F, 1369.0F)
            },
            new ExactBody[]{
                    body(5138.0F, 0.0F, 3334.0F, 1670.0F),
                    body(8734.0F, 1.0F, 2336.0F, 1669.0F),
                    body(0.0F, 1715.0F, 16204.0F, 4539.0F),
                    body(0.0F, 6615.0F, 16204.0F, 6372.0F)
            }, true
    );

    private static final ExactMappedArtwork D62D_BOTTOM_ARTWORK = new ExactMappedArtwork(
            16204.0F, 8215.0F, 5.80F, artwork("d62d_bottom_frame.png"), artwork("d62d_bottom_graphics.png"),
            new ExactTintedLayer[]{
                    layer("d62d_bottom_route.png", 0), layer("d62d_bottom_panel.png", 1)
            },
            new ExactTextPlacement[]{
                    text(0, 8102.0F, 834.5F, 3100.0F, 1081.0F),
                    text(1, 8103.0F, 3027.0F, 9000.0F, 1338.0F)
            },
            new ExactBody[]{
                    body(6435.0F, 0.0F, 3334.0F, 1669.0F),
                    body(0.0F, 1843.0F, 16204.0F, 6372.0F)
            }, true
    );

    private static final ExactMappedArtwork D63C_ARTWORK = new ExactMappedArtwork(
            13403.0F, 9935.0F, 6.00F, artwork("d63c_frame.png"), artwork("d63c_graphics.png"),
            new ExactTintedLayer[]{
                    layer("d63c_panel_top.png", 0), layer("d63c_panel_middle.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 3068.5F, 2032.5F, 3900.0F, 1095.0F),
                    text(1, 11480.0F, 1953.0F, 3100.0F, 1550.0F),
                    text(2, 6396.0F, 5995.0F, 12000.0F, 1486.0F),
                    text(3, 6704.0F, 9025.0F, 12400.0F, 1348.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 13403.0F, 3905.0F),
                    body(0.0F, 4267.0F, 13403.0F, 2905.0F),
                    body(0.0F, 7530.0F, 13403.0F, 2405.0F)
            }, true
    );

    private static final ExactMappedArtwork D63D_ARTWORK = new ExactMappedArtwork(
            12370.0F, 11469.0F, 5.80F, artwork("d63d_frame.png"), null,
            new ExactTintedLayer[]{
                    layer("d63d_panel_top.png", 1), layer("d63d_route.png", 0),
                    layer("d63d_panel_middle.png", 2), layer("d63d_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 2675.5F, 1592.0F, 3500.0F, 1082.0F),
                    text(1, 10888.0F, 1580.5F, 1500.0F, 1083.0F),
                    text(2, 6188.5F, 4719.5F, 11200.0F, 1363.0F),
                    text(3, 5363.0F, 8110.0F, 10000.0F, 1350.0F),
                    text(4, 5363.0F, 9810.0F, 10000.0F, 1350.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 12370.0F, 2905.0F),
                    body(0.0F, 3268.0F, 12370.0F, 2905.0F),
                    body(0.0F, 6531.0F, 12370.0F, 4938.0F)
            }, true
    );

    private static final ExactMappedArtwork D71_ARTWORK = new ExactMappedArtwork(
            13213.0F, 6434.0F, 5.60F, artwork("d71_frame.png"), null,
            new ExactTintedLayer[]{layer("d71_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 6603.0F, 1291.0F, 12000.0F, 850.0F),
                    text(1, 6020.0F, 3356.0F, 11200.0F, 1160.0F),
                    text(2, 5910.0F, 5349.0F, 11200.0F, 850.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 13213.0F, 6434.0F)}, true
    );

    private static final ExactMappedArtwork D72_ARTWORK = new ExactMappedArtwork(
            12012.0F, 14926.0F, 4.40F, artwork("d72_frame.png"), artwork("d72_graphics.png"),
            new ExactTintedLayer[]{layer("d72_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 3752.0F, 1039.0F, 6900.0F, 800.0F),
                    text(1, 6083.0F, 2711.0F, 10500.0F, 1125.0F),
                    text(2, 3207.0F, 5334.0F, 3900.0F, 1100.0F),
                    text(3, 3204.0F, 7974.0F, 3900.0F, 1100.0F),
                    text(4, 3204.0F, 10584.0F, 3900.0F, 1100.0F),
                    text(5, 3201.0F, 13202.0F, 3900.0F, 1100.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 12012.0F, 14926.0F)}, false
    );

    private static final ExactMappedArtwork D73_ARTWORK = new ExactMappedArtwork(
            6037.0F, 6596.0F, 3.80F, artwork("d73_frame.png"), artwork("d73_graphics.png"),
            new ExactTintedLayer[]{layer("d73_panel_top.png", 0), layer("d73_panel_bottom.png", 1)},
            new ExactTextPlacement[]{
                    text(0, 3014.5F, 1931.5F, 4100.0F, 1083.0F),
                    text(1, 3062.0F, 5408.5F, 5500.0F, 1093.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 6037.0F, 3861.0F),
                    body(0.0F, 4217.0F, 6037.0F, 2379.0F)
            }, false
    );

    private static final ExactMappedArtwork DA31A_ARTWORK = new ExactMappedArtwork(
            14843.0F, 8662.0F, 5.90F, artwork("da31a_frame.png"), artwork("da31a_graphics.png"),
            new ExactTintedLayer[]{layer("da31a_panel_top.png", 0), layer("da31a_panel_bottom.png", 1)},
            new ExactTextPlacement[]{
                    text(0, 7422.0F, 1419.5F, 8000.0F, 1335.0F),
                    text(1, 7422.0F, 4368.0F, 14000.0F, 1075.0F),
                    text(2, 2999.0F, 6968.0F, 4000.0F, 1075.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 14843.0F, 2836.0F),
                    body(0.0F, 3190.0F, 14843.0F, 5472.0F)
            }, true
    );

    private static final ExactMappedArtwork DA31B_ARTWORK = new ExactMappedArtwork(
            14494.0F, 13776.0F, 5.80F, artwork("da31b_frame.png"), artwork("da31b_graphics.png"),
            new ExactTintedLayer[]{
                    layer("da31b_route.png", 0), layer("da31b_panel_top.png", 1), layer("da31b_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 7243.0F, 830.0F, 3000.0F, 1092.0F),
                    text(1, 7248.0F, 3227.0F, 13000.0F, 1364.0F),
                    text(2, 7255.0F, 4988.0F, 13000.0F, 1364.0F),
                    text(3, 7246.0F, 7790.0F, 13000.0F, 1089.0F),
                    text(4, 7246.0F, 9393.0F, 13000.0F, 1089.0F)
            },
            new ExactBody[]{
                    body(5586.0F, 0.0F, 3326.0F, 1663.0F),
                    body(0.0F, 1974.0F, 14494.0F, 4259.0F),
                    body(0.0F, 6592.0F, 14494.0F, 7184.0F)
            }, true
    );

    private static final ExactMappedArtwork DA31D_ARTWORK = new ExactMappedArtwork(
            14336.0F, 10470.0F, 5.80F, artwork("da31d_frame.png"), artwork("da31d_graphics.png"),
            new ExactTintedLayer[]{layer("da31d_panel_top.png", 0), layer("da31d_panel_bottom.png", 1)},
            new ExactTextPlacement[]{
                    text(0, 7175.0F, 1453.0F, 13200.0F, 1366.0F),
                    text(1, 7173.0F, 4339.0F, 13200.0F, 1097.0F),
                    text(2, 7179.0F, 6059.0F, 7000.0F, 1096.0F),
                    text(3, 3067.0F, 8731.0F, 4000.0F, 1099.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 14336.0F, 2905.0F),
                    body(0.0F, 3266.0F, 14336.0F, 7204.0F)
            }, true
    );

    private static final ExactMappedArtwork DA31E_ARTWORK = new ExactMappedArtwork(
            13331.0F, 10792.0F, 5.60F, artwork("da31e_frame.png"), artwork("da31e_graphics.png"),
            new ExactTintedLayer[]{
                    layer("da31e_route.png", 0), layer("da31e_panel_top.png", 1), layer("da31e_panel_bottom.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 6665.0F, 827.0F, 3900.0F, 1062.0F),
                    text(1, 6667.0F, 3472.0F, 12200.0F, 1476.0F),
                    text(2, 6666.0F, 6289.0F, 12200.0F, 1092.0F)
            },
            new ExactBody[]{
                    body(4511.0F, 0.0F, 4311.0F, 1659.0F),
                    body(0.0F, 1970.0F, 13331.0F, 2891.0F),
                    body(0.0F, 5217.0F, 13331.0F, 5575.0F)
            }, true
    );

    private static final ExactMappedArtwork DA31F_ARTWORK = new ExactMappedArtwork(
            9971.0F, 14281.0F, 4.80F, artwork("da31f_frame.png"), artwork("da31f_graphics.png"),
            new ExactTintedLayer[]{
                    layer("da31f_route.png", 0), layer("da31f_panel_top.png", 1), layer("da31f_panel_bottom.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 4988.0F, 847.0F, 3000.0F, 1090.0F),
                    text(1, 4984.0F, 3459.0F, 9000.0F, 1378.0F),
                    text(2, 4986.0F, 6769.0F, 8500.0F, 1373.0F),
                    text(3, 4982.0F, 8775.0F, 8500.0F, 1346.0F)
            },
            new ExactBody[]{
                    body(3307.0F, 0.0F, 3357.0F, 1681.0F),
                    body(0.0F, 1995.0F, 9971.0F, 2924.0F),
                    body(0.0F, 5283.0F, 9971.0F, 8998.0F)
            }, true
    );

    private static final ExactMappedArtwork DA32A_ARTWORK = new ExactMappedArtwork(
            9909.0F, 7208.0F, 4.60F, artwork("da32a_frame.png"), artwork("da32a_graphics.png"),
            new ExactTintedLayer[]{layer("da32a_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 5078.0F, 1214.0F, 8600.0F, 1090.0F),
                    text(1, 4962.0F, 2807.0F, 9000.0F, 1093.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 9909.0F, 7208.0F)}, true
    );

    private static final ExactMappedArtwork DA32A_DC_ARTWORK = new ExactMappedArtwork(
            9909.0F, 7208.0F, 4.60F, artwork("da32a_dc_frame.png"), artwork("da32a_dc_graphics.png"),
            new ExactTintedLayer[]{layer("da32a_dc_panel.png", 0)},
            DA32A_ARTWORK.texts(), DA32A_ARTWORK.bodies(), true
    );

    private static final ExactMappedArtwork DA32B_ARTWORK = new ExactMappedArtwork(
            9909.0F, 8942.0F, 4.60F, artwork("da32b_frame.png"), artwork("da32b_graphics.png"),
            new ExactTintedLayer[]{layer("da32b_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 4954.0F, 1484.0F, 8800.0F, 1374.0F),
                    text(1, 4962.0F, 3475.0F, 9000.0F, 1374.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 9909.0F, 8942.0F)}, true
    );

    private static final ExactMappedArtwork DA32B_DC_ARTWORK = new ExactMappedArtwork(
            9909.0F, 8942.0F, 4.60F, artwork("da32b_dc_frame.png"), artwork("da32b_dc_graphics.png"),
            new ExactTintedLayer[]{layer("da32b_dc_panel.png", 0)},
            DA32B_ARTWORK.texts(), DA32B_ARTWORK.bodies(), true
    );

    private static final ExactMappedArtwork D31B_EX1_ARTWORK = new ExactMappedArtwork(
            7832.0F, 4993.0F, 4.20F, artwork("d31b_ex1_frame.png"), artwork("d31b_ex1_graphics.png"),
            new ExactTintedLayer[]{layer("d31b_ex1_route.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 1781.5F, 860.0F, 2400.0F, 641.0F),
                    text(1, 3916.0F, 3045.0F, 7000.0F, 640.0F),
                    text(2, 3916.0F, 3970.0F, 7000.0F, 682.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 7832.0F, 2349.0F),
                    body(0.0F, 2644.0F, 7832.0F, 2349.0F)
            }, true
    );

    private static final ExactMappedArtwork D31B_EX2_ARTWORK = new ExactMappedArtwork(
            15397.0F, 11745.0F, 6.00F, artwork("d31b_ex2_frame.png"), artwork("d31b_ex2_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31b_ex2_panel_top.png", MotorwaySignColor.WHITE),
                    layer("d31b_ex2_route.png", 0), layer("d31b_ex2_panel_bottom.png", 1)
            },
            new ExactTextPlacement[]{
                    text(0, 1952.5F, 1783.5F, 2300.0F, 969.0F),
                    text(1, 7693.5F, 6500.0F, 13800.0F, 1450.0F),
                    text(2, 7693.5F, 8500.0F, 13800.0F, 1450.0F),
                    text(3, 7693.5F, 10500.0F, 13800.0F, 1450.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 15397.0F, 4535.0F),
                    body(0.0F, 4885.0F, 15397.0F, 6860.0F)
            }, true
    );

    private static final ExactMappedArtwork D31D_ARTWORK = new ExactMappedArtwork(
            12505.0F, 11429.0F, 5.50F, artwork("d31d_frame.png"), artwork("d31d_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31d_panel_middle.png", MotorwaySignColor.WHITE),
                    layer("d31d_panel_top.png", 1), layer("d31d_panel_bottom.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 3088.0F, 1965.0F, 4200.0F, 1100.0F),
                    text(1, 6252.0F, 5716.0F, 11200.0F, 1360.0F),
                    text(2, 6252.0F, 8550.0F, 11200.0F, 1100.0F),
                    text(3, 6252.0F, 10300.0F, 8000.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 12505.0F, 3930.0F),
                    body(3.0F, 4256.0F, 12500.0F, 2921.0F),
                    body(0.0F, 7500.0F, 12505.0F, 3929.0F)
            }, true
    );

    private static final ExactMappedArtwork D31E_ARTWORK = new ExactMappedArtwork(
            13403.0F, 11036.0F, 5.80F, artwork("d31e_frame.png"), artwork("d31e_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31e_panel_top.png", MotorwaySignColor.WHITE),
                    layer("d31e_route.png", 0), layer("d31e_panel_middle.png", 1), layer("d31e_panel_bottom.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 2684.5F, 1469.0F, 3700.0F, 1062.0F),
                    text(1, 6701.5F, 6300.0F, 12100.0F, 1484.0F),
                    text(2, 6701.5F, 9800.0F, 12100.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 13403.0F, 4005.0F),
                    body(0.0F, 4366.0F, 13403.0F, 3905.0F),
                    body(0.0F, 8631.0F, 13403.0F, 2405.0F)
            }, true
    );

    private static final ExactMappedArtwork D32A_ARTWORK = new ExactMappedArtwork(
            11621.0F, 3922.0F, 5.20F, artwork("d32a_frame.png"), artwork("d32a_graphics.png"),
            new ExactTintedLayer[]{layer("d32a_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 3800.0F, 1250.0F, 6500.0F, 850.0F),
                    text(1, 3800.0F, 2850.0F, 6800.0F, 1100.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 11621.0F, 3922.0F)}, true
    );

    private static final ExactMappedArtwork D32A_DC_ARTWORK = new ExactMappedArtwork(
            11621.0F, 3922.0F, 5.20F, artwork("d32a_dc_frame.png"), artwork("d32a_dc_graphics.png"),
            new ExactTintedLayer[]{layer("d32a_dc_panel.png", 0)},
            D32A_ARTWORK.texts(), D32A_ARTWORK.bodies(), true
    );

    private static final ExactMappedArtwork D32B_ARTWORK = new ExactMappedArtwork(
            13504.0F, 4938.0F, 5.20F, artwork("d32b_frame.png"), artwork("d32b_graphics.png"),
            new ExactTintedLayer[]{layer("d32b_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 4300.0F, 1450.0F, 7500.0F, 1050.0F),
                    text(1, 4300.0F, 3400.0F, 7800.0F, 1250.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 13504.0F, 4938.0F)}, true
    );

    private static final ExactMappedArtwork D41A_ARTWORK = new ExactMappedArtwork(
            12467.0F, 11270.0F, 5.50F, artwork("d41a_frame.png"), artwork("d41a_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d41a_panel_top.png", MotorwaySignColor.WHITE),
                    layer("d41a_panel_middle.png", 1), layer("d41a_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 3060.5F, 1947.0F, 4100.0F, 1100.0F),
                    text(4, 9500.0F, 1947.0F, 3000.0F, 1000.0F),
                    text(1, 6233.5F, 5450.0F, 11200.0F, 1100.0F),
                    text(2, 6233.5F, 7200.0F, 11200.0F, 1100.0F),
                    text(3, 6233.5F, 10050.0F, 11200.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 12467.0F, 3894.0F),
                    body(0.0F, 4255.0F, 12467.0F, 4260.0F),
                    body(0.0F, 8872.0F, 12467.0F, 2398.0F)
            }, true
    );

    private static final ExactMappedArtwork D41B_ARTWORK = new ExactMappedArtwork(
            11536.0F, 8413.0F, 5.20F, artwork("d41b_frame.png"), null,
            new ExactTintedLayer[]{
                    fixedLayer("d41b_panel_top.png", MotorwaySignColor.WHITE), layer("d41b_route.png", 0),
                    layer("d41b_panel_middle.png", 1), layer("d41b_panel_bottom.png", 2)
            },
            new ExactTextPlacement[]{
                    text(0, 2685.5F, 1198.5F, 3600.0F, 1000.0F),
                    text(3, 8800.0F, 1198.5F, 2400.0F, 900.0F),
                    text(1, 5768.0F, 4200.0F, 10400.0F, 1200.0F),
                    text(2, 5768.0F, 7200.0F, 10400.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 11536.0F, 2399.0F),
                    body(0.0F, 2758.0F, 11536.0F, 2897.0F),
                    body(0.0F, 6014.0F, 11536.0F, 2399.0F)
            }, true
    );

    private static final ExactMappedArtwork D41C_ARTWORK = new ExactMappedArtwork(
            13098.0F, 11401.0F, 5.50F, artwork("d41c_frame.png"), null,
            new ExactTintedLayer[]{
                    layer("d41c_panel_top.png", 1), layer("d41c_route.png", 0),
                    layer("d41c_panel_middle.png", 1), layer("d41c_panel_bottom.png", 3)
            },
            new ExactTextPlacement[]{
                    text(0, 2429.5F, 1451.0F, 3000.0F, 1050.0F),
                    text(4, 10800.0F, 1451.0F, 3000.0F, 1000.0F),
                    text(1, 6549.0F, 4750.0F, 11800.0F, 1200.0F),
                    text(2, 6549.0F, 6800.0F, 11800.0F, 1200.0F),
                    text(3, 3800.0F, 10000.0F, 7000.0F, 1200.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 13098.0F, 2897.0F),
                    body(0.0F, 3259.0F, 13098.0F, 4890.0F),
                    body(0.0F, 8504.0F, 13098.0F, 2897.0F)
            }, true
    );

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
    private static final float MIN_PANEL_WIDTH = 2.30F;
    private static final float MAX_PANEL_WIDTH = 6.80F;
    private static final float DARK_TEXT_SCALE = 0.032F;
    private static final float LIGHT_TEXT_SCALE = 0.028F;
    private static final int PANEL_EDGE = 0xFFD7D7D2;
    private static final int SUPPORT_COLOR = 0xFF292929;
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final float D61_CARTOUCHE_HEIGHT = (float) (
            CartoucheLayout.CARTOUCHE_RENDER_HEIGHT / MotorwaySignGeometry.WORLD_SCALE
    );
    /** Cartouches D63c volontairement plus grands que ceux des petites pancartes. */
    private static final float D63C_CARTOUCHE_SCALE = 1.12F;
    private static final float D63C_CARTOUCHE_HEIGHT = D61_CARTOUCHE_HEIGHT
            * (D63C_CARTOUCHE_SCALE / CartoucheLayout.MODEL_SCALE);
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
        MotorwaySignPreset preset = state.preset == null ? MotorwaySignPreset.D31B_EX1 : state.preset;
        DEFERRED_TEXT_CONTEXT.set(new DeferredTextContext(
                state.facing,
                state.mountedOnCrossbar ? 0.0F : MotorwaySignGeometry.D61B_PANEL_FORWARD
        ));
        CustomStackLayout customLayout;
        if (preset == MotorwaySignPreset.D61B) {
            customLayout = buildD61BStackLayout(font, state.customPanels);
        } else {
            float presetWidth = MotorwaySignGeometry.forPreset(
                    preset, state.lines, state.mountedOnCrossbar
            ).width() / MotorwaySignGeometry.WORLD_SCALE;
            customLayout = withSharedPanelWidth(
                    buildCustomStackLayout(font, state.customPanels, false, preset == MotorwaySignPreset.D63C),
                    presetWidth
            );
        }
        float customPanelHeight = customPanelStackHeight(customLayout);
        float customTop = 0.0F;
        if (preset == MotorwaySignPreset.D61B) {
            customTop = d61PanelBottomInternal(state.customPanels)
                    + customLayout.totalHeight();
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
            MotorwaySignPanelData cartouchePanel = preset == MotorwaySignPreset.D61B
                    ? customLayout.panels().getFirst()
                    : firstConfiguredPanel(state.customPanels);
            if (cartouchePanel != null && cartouchePanel.cartoucheType().isVisible()) {
                float cartoucheTop = customTop;
                if (preset != MotorwaySignPreset.D61B) {
                    float originalHeight = MotorwaySignGeometry.forPreset(
                            preset, state.lines, state.mountedOnCrossbar
                    ).height() / MotorwaySignGeometry.WORLD_SCALE;
                    float originalTop = state.mountedOnCrossbar
                            ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                            : 2.05F + customPanelHeight
                            + (customLayout.panels().isEmpty() ? 0.0F : PANEL_GAP)
                            + originalHeight;
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
        if (preset != MotorwaySignPreset.D61B) {
            submitOriginalCartouches(
                    state, preset, font, customLayout, poseStack, collector
            );
        }

        if (!state.mountedOnCrossbar) {
            MotorwaySignGeometry groundGeometry = MotorwaySignGeometry.forComposite(
                    preset, state.lines, state.customPanels, false
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

        if (preset == MotorwaySignPreset.D61B) {
            drawCustomStack(
                    collector, poseStack, font, customLayout, customTop, state.lightCoords, true, false
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
                float originalBottom = MotorwaySignGeometry.MOUNTED_PANEL_TOP
                        / MotorwaySignGeometry.WORLD_SCALE - originalHeight;
                drawCustomStack(
                        collector, poseStack, font, additions,
                        originalBottom - PANEL_GAP, state.lightCoords, false,
                        preset == MotorwaySignPreset.D63C
                );
            } else {
                float originalShift = customPanelHeight + PANEL_GAP;
                drawCustomStack(
                        collector, poseStack, font, additions,
                        2.05F + customPanelHeight, state.lightCoords, false,
                        preset == MotorwaySignPreset.D63C
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

        ExactMappedArtwork exactArtwork = exactMappedArtwork(preset);
        if (exactArtwork != null) {
            drawExactMappedArtwork(
                    collector, poseStack, font, preset, state.lines, state.lightCoords,
                    exactArtwork, state.mountedOnCrossbar
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

        submitBox(collector, poseStack, left, right, topPanelBottom, topPanelTop,
                BACK_Z, FRONT_Z, PANEL_EDGE, light, -30);
        submitBox(collector, poseStack, left, right, bottomPanelBottom, bottomPanelTop,
                BACK_Z, FRONT_Z, PANEL_EDGE, light, -30);

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
            submitBox(collector, poseStack, left, right,
                    sourceY(top, height, 1662.0F, sourceHeight),
                    sourceY(top, height, 8.0F, sourceHeight),
                    BACK_Z, FRONT_Z, PANEL_EDGE, light, -30);
        }
        submitBox(collector, poseStack, left, right,
                sourceY(top, height, sourceHeight - 5.0F, sourceHeight),
                sourceY(top, height, 1837.0F, sourceHeight),
                BACK_Z, FRONT_Z, PANEL_EDGE, light, -30);
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

    private static ExactMappedArtwork exactMappedArtwork(MotorwaySignPreset preset) {
        return switch (preset) {
            case D31B_EX1 -> D31B_EX1_ARTWORK;
            case D31B_EX2 -> D31B_EX2_ARTWORK;
            case D31D -> D31D_ARTWORK;
            case D31E -> D31E_ARTWORK;
            case D32A -> D32A_ARTWORK;
            case D32A_DC -> D32A_DC_ARTWORK;
            case D32B -> D32B_ARTWORK;
            case D41A -> D41A_ARTWORK;
            case D41B -> D41B_ARTWORK;
            case D41C -> D41C_ARTWORK;
            case D61B -> D61B_ARTWORK;
            case D62A -> D62A_ARTWORK;
            case D62B -> D62B_ARTWORK;
            case D62D_TOP -> D62D_TOP_ARTWORK;
            case D62D_BOTTOM -> D62D_BOTTOM_ARTWORK;
            case D63C -> D63C_ARTWORK;
            case D63D -> D63D_ARTWORK;
            case D71 -> D71_ARTWORK;
            case D72 -> D72_ARTWORK;
            case D73 -> D73_ARTWORK;
            case DA31A -> DA31A_ARTWORK;
            case DA31B -> DA31B_ARTWORK;
            case DA31D -> DA31D_ARTWORK;
            case DA31E -> DA31E_ARTWORK;
            case DA31F -> DA31F_ARTWORK;
            case DA32A -> DA32A_ARTWORK;
            case DA32A_DC -> DA32A_DC_ARTWORK;
            case DA32B -> DA32B_ARTWORK;
            case DA32B_DC -> DA32B_DC_ARTWORK;
            default -> null;
        };
    }

    private static void drawExactMappedArtwork(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            int light,
            ExactMappedArtwork artwork,
            boolean mountedOnCrossbar
    ) {
        float width = artwork.physicalWidth();
        float height = width * artwork.sourceHeight() / artwork.sourceWidth();
        float left = -width / 2.0F;
        float right = width / 2.0F;
        float top = mountedOnCrossbar
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP / MotorwaySignGeometry.WORLD_SCALE
                : 2.05F + height;
        float bottom = top - height;

        if (!mountedOnCrossbar) {
            drawExactJunctionSupports(collector, poseStack, width, bottom, artwork.doublePost(), light);
        } else {
            drawCrossbarMounts(collector, poseStack, width, bottom, top, light);
        }
        int roadCartoucheBodies = standaloneRoadCartoucheCount(preset, artwork);
        for (int bodyIndex = 0; bodyIndex < artwork.bodies().length; bodyIndex++) {
            if (bodyIndex < roadCartoucheBodies) {
                continue;
            }
            ExactBody body = artwork.bodies()[bodyIndex];
            float bodyLeft = sourceX(left, width, body.x(), artwork.sourceWidth());
            float bodyRight = sourceX(left, width, body.x() + body.width(), artwork.sourceWidth());
            float bodyTop = sourceY(top, height, body.y(), artwork.sourceHeight());
            float bodyBottom = sourceY(top, height, body.y() + body.height(), artwork.sourceHeight());
            submitBox(collector, poseStack, bodyLeft, bodyRight, bodyBottom, bodyTop,
                    BACK_Z, FRONT_Z, PANEL_EDGE, light, -30);
        }

        drawArtworkLayer(collector, poseStack, artwork.frame(), left, right, bottom, top,
                FRONT_Z + 0.002F, 0xFFFFFFFF, light, -18);
        for (int layerIndex = 0; layerIndex < artwork.layers().length; layerIndex++) {
            ExactTintedLayer layer = artwork.layers()[layerIndex];
            if (layer.fixedArgb() == 0
                    && isStandaloneRoadCartoucheSlot(
                    preset, artwork, layer.slotIndex()
            )) {
                continue;
            }
            MotorwaySignLineData data = safeLine(values, layer.slotIndex(), preset.getSlot(layer.slotIndex()));
            int layerColor = layer.fixedArgb() == 0 ? data.color().getArgb() : layer.fixedArgb();
            drawArtworkLayer(collector, poseStack, layer.texture(), left, right, bottom, top,
                    FRONT_Z + 0.004F + layerIndex * 0.0005F, layerColor, light, -17 + layerIndex);
        }
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
            submitQuad(
                    collector, poseStack,
                    left, right,
                    panelBottom, panelTop,
                    FRONT_Z + 0.0065F,
                    MotorwaySignColor.WHITE.getArgb(), light, -14
            );
            submitQuad(
                    collector, poseStack,
                    left + LISTEL, right - LISTEL,
                    panelBottom + LISTEL, panelTop - LISTEL,
                    FRONT_Z + 0.0070F,
                    bottomPanel.color().getArgb(), light, -13
            );
        }
        if (artwork.graphics() != null) {
            drawArtworkLayer(collector, poseStack, artwork.graphics(), left, right, bottom, top,
                    FRONT_Z + 0.008F, 0xFFFFFFFF, light, -15);
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
            if (preset == MotorwaySignPreset.D63C
                    && (placement.slotIndex() == 2 || placement.slotIndex() == 3)) {
                FormattedCharSequence sequence = styled(data.text(), data.font());
                int pixelWidth = font.width(sequence);
                float availableWidth = Math.max(0.20F, width - 0.64F);
                float actualScale = pixelWidth <= 0
                        ? scale
                        : Math.min(scale, availableWidth / pixelWidth);
                x = left + 0.32F + pixelWidth * actualScale / 2.0F;
                maximumWidth = availableWidth;
            }
            if (isExitNumberSlot(slot)) {
                drawExitNumber(collector, poseStack, font, data.text(), x, y, maximumWidth,
                        data.font(), data.color().getTextArgb(), scale, light);
            } else {
                drawText(collector, poseStack, font, data.text(), x, y, maximumWidth,
                        data.font(), data.color().getTextArgb(), scale, light);
            }
        }
    }

    private static ExactTintedLayer layer(String filename, int slotIndex) {
        return new ExactTintedLayer(artwork(filename), slotIndex, 0);
    }

    private static ExactTintedLayer fixedLayer(String filename, MotorwaySignColor color) {
        return new ExactTintedLayer(artwork(filename), 0, color.getArgb());
    }

    private static ExactTextPlacement text(
            int slotIndex,
            float x,
            float y,
            float maximumWidth,
            float sourceHeight
    ) {
        return new ExactTextPlacement(slotIndex, x, y, maximumWidth, sourceHeight);
    }

    private static ExactBody body(float x, float y, float width, float height) {
        return new ExactBody(x, y, width, height);
    }

    private record ExactMappedArtwork(
            float sourceWidth,
            float sourceHeight,
            float physicalWidth,
            Identifier frame,
            Identifier graphics,
            ExactTintedLayer[] layers,
            ExactTextPlacement[] texts,
            ExactBody[] bodies,
            boolean doublePost
    ) {
    }

    private record ExactTintedLayer(Identifier texture, int slotIndex, int fixedArgb) {
    }

    private record ExactTextPlacement(
            int slotIndex,
            float x,
            float y,
            float maximumWidth,
            float sourceHeight
    ) {
    }

    private record ExactBody(float x, float y, float width, float height) {
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
            boolean spaciousMultiline
    ) {
        List<MotorwaySignPanelData> panels = new ArrayList<>();
        if (configuredPanels != null) {
            for (int configuredIndex = 0; configuredIndex < configuredPanels.length; configuredIndex++) {
                MotorwaySignPanelData panel = configuredPanels[configuredIndex];
                if (panel != null && panel.enabled()) {
                    if (!includeCartoucheOnlyPanel
                            && panel.cartoucheType().isVisible()
                            && !panel.hasPanelContent()) {
                        continue;
                    }
                    MotorwaySignPanelData normalized = withAllowedCustomBackground(panel);
                    panels.add(spaciousMultiline
                            ? withoutCartouche(normalized)
                            : (configuredIndex == 0
                            ? normalized
                            : withoutCartouche(normalized)));
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
            heights[index] = spaciousMultiline && panel.lineCount() >= 3
                    ? 0.64F + 0.58F * panel.lineCount()
                    : 0.48F + 0.40F * panel.lineCount();
            if (usesBottomArrow(panel.graphic())) {
                heights[index] += 0.50F;
            }
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
            MotorwaySignPanelData[] configuredPanels
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
                font, enabled.toArray(MotorwaySignPanelData[]::new), true, false
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
            boolean d61Style,
            boolean d63cStyle
    ) {
        float cursorTop = top;
        for (int index = 0; index < layout.panels().size(); index++) {
            MotorwaySignPanelData data = layout.panels().get(index);
            if (d61Style && data.cartoucheType().isVisible()) {
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
            if (d61Style) {
                drawD61PanelText(collector, poseStack, font, data, panelLayout, light);
            } else if (d63cStyle) {
                drawD63CPanelText(collector, poseStack, font, data, panelLayout, light);
            } else {
                drawCustomPanelText(collector, poseStack, font, data, panelLayout, light);
            }
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
            int light
    ) {
        final float textScale = 0.044F;
        final float lineStep = 0.45F;
        final float distanceGap = 0.30F;
        float leftMargin = 0.32F;
        float rightMargin = 0.28F;
        float reserve = sideGraphicReserve(data.graphic());
        float graphicOffset = textCenterOffset(data.graphic(), reserve);
        if (graphicOffset > 0.0F) {
            leftMargin += reserve;
        } else if (graphicOffset < 0.0F) {
            rightMargin += reserve;
        }

        float y = panel.centerY() + (data.lineCount() - 1) * lineStep / 2.0F - 0.055F;
        if (usesBottomArrow(data.graphic())) {
            y += 0.22F;
        }

        for (int index = 0; index < data.lineCount(); index++) {
            float lineY = y - index * lineStep;
            FormattedCharSequence distanceSequence = styled(data.distance(index), data.font(index));
            int distancePixels = font.width(distanceSequence);
            float distanceWidth = data.distance(index).isBlank()
                    ? 0.0F
                    : distancePixels * textScale;
            if (distanceWidth > 0.0F) {
                drawText(
                        collector, poseStack, font, data.distance(index),
                        panel.right() - rightMargin - distanceWidth / 2.0F, lineY,
                        distanceWidth + 0.002F, data.font(index),
                        panel.color().getTextArgb(), textScale, light
                );
            }

            FormattedCharSequence citySequence = styled(data.line(index), data.font(index));
            int cityPixels = font.width(citySequence);
            if (cityPixels <= 0) {
                continue;
            }
            float cityLeft = panel.left() + leftMargin;
            float cityRight = panel.right() - rightMargin
                    - (distanceWidth > 0.0F ? distanceWidth + distanceGap : 0.0F);
            float maximumWidth = Math.max(0.20F, cityRight - cityLeft);
            float actualScale = Math.min(textScale, maximumWidth / cityPixels);
            float actualWidth = cityPixels * actualScale;
            drawText(
                    collector, poseStack, font, data.line(index),
                    cityLeft + actualWidth / 2.0F, lineY, maximumWidth,
                    data.font(index), panel.color().getTextArgb(), textScale, light
            );
        }
    }

    /** D63c : les panneaux multilignes gardent une taille et un interligne généreux. */
    private static void drawD63CPanelText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPanelData data,
            PanelLayout panel,
            int light
    ) {
        final boolean multi = data.lineCount() >= 3;
        final float textScale = multi ? 0.064F : 0.046F;
        final float lineStep = multi ? 0.68F : 0.47F;
        final float distanceGap = 0.34F;
        final float leftMargin = 0.34F;
        final float rightMargin = 0.38F;
        float y = panel.centerY() + (data.lineCount() - 1) * lineStep / 2.0F - 0.045F;
        if (usesBottomArrow(data.graphic())) {
            y += 0.22F;
        }

        for (int index = 0; index < data.lineCount(); index++) {
            float lineY = y - index * lineStep;
            FormattedCharSequence distanceSequence = styled(data.distance(index), data.font(index));
            int distancePixels = font.width(distanceSequence);
            float distanceWidth = data.distance(index).isBlank()
                    ? 0.0F
                    : distancePixels * textScale;
            if (distanceWidth > 0.0F) {
                drawText(
                        collector, poseStack, font, data.distance(index),
                        panel.right() - rightMargin - distanceWidth / 2.0F, lineY,
                        distanceWidth + 0.002F, data.font(index),
                        panel.color().getTextArgb(), textScale, light
                );
            }

            FormattedCharSequence citySequence = styled(data.line(index), data.font(index));
            int cityPixels = font.width(citySequence);
            if (cityPixels <= 0) {
                continue;
            }
            float cityLeft = panel.left() + leftMargin;
            float cityRight = panel.right() - rightMargin
                    - (distanceWidth > 0.0F ? distanceWidth + distanceGap : 0.0F);
            float maximumWidth = Math.max(0.20F, cityRight - cityLeft);
            float actualScale = Math.min(textScale, maximumWidth / cityPixels);
            float actualWidth = cityPixels * actualScale;
            drawText(
                    collector, poseStack, font, data.line(index),
                    cityLeft + actualWidth / 2.0F, lineY, maximumWidth,
                    data.font(index), panel.color().getTextArgb(), textScale, light
            );
        }
    }

    private static void drawD61PanelText(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MotorwaySignPanelData data,
            PanelLayout panel,
            int light
    ) {
        final float textScale = 0.044F;
        final float leftMargin = 0.32F;
        final float rightMargin = 0.28F;
        final float distanceGap = 0.30F;
        final float lineStep = 0.45F;
        /* La fonte routière paraît optiquement un peu haute malgré un centrage métrique exact. */
        float firstY = panel.centerY() + (data.lineCount() - 1) * lineStep / 2.0F - 0.055F;

        for (int index = 0; index < data.lineCount(); index++) {
            float y = firstY - index * lineStep;
            FormattedCharSequence distanceSequence = styled(data.distance(index), data.font(index));
            int distancePixels = font.width(distanceSequence);
            float distanceWidth = data.distance(index).isBlank()
                    ? 0.0F
                    : distancePixels * textScale;
            if (distanceWidth > 0.0F) {
                drawText(
                        collector, poseStack, font, data.distance(index),
                        panel.right() - rightMargin - distanceWidth / 2.0F, y,
                        distanceWidth + 0.002F, data.font(index),
                        data.background().getTextArgb(), textScale, light
                );
            }

            FormattedCharSequence citySequence = styled(data.line(index), data.font(index));
            int cityPixels = font.width(citySequence);
            if (cityPixels <= 0) {
                continue;
            }
            float cityLeft = panel.left() + leftMargin;
            float cityRight = panel.right() - rightMargin
                    - (distanceWidth > 0.0F ? distanceWidth + distanceGap : 0.0F);
            float maximumWidth = Math.max(0.20F, cityRight - cityLeft);
            float actualScale = Math.min(textScale, maximumWidth / cityPixels);
            float actualWidth = cityPixels * actualScale;
            drawText(
                    collector, poseStack, font, data.line(index),
                    cityLeft + actualWidth / 2.0F, y,
                    maximumWidth, data.font(index),
                    data.background().getTextArgb(), textScale, light
            );
        }
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
            SubmitNodeCollector collector
    ) {
        float originalShift = !state.mountedOnCrossbar && !customLayout.panels().isEmpty()
                ? customPanelStackHeight(customLayout) + PANEL_GAP
                : 0.0F;
        float panelForward = state.mountedOnCrossbar
                ? 0.0F
                : MotorwaySignGeometry.D61B_PANEL_FORWARD;

        ExactMappedArtwork artwork = exactMappedArtwork(preset);
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

        if (firstVisible) {
            submitCartoucheModelScaled(
                    state, first.cartoucheType(), first.cartoucheText(),
                    cartoucheBottom, panelTop, panelForward,
                    secondVisible ? -lateral : 0.0F,
                    D63C_CARTOUCHE_SCALE,
                    poseStack, collector
            );
        }
        if (secondVisible) {
            submitCartoucheModelScaled(
                    state, second.cartoucheType(), second.cartoucheText(),
                    cartoucheBottom, panelTop, panelForward,
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
            case BLUE -> CartoucheType.E47;
            case BLACK, BROWN -> CartoucheType.E47;
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
            case E47 -> MotorwaySignColor.BLUE;
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

    private static void drawPlate(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            float left,
            float right,
            float bottom,
            float top,
            MotorwaySignColor color,
            int light
    ) {
        submitBox(collector, poseStack, left, right, bottom, top, BACK_Z, FRONT_Z, PANEL_EDGE, light, -20);
        submitQuad(collector, poseStack,
                left, right, bottom, top,
                FACE_Z, panelBorderColor(color), light, -16);
        submitQuad(collector, poseStack,
                left + LISTEL, right - LISTEL, bottom + LISTEL, top - LISTEL,
                FACE_Z + 0.001F, color.getArgb(), light, -15);
    }

    private static int panelBorderColor(MotorwaySignColor color) {
        return switch (color) {
            case BLUE, GREEN -> MotorwaySignColor.WHITE.getArgb();
            case WHITE -> MotorwaySignColor.BLACK.getArgb();
            default -> PANEL_EDGE;
        };
    }

    private static MotorwaySignColor allowedCustomBackground(MotorwaySignColor color) {
        return color == MotorwaySignColor.GREEN || color == MotorwaySignColor.WHITE
                ? color
                : MotorwaySignColor.BLUE;
    }

    private static MotorwaySignPanelData withAllowedCustomBackground(MotorwaySignPanelData panel) {
        MotorwaySignColor background = allowedCustomBackground(panel.background());
        if (background == panel.background()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                background, panel.cartoucheType(), panel.cartoucheText(), panel.graphic()
        );
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
        float size = Math.min(0.43F, panel.height() * 0.28F);
        float left = panel.left() + 0.13F;
        float bottom = panel.centerY() - size / 2.0F;
        drawServiceTexture(collector, poseStack, SERVICE_TEXTURE_1, left, left + size, bottom, bottom + size, light);
        drawServiceTexture(collector, poseStack, SERVICE_TEXTURE_2, left + size + 0.05F, left + size * 2.0F + 0.05F, bottom, bottom + size, light);
        if (panel.height() > 1.20F) {
            drawServiceTexture(collector, poseStack, SERVICE_TEXTURE_3, left, left + size, bottom - size - 0.05F, bottom - 0.05F, light);
        }
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
        FormattedCharSequence sequence = styled(cleaned, roadFont);
        int width = font.width(sequence);
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

        // Repli hors du renderer principal : rendu vanilla NORMAL, comme V9.0.
        float worldScale = MotorwaySignGeometry.WORLD_SCALE;
        float textScaleWorld = scale * worldScale;
        poseStack.pushPose();
        poseStack.translate(
                x * worldScale,
                y * worldScale,
                TEXT_Z * worldScale
        );
        poseStack.scale(textScaleWorld, -textScaleWorld, textScaleWorld);
        collector.submitText(
                poseStack,
                -width / 2.0F,
                -font.lineHeight / 2.0F,
                sequence,
                false,
                Font.DisplayMode.NORMAL,
                light,
                color,
                0x00000000,
                0x00000000
        );
        poseStack.popPose();
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

            collector.submitText(
                    poseStack,
                    -width / 2.0F,
                    -font.lineHeight / 2.0F,
                    sequence,
                    false,
                    Font.DisplayMode.NORMAL,
                    deferred.light(),
                    deferred.color(),
                    0x00000000,
                    0x00000000
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
        FormattedCharSequence sequence = styled(number, roadFont);
        int pixelWidth = font.width(sequence);
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

    private static void submitBox(
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
                : MotorwaySignLineData.fromSlot(fallback);
    }

    private static float renderedTextWidth(Font font, MotorwaySignLineData line) {
        int pixels = font.width(styled(line.text(), line.font()));
        float scale = line.color().isLight() ? LIGHT_TEXT_SCALE : DARK_TEXT_SCALE;
        return Math.max(0.0F, pixels * scale);
    }

    private static FormattedCharSequence styled(String value, RoadTextFont font) {
        return Component.literal(value == null ? "" : value)
                .withStyle(Style.EMPTY.withFont(font == RoadTextFont.L4 ? ROAD_FONT_L4 : ROAD_FONT_L1))
                .getVisualOrderText();
    }

    private static float sideGraphicReserve(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case DIAGONAL_LEFT, DIAGONAL_RIGHT, EXIT -> 0.82F;
            case SCHEMATIC_LEFT, SCHEMATIC_RIGHT -> 1.02F;
            case SERVICES, MOTORWAY -> 1.12F;
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Identifier texture(String filename) {
        return Identifier.fromNamespaceAndPath(MoreRoad.MODID, "textures/block/" + filename);
    }

    private static Identifier artwork(String filename) {
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
