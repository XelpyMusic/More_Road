package net.xelpy.moreroad.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Calcul commun de la position verticale, du support arrière et des hitboxes
 * des cartouches E41 à E47.
 *
 * Les modèles commencent à Y = 0. Le renderer les translate jusqu'au bord
 * supérieur réel de l'ensemble de panneaux puis ajoute un petit espace visuel
 * constant. Les hitboxes sont calculées à partir des dimensions réelles des
 * modèles actuellement fournis par l'utilisateur.
 */
public final class CartoucheLayout {

    private CartoucheLayout() {
    }

    /**
     * Petit espace entre le haut du panneau et le bas du cartouche.
     * 0,05 bloc = 0,8 unité de modèle.
     */
    public static final double PANEL_GAP = 0.050D;

    /**
     * Réduction visuelle conservée pour garder des proportions réalistes.
     */
    public static final float MODEL_SCALE = 0.72F;

    private static final double D21_SIMPLE_TOP = 14.96D / 16.0D;
    private static final double D21_DOUBLE_TOP = 14.96D / 16.0D;

    private static final double D61_SIMPLE_TOP = 14.99D / 16.0D;
    private static final double D61_DOUBLE_TOP = 14.92040D / 16.0D;

    private static final double EB_TOP = 15.00D / 16.0D;

    /*
     * Bornes réelles des nouveaux modèles de cartouches.
     * Relevées sur les fichiers JSON :
     *   X = 2.4 -> 13.6
     *   Y = 0.00286 -> 5.2
     *   Z = 6 -> 8
     *
     * Pour la hitbox, on ramène Ymin à 0 afin que la sélection englobe bien
     * toute la base du cartouche et la connexion au support arrière.
     */
    private static final double MODEL_MIN_X = 2.4D;
    private static final double MODEL_MAX_X = 13.6D;
    private static final double MODEL_MIN_Z = 5.0D;
    private static final double MODEL_MAX_Z = 7.0D;
    private static final double MODEL_MAX_Y = 5.2D;

    public static final double CARTOUCHE_RENDER_HEIGHT =
            (MODEL_MAX_Y * MODEL_SCALE) / 16.0D;

    /*
     * Recherche du poteau porteur le plus proche. Le rayon horizontal couvre
     * largement les assemblages de panneaux côte à côte du mod, tandis que
     * la recherche verticale permet de retrouver le poteau porteur dans le
     * bloc courant ou dans les blocs situés en dessous. Un poteau placé
     * au-dessus n'est jamais choisi comme support.
     */
    private static final int POLE_SEARCH_HORIZONTAL_RADIUS = 3;
    private static final int POLE_SEARCH_DOWN = 4;
    private static final int POLE_SEARCH_UP = 0;


    public static double getD21HighestTopY(D21APanelData[] panels) {
        if (panels == null) {
            return D21_SIMPLE_TOP;
        }

        boolean[] enabled = new boolean[panels.length];
        boolean[] doubleLines = new boolean[panels.length];

        for (int i = 0; i < panels.length; i++) {
            D21APanelData panel = panels[i];
            if (panel == null) {
                continue;
            }

            enabled[i] = panel.enabled();
            doubleLines[i] = panel.doubleLine();
        }

        return getD21HighestTopY(enabled, doubleLines);
    }

    public static double getD21HighestTopY(
            boolean[] enabled,
            boolean[] doubleLines
    ) {
        return getHighestTopY(enabled, doubleLines, true);
    }

    public static double getD21BottomY(D21APanelData[] panels) {
        return getD21HighestTopY(panels) + PANEL_GAP;
    }

    public static double getD21BottomY(
            boolean[] enabled,
            boolean[] doubleLines
    ) {
        return getD21HighestTopY(enabled, doubleLines) + PANEL_GAP;
    }

    public static double getD61HighestTopY(D61APanelData[] panels) {
        if (panels == null) {
            return D61_SIMPLE_TOP;
        }

        boolean[] enabled = new boolean[panels.length];
        boolean[] doubleLines = new boolean[panels.length];

        for (int i = 0; i < panels.length; i++) {
            D61APanelData panel = panels[i];
            if (panel == null) {
                continue;
            }

            enabled[i] = panel.enabled();
            doubleLines[i] = panel.doubleLine();
        }

        return getD61HighestTopY(enabled, doubleLines);
    }

    public static double getD61HighestTopY(
            boolean[] enabled,
            boolean[] doubleLines
    ) {
        return getHighestTopY(enabled, doubleLines, false);
    }

    public static double getD61BottomY(D61APanelData[] panels) {
        return getD61HighestTopY(panels) + PANEL_GAP;
    }

    public static double getD61BottomY(
            boolean[] enabled,
            boolean[] doubleLines
    ) {
        return getD61HighestTopY(enabled, doubleLines) + PANEL_GAP;
    }

    public static double getEBTopY() {
        return EB_TOP;
    }

    public static double getEBBottomY() {
        return EB_TOP + PANEL_GAP;
    }

    public static VoxelShape getCartoucheShape(
            Direction facing,
            double cartoucheBottomY
    ) {
        double bottomY = cartoucheBottomY * 16.0D;
        double topY = bottomY + (MODEL_MAX_Y * MODEL_SCALE);

        double wideMin = scaleAroundCenter(MODEL_MIN_X);
        double wideMax = scaleAroundCenter(MODEL_MAX_X);
        double thinMin = scaleAroundCenter(MODEL_MIN_Z);
        double thinMax = scaleAroundCenter(MODEL_MAX_Z);

        Direction safeFacing = facing == null ? Direction.NORTH : facing;

        return switch (safeFacing) {
            case SOUTH -> Block.box(
                    wideMin,
                    bottomY,
                    16.0D - thinMax,
                    wideMax,
                    topY,
                    16.0D - thinMin
            );
            case EAST -> Block.box(
                    16.0D - thinMax,
                    bottomY,
                    wideMin,
                    16.0D - thinMin,
                    topY,
                    wideMax
            );
            case WEST -> Block.box(
                    thinMin,
                    bottomY,
                    wideMin,
                    thinMax,
                    topY,
                    wideMax
            );
            default -> Block.box(
                    wideMin,
                    bottomY,
                    thinMin,
                    wideMax,
                    topY,
                    thinMax
            );
        };
    }

    public record PoleAnchor(
            double offsetX,
            double offsetZ,
            double poleTopY
    ) {
    }

    /**
     * Recherche le poteau réel le plus proche du cartouche.
     *
     * Un "poteau" peut être :
     * - un PoteauBlock explicite ;
     * - le poteau intégré d'un D21A / D21A2 ;
     * - le poteau intégré d'un D61A / D61A2 ;
     * - le poteau intégré d'un EB10 / EB20.
     *
     * Le calcul est fait en coordonnées de blocs par rapport au bloc porteur
     * du cartouche. Le support peut ainsi changer automatiquement de colonne
     * d'un panneau à l'autre.
     */
    public static PoleAnchor findNearestPoleAnchor(
            BlockGetter level,
            BlockPos origin,
            Direction facing,
            double cartoucheBottomY
    ) {
        if (level == null || origin == null) {
            return new PoleAnchor(0.0D, 0.0D, 1.0D);
        }

        Direction safeFacing =
                facing == null
                        ? Direction.NORTH
                        : facing;

        PoleAnchor bestAnchor = null;
        double bestScore = Double.POSITIVE_INFINITY;

        double targetY =
                cartoucheBottomY
                        + CARTOUCHE_RENDER_HEIGHT / 2.0D;

        /*
         * Le panneau s'étend latéralement sur un seul axe :
         * - NORTH / SOUTH : axe X ;
         * - EAST / WEST   : axe Z.
         *
         * On cherche donc les poteaux le long de cet axe, plutôt que de
         * balayer un carré complet autour de chaque panneau. Le résultat est
         * identique pour nos assemblages, avec beaucoup moins de lectures de
         * blocs côté rendu.
         */
        boolean lateralOnX =
                safeFacing == Direction.NORTH
                        || safeFacing == Direction.SOUTH;

        for (
                int dy = -POLE_SEARCH_DOWN;
                dy <= POLE_SEARCH_UP;
                dy++
        ) {
            for (
                    int lateral = -POLE_SEARCH_HORIZONTAL_RADIUS;
                    lateral <= POLE_SEARCH_HORIZONTAL_RADIUS;
                    lateral++
            ) {
                int dx = lateralOnX ? lateral : 0;
                int dz = lateralOnX ? 0 : lateral;

                BlockPos candidatePos =
                        origin.offset(dx, dy, dz);

                BlockState candidateState =
                        level.getBlockState(candidatePos);

                if (!isPoleCarrier(candidateState)) {
                    continue;
                }

                if (
                        candidateState.hasProperty(
                                HorizontalDirectionalBlock.FACING
                        )
                                && candidateState.getValue(
                                HorizontalDirectionalBlock.FACING
                        ) != safeFacing
                ) {
                    continue;
                }

                /*
                 * Les poteaux intégrés occupent toute la hauteur de leur
                 * bloc. Leur sommet est donc Y + 1 par rapport au bloc
                 * d'origine du cartouche.
                 */
                double poleTopY = dy + 1.0D;

                /*
                 * Distance 3D vers le cartouche. Le terme horizontal reste
                 * dominant, ce qui sélectionne naturellement la colonne
                 * de poteau la plus proche du cartouche.
                 */
                double verticalDelta =
                        poleTopY - targetY;

                double score =
                        dx * dx
                                + dz * dz
                                + verticalDelta
                                * verticalDelta
                                * 0.20D;

                /*
                 * En cas d'égalité, on préfère le poteau dont le sommet
                 * est le plus proche verticalement du cartouche.
                 */
                if (
                        score < bestScore
                                || (
                                Math.abs(score - bestScore) < 1.0E-7D
                                        && bestAnchor != null
                                        && Math.abs(verticalDelta)
                                        < Math.abs(
                                        bestAnchor.poleTopY()
                                                - targetY
                                )
                        )
                ) {
                    bestScore = score;
                    bestAnchor =
                            new PoleAnchor(
                                    dx,
                                    dz,
                                    poleTopY
                            );
                }
            }
        }

        /*
         * Les blocs D21/D61/EB possèdent normalement déjà leur propre poteau,
         * donc ce fallback ne doit servir que pendant un chargement incomplet.
         */
        return bestAnchor != null
                ? bestAnchor
                : new PoleAnchor(0.0D, 0.0D, 1.0D);
    }

    public static double getSupportBottomY(PoleAnchor anchor) {
        PoleAnchor safeAnchor =
                anchor == null
                        ? new PoleAnchor(0.0D, 0.0D, 1.0D)
                        : anchor;

        /*
         * V59 : raccord bord à bord.
         * L'extension commence exactement au sommet du poteau détecté.
         * Aucun chevauchement n'est appliqué, ce qui supprime la double
         * épaisseur visible au point de jonction.
         */
        return safeAnchor.poleTopY();
    }

    public static double getSupportTopY(double cartoucheBottomY) {
        /*
         * Le support monte derrière toute la hauteur du cartouche. Il reste
         * ainsi réellement raccordé au cartouche, même lorsqu'il doit partir
         * d'un poteau situé plus bas.
         */
        return cartoucheBottomY + CARTOUCHE_RENDER_HEIGHT;
    }

    public static VoxelShape getSupportShape(
            Direction facing,
            PoleAnchor anchor,
            double cartoucheBottomY
    ) {
        PoleAnchor safeAnchor =
                anchor == null
                        ? new PoleAnchor(0.0D, 0.0D, 1.0D)
                        : anchor;

        double minY =
                getSupportBottomY(safeAnchor) * 16.0D;

        double maxY =
                getSupportTopY(cartoucheBottomY) * 16.0D;

        if (maxY <= minY) {
            return Shapes.empty();
        }

        double shiftX = safeAnchor.offsetX() * 16.0D;
        double shiftZ = safeAnchor.offsetZ() * 16.0D;

        /*
         * Même section 2 x 2 que les poteaux existants. La forme est ensuite
         * déplacée vers la colonne du poteau réellement détecté.
         */
        Direction safeFacing =
                facing == null
                        ? Direction.NORTH
                        : facing;

        return switch (safeFacing) {
            case SOUTH -> Block.box(
                    7.0D + shiftX,
                    minY,
                    7.0D + shiftZ,
                    9.0D + shiftX,
                    maxY,
                    9.0D + shiftZ
            );
            case EAST -> Block.box(
                    7.0D + shiftX,
                    minY,
                    7.0D + shiftZ,
                    9.0D + shiftX,
                    maxY,
                    9.0D + shiftZ
            );
            case WEST -> Block.box(
                    7.0D + shiftX,
                    minY,
                    7.0D + shiftZ,
                    9.0D + shiftX,
                    maxY,
                    9.0D + shiftZ
            );
            default -> Block.box(
                    7.0D + shiftX,
                    minY,
                    7.0D + shiftZ,
                    9.0D + shiftX,
                    maxY,
                    9.0D + shiftZ
            );
        };
    }

    private static boolean isPoleCarrier(BlockState state) {
        if (state == null) {
            return false;
        }

        Block block = state.getBlock();

        return block instanceof PoteauBlock
                || block instanceof D21ABlock
                || block instanceof D21A2Block
                || block instanceof D61ABlock
                || block instanceof D61A2Block
                || block instanceof EB10Block;
    }

    private static double getHighestTopY(
            boolean[] enabled,
            boolean[] doubleLines,
            boolean d21
    ) {
        double highestTop = Double.NEGATIVE_INFINITY;

        if (enabled != null && doubleLines != null) {
            int count = Math.min(enabled.length, doubleLines.length);

            for (int i = 0; i < count; i++) {
                if (!enabled[i]) {
                    continue;
                }

                double panelTop;

                if (d21) {
                    panelTop =
                            D21APanelLayout.getPanelYOffset(enabled, doubleLines, i)
                                    + (doubleLines[i] ? D21_DOUBLE_TOP : D21_SIMPLE_TOP);
                } else {
                    panelTop =
                            D61APanelLayout.getPanelYOffset(enabled, doubleLines, i)
                                    + (doubleLines[i] ? D61_DOUBLE_TOP : D61_SIMPLE_TOP);
                }

                highestTop = Math.max(highestTop, panelTop);
            }
        }

        if (!Double.isFinite(highestTop)) {
            highestTop = d21 ? D21_SIMPLE_TOP : D61_SIMPLE_TOP;
        }

        return highestTop;
    }

    private static double scaleAroundCenter(double modelCoordinate) {
        return 8.0D + (modelCoordinate - 8.0D) * MODEL_SCALE;
    }
}
