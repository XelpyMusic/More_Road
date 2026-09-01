package net.xelpy.moreroad.client.renderer;

import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.MotorwaySignPreviewLayout;

/**
 * Catalogue des panneaux reproduits exactement depuis un SVG de référence
 * (registres, textes, pastilles) pour {@link MotorwaySignBlockEntityRenderer}.
 *
 * Ce fichier ne contient QUE des données (coordonnées mesurées au pixel près
 * sur les SVG, dans build/tmp/motorway-svg/) : aucune logique de dessin. Le
 * moteur de rendu partagé (drawExactMappedArtwork() et ses aides) reste dans
 * MotorwaySignBlockEntityRenderer, qui référence ce catalogue via
 * {@link #exactMappedArtwork(MotorwaySignPreset)}. Séparé du renderer
 * uniquement pour garder ce dernier lisible : les deux fichiers restent dans
 * le même package et s'utilisent mutuellement sans getters supplémentaires.
 *
 * Classe publique uniquement pour {@link #previewLayoutFor(MotorwaySignPreset)},
 * utilisée par l'éditeur générique (autre package) afin que son aperçu 2D
 * reflète la vraie disposition des registres au lieu de suppositions
 * génériques. Les types internes (ExactMappedArtwork, etc.) restent
 * package-private : la classe reste donc, en pratique, réservée au rendu.
 */
public final class MotorwaySignArtworkCatalog {

    private MotorwaySignArtworkCatalog() {
    }

    /** Null si {@code preset} n'a pas de dessin exact (voir {@link #exactMappedArtwork}). */
    public static MotorwaySignPreviewLayout previewLayoutFor(MotorwaySignPreset preset) {
        ExactMappedArtwork artwork = exactMappedArtwork(preset);
        if (artwork == null) {
            return null;
        }
        ExactBody[] bodies = artwork.bodies();
        float[] bodyX = new float[bodies.length];
        float[] bodyY = new float[bodies.length];
        float[] bodyWidth = new float[bodies.length];
        float[] bodyHeight = new float[bodies.length];
        for (int index = 0; index < bodies.length; index++) {
            bodyX[index] = bodies[index].x();
            bodyY[index] = bodies[index].y();
            bodyWidth[index] = bodies[index].width();
            bodyHeight[index] = bodies[index].height();
        }
        ExactTextPlacement[] texts = artwork.texts();
        int[] textSlotIndex = new int[texts.length];
        float[] textX = new float[texts.length];
        float[] textY = new float[texts.length];
        float[] textWidth = new float[texts.length];
        float[] textHeight = new float[texts.length];
        for (int index = 0; index < texts.length; index++) {
            textSlotIndex[index] = texts[index].slotIndex();
            textX[index] = texts[index].x();
            textY[index] = texts[index].y();
            textWidth[index] = texts[index].maximumWidth();
            textHeight[index] = texts[index].sourceHeight();
        }
        return new MotorwaySignPreviewLayout(
                artwork.sourceWidth(), artwork.sourceHeight(),
                bodyX, bodyY, bodyWidth, bodyHeight,
                textSlotIndex, textX, textY, textWidth, textHeight
        );
    }

    static ExactMappedArtwork exactMappedArtwork(MotorwaySignPreset preset) {
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
            case D44 -> D44_ARTWORK;
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

    static final ExactMappedArtwork D61B_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D62A_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D62B_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D62D_TOP_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D62D_BOTTOM_ARTWORK = new ExactMappedArtwork(
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

    /**
     * D44 : présignalisation de village étape. Le SVG de référence est
     * l'assemblage complet "D44 + 6 CE" ; les deux premiers registres
     * (sortie + distance, puis nom + mention "village étape") viennent de
     * ce SVG mesuré au pixel près. Les 3 premiers panonceaux CE (toujours
     * identiques à ceux du D45, réglementairement) sont ajoutés en dessous
     * comme une 3e rangée fixe — voir
     * {@link MotorwaySignBlockEntityRenderer#drawD44ServiceRow} — le joueur
     * choisit lui-même lesquels afficher (voir MotorwaySignServiceIcon),
     * jusqu'à 2 rangées de 3, plutôt qu'un jeu figé repris de l'"exemple" du
     * SVG. La hauteur source (7267x9304) réserve donc la place des 2
     * rangées même quand une seule est utilisée : simplifie la géométrie
     * (pas de recalcul par instance) au prix d'une marge basse inutilisée
     * quand moins de 4 panonceaux sont activés. La mention "village étape"
     * est réglementairement fixe : elle est cuite dans le calque graphics
     * plutôt qu'éditable.
     */
    static final float D44_SERVICE_ROW_TOP = 4618.0F;
    static final float D44_SERVICE_ICON_SIZE = 2203.0F;
    static final float D44_SERVICE_ROW_GAP = 150.0F;
    static final float[] D44_SERVICE_ICON_LEFT = {85.0F, 2527.0F, 4980.0F};

    static final ExactMappedArtwork D44_ARTWORK = new ExactMappedArtwork(
            7267.0F, 9304.0F, 4.60F, artwork("d44_frame.png"), artwork("d44_graphics.png"),
            new ExactTintedLayer[]{},
            new ExactTextPlacement[]{
                    /* x/y sont le CENTRE de la boîte mesurée sur le SVG (drawTextLine centre autour de x,y), pas son coin haut-gauche. */
                    text(0, 1522.5F, 908.5F, 1659.0F, 537.0F),
                    text(1, 5955.5F, 914.0F, 2015.0F, 548.0F),
                    text(2, 1967.5F, 2711.0F, 3241.0F, 662.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 7267.0F, 1826.0F),
                    body(0.0F, 2013.0F, 7267.0F, 2455.0F)
            }, true
    );

    static final ExactMappedArtwork D63C_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D63D_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D71_ARTWORK = new ExactMappedArtwork(
            13213.0F, 6434.0F, 5.60F, artwork("d71_frame.png"), null,
            new ExactTintedLayer[]{layer("d71_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 6603.0F, 1291.0F, 12000.0F, 850.0F),
                    text(1, 6020.0F, 3356.0F, 11200.0F, 1160.0F),
                    text(2, 5910.0F, 5349.0F, 11200.0F, 850.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 13213.0F, 6434.0F)}, true
    );

    static final ExactMappedArtwork D72_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D73_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA31A_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA31B_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA31D_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA31E_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA31F_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork DA32A_ARTWORK = new ExactMappedArtwork(
            9909.0F, 7208.0F, 4.60F, artwork("da32a_frame.png"), artwork("da32a_graphics.png"),
            new ExactTintedLayer[]{layer("da32a_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 5078.0F, 1214.0F, 8600.0F, 1090.0F),
                    text(1, 4962.0F, 2807.0F, 9000.0F, 1093.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 9909.0F, 7208.0F)}, true
    );

    static final ExactMappedArtwork DA32A_DC_ARTWORK = new ExactMappedArtwork(
            9909.0F, 7208.0F, 4.60F, artwork("da32a_dc_frame.png"), artwork("da32a_dc_graphics.png"),
            new ExactTintedLayer[]{layer("da32a_dc_panel.png", 0)},
            DA32A_ARTWORK.texts(), DA32A_ARTWORK.bodies(), true
    );

    static final ExactMappedArtwork DA32B_ARTWORK = new ExactMappedArtwork(
            9909.0F, 8942.0F, 4.60F, artwork("da32b_frame.png"), artwork("da32b_graphics.png"),
            new ExactTintedLayer[]{layer("da32b_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 4954.0F, 1484.0F, 8800.0F, 1374.0F),
                    text(1, 4962.0F, 3475.0F, 9000.0F, 1374.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 9909.0F, 8942.0F)}, true
    );

    static final ExactMappedArtwork DA32B_DC_ARTWORK = new ExactMappedArtwork(
            9909.0F, 8942.0F, 4.60F, artwork("da32b_dc_frame.png"), artwork("da32b_dc_graphics.png"),
            new ExactTintedLayer[]{layer("da32b_dc_panel.png", 0)},
            DA32B_ARTWORK.texts(), DA32B_ARTWORK.bodies(), true
    );

    static final ExactMappedArtwork D31B_EX1_ARTWORK = new ExactMappedArtwork(
            7832.0F, 4993.0F, 4.20F, artwork("d31b_ex1_frame.png"), artwork("d31b_ex1_graphics.png"),
            new ExactTintedLayer[]{layer("d31b_ex1_route.png", 0)},
            new ExactTextPlacement[]{
                    /*
                     * D31b ex.1 :
                     * - numéro de route recentré dans le cadre jaune du SVG ;
                     * - destinations descendues et légèrement réduites pour
                     *   rester entièrement dans le panneau inférieur, même
                     *   avec des noms un peu plus longs.
                     */
                    /* D31b ex.1 : conserver le centrage initial du numéro de route. */
                    text(0, 1778.3F, 952.0F, 2500.0F, 620.0F),
                    /*
                     * Destinations davantage alignées à gauche et légèrement
                     * agrandies pour retrouver une taille proche de la lame
                     * "TOULOUSE / AGEN". Léger surplus d'espace par rapport à
                     * l'origine (775 d'écart) sans aller jusqu'aux panneaux
                     * réels (lignes quasi collées, ex. "D 922 / ST SAUVES /
                     * LAQUEUILLE") : un premier essai plus large était allé
                     * trop loin dans l'autre sens.
                     */
                    text(1, 3700.0F, 3440.0F, 7300.0F, 600.0F),
                    text(2, 3550.0F, 4435.0F, 7000.0F, 600.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 7832.0F, 2349.0F),
                    body(0.0F, 2644.0F, 7832.0F, 2349.0F)
            }, true
    );

    static final ExactMappedArtwork D31B_EX2_ARTWORK = new ExactMappedArtwork(
            15397.0F, 11745.0F, 6.00F, artwork("d31b_ex2_frame.png"), artwork("d31b_ex2_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31b_ex2_panel_top.png", MotorwaySignColor.WHITE),
                    layer("d31b_ex2_route.png", 0), layer("d31b_ex2_panel_bottom.png", 1)
            },
            new ExactTextPlacement[]{
                    /* D31b ex.2 : route recentrée dans le cartouche rouge. */
                    text(0, 1963.0F, 1842.0F, 2550.0F, 969.0F),
                    /*
                     * Les destinations doivent partir du même bord gauche,
                     * comme sur le SVG, au lieu d'être centrées dans chaque
                     * panneau.
                     */
                    text(1, 7693.5F, 6660.0F, 13800.0F, 1450.0F),
                    text(2, 7693.5F, 8690.0F, 13800.0F, 1450.0F),
                    text(3, 7693.5F, 10500.0F, 13800.0F, 1450.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 15397.0F, 4535.0F),
                    body(0.0F, 4885.0F, 15397.0F, 6860.0F)
            }, true
    );

    static final ExactMappedArtwork D31D_ARTWORK = new ExactMappedArtwork(
            12505.0F, 11429.0F, 5.50F, artwork("d31d_frame.png"), artwork("d31d_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31d_panel_middle.png", MotorwaySignColor.WHITE),
                    layer("d31d_panel_top.png", 1), layer("d31d_panel_bottom.png", 5)
            },
            new ExactTextPlacement[]{
                    /*
                     * Signalé : "19" un peu trop haut dans son cartouche, et
                     * l'ensemble du panneau (registres 1 à 3) un peu trop
                     * haut par rapport à la photo de référence — léger
                     * décalage vers le bas de tous les registres.
                     * L'écart entre les deux destinations du bas (registre 3)
                     * était aussi trop large : resserré (1750 -> 1250).
                     *
                     * Le premier essai (1965 -> 2300) est allé trop loin :
                     * le "19" ressortait alors trop bas par rapport à
                     * l'ovale fixe qui l'entoure (image du cadre, non lié à
                     * ce placement de texte) — décalage réduit à +85 au lieu
                     * de +335 pour recentrer le texte dans cet ovale.
                     *
                     * Le resserrement à 1250 est ensuite allé trop loin dans
                     * l'autre sens (les deux noms de villes du bas trop
                     * collés l'un à l'autre) : réécarté à 1450, entre les
                     * deux valeurs déjà essayées.
                     */
                    text(0, 3088.0F, 2050.0F, 4200.0F, 1100.0F),
                    text(1, 6252.0F, 5916.0F, 11200.0F, 1360.0F),
                    text(5, 6252.0F, 9000.0F, 11200.0F, 1100.0F),
                    text(6, 6252.0F, 10450.0F, 8000.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 12505.0F, 3930.0F),
                    body(3.0F, 4256.0F, 12500.0F, 2921.0F),
                    body(0.0F, 7500.0F, 12505.0F, 3929.0F)
            }, true
    );

    static final ExactMappedArtwork D31E_ARTWORK = new ExactMappedArtwork(
            13403.0F, 11036.0F, 5.80F, artwork("d31e_frame.png"), artwork("d31e_graphics.png"),
            new ExactTintedLayer[]{
                    fixedLayer("d31e_panel_top.png", MotorwaySignColor.WHITE),
                    layer("d31e_route.png", 0), layer("d31e_panel_middle.png", 1), layer("d31e_panel_bottom.png", 5)
            },
            new ExactTextPlacement[]{
                    text(0, 2684.5F, 1469.0F, 3700.0F, 1062.0F),
                    text(1, 6701.5F, 6300.0F, 12100.0F, 1484.0F),
                    text(5, 6701.5F, 9800.0F, 12100.0F, 1100.0F)
            },
            new ExactBody[]{
                    body(0.0F, 0.0F, 13403.0F, 4005.0F),
                    body(0.0F, 4366.0F, 13403.0F, 3905.0F),
                    body(0.0F, 8631.0F, 13403.0F, 2405.0F)
            }, true
    );

    static final ExactMappedArtwork D32A_ARTWORK = new ExactMappedArtwork(
            11621.0F, 3922.0F, 5.20F, artwork("d32a_frame.png"), artwork("d32a_graphics.png"),
            new ExactTintedLayer[]{layer("d32a_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 3800.0F, 1250.0F, 6500.0F, 850.0F),
                    text(1, 3800.0F, 2850.0F, 6800.0F, 1100.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 11621.0F, 3922.0F)}, true
    );

    static final ExactMappedArtwork D32A_DC_ARTWORK = new ExactMappedArtwork(
            11621.0F, 3922.0F, 5.20F, artwork("d32a_dc_frame.png"), artwork("d32a_dc_graphics.png"),
            new ExactTintedLayer[]{layer("d32a_dc_panel.png", 0)},
            D32A_ARTWORK.texts(), D32A_ARTWORK.bodies(), true
    );

    static final ExactMappedArtwork D32B_ARTWORK = new ExactMappedArtwork(
            13504.0F, 4938.0F, 5.20F, artwork("d32b_frame.png"), artwork("d32b_graphics.png"),
            new ExactTintedLayer[]{layer("d32b_panel.png", 0)},
            new ExactTextPlacement[]{
                    text(0, 4300.0F, 1450.0F, 7500.0F, 1050.0F),
                    text(1, 4300.0F, 3400.0F, 7800.0F, 1250.0F)
            },
            new ExactBody[]{body(0.0F, 0.0F, 13504.0F, 4938.0F)}, true
    );

    static final ExactMappedArtwork D41A_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D41B_ARTWORK = new ExactMappedArtwork(
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

    static final ExactMappedArtwork D41C_ARTWORK = new ExactMappedArtwork(
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

    private static Identifier artwork(String filename) {
        return MotorwaySignBlockEntityRenderer.artwork(filename);
    }
}

/*
 * Types de premier niveau (et non imbriqués dans MotorwaySignArtworkCatalog)
 * afin de rester utilisables par leur simple nom depuis
 * MotorwaySignBlockEntityRenderer, comme avant l'extraction du catalogue :
 * un type imbriqué exigerait de qualifier chaque usage
 * (MotorwaySignArtworkCatalog.ExactMappedArtwork, etc.) dans tout le moteur
 * de rendu.
 */
record ExactMappedArtwork(
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

record ExactTintedLayer(Identifier texture, int slotIndex, int fixedArgb) {
}

record ExactTextPlacement(
        int slotIndex,
        float x,
        float y,
        float maximumWidth,
        float sourceHeight
) {
}

record ExactBody(float x, float y, float width, float height) {
}
