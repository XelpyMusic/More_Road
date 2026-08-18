package net.xelpy.moreroad.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mise en page commune aux D21A simples et D21A2 doubles.
 *
 * Chaque emplacement peut maintenant choisir son propre format. Cette
 * classe calcule donc la position verticale de chaque plaque en fonction
 * de sa vraie hauteur afin de pouvoir mélanger 1 ligne et 2 lignes sur le
 * même poteau sans chevauchement et sans casser l'espacement actuel des
 * ensembles 100 % simples ou 100 % doubles.
 */
public final class D21APanelLayout {

    private D21APanelLayout() {
    }

    /* ============================================================
     * GÉOMÉTRIE VERTICALE
     * ============================================================ */

    private static final double SIMPLE_HEIGHT = 7.16D / 16.0D;
    private static final double DOUBLE_HEIGHT = 8.88D / 16.0D;

    private static final double SIMPLE_BASE_CENTER = 11.38D / 16.0D;
    private static final double DOUBLE_BASE_CENTER = 10.52D / 16.0D;

    private static final double SIMPLE_GAP =
            D21ABlock.PANEL_VERTICAL_STEP - SIMPLE_HEIGHT;

    private static final double DOUBLE_GAP =
            D21A2Block.PANEL_VERTICAL_STEP - DOUBLE_HEIGHT;

    public static double getPanelYOffset(
            D21APanelData[] panels,
            int targetIndex
    ) {
        if (
                panels == null
                        || targetIndex < 0
                        || targetIndex >= panels.length
                        || panels[targetIndex] == null
                        || !panels[targetIndex].enabled()
        ) {
            return 0.0D;
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

        return getPanelYOffset(
                enabled,
                doubleLines,
                targetIndex
        );
    }

    public static double getPanelYOffset(
            boolean[] enabled,
            boolean[] doubleLines,
            int targetIndex
    ) {
        if (
                enabled == null
                        || doubleLines == null
                        || targetIndex < 0
                        || targetIndex >= enabled.length
                        || targetIndex >= doubleLines.length
                        || !enabled[targetIndex]
        ) {
            return 0.0D;
        }

        /*
         * V48 :
         * On conserve les modèles simples recentrés de la V47, mais on
         * restaure le centrage vertical de la pile autour de sa position
         * historique.
         *
         * La V46 avait ancré toutes les piles depuis le haut. Cela alignait
         * leur sommet, mais faisait descendre fortement les ensembles de
         * plusieurs panneaux. Avec les géométries gauche/droite maintenant
         * normalisées en V47, cet ancrage n'est plus nécessaire.
         */
        int enabledCount = 0;
        double stackCenter = 0.0D;
        double totalHeight = 0.0D;

        int previousEnabledIndex = -1;

        for (int i = 0; i < enabled.length; i++) {
            if (!enabled[i]) {
                continue;
            }

            boolean doubleLine = doubleLines[i];

            enabledCount++;
            stackCenter += getBaseCenter(doubleLine);
            totalHeight += getHeight(doubleLine);

            if (previousEnabledIndex >= 0) {
                totalHeight += getGap(
                        doubleLines[previousEnabledIndex],
                        doubleLine
                );
            }

            previousEnabledIndex = i;
        }

        if (enabledCount <= 1) {
            return 0.0D;
        }

        stackCenter /= enabledCount;

        double cursorTop =
                stackCenter
                        + totalHeight / 2.0D;

        for (int i = 0; i < enabled.length; i++) {
            if (!enabled[i]) {
                continue;
            }

            boolean doubleLine = doubleLines[i];
            double height = getHeight(doubleLine);
            double desiredCenter = cursorTop - height / 2.0D;

            if (i == targetIndex) {
                return desiredCenter - getBaseCenter(doubleLine);
            }

            int nextEnabledIndex = findNextEnabled(enabled, i + 1);

            if (nextEnabledIndex < 0) {
                break;
            }

            cursorTop -=
                    height
                            + getGap(
                            doubleLine,
                            doubleLines[nextEnabledIndex]
                    );
        }

        return 0.0D;
    }

    private static int findNextEnabled(
            boolean[] enabled,
            int start
    ) {
        for (int i = start; i < enabled.length; i++) {
            if (enabled[i]) {
                return i;
            }
        }

        return -1;
    }

    private static double getHeight(boolean doubleLine) {
        return doubleLine
                ? DOUBLE_HEIGHT
                : SIMPLE_HEIGHT;
    }

    private static double getBaseCenter(boolean doubleLine) {
        return doubleLine
                ? DOUBLE_BASE_CENTER
                : SIMPLE_BASE_CENTER;
    }

    private static double getGap(
            boolean firstDouble,
            boolean secondDouble
    ) {
        double firstGap =
                firstDouble
                        ? DOUBLE_GAP
                        : SIMPLE_GAP;

        double secondGap =
                secondDouble
                        ? DOUBLE_GAP
                        : SIMPLE_GAP;

        return (firstGap + secondGap) / 2.0D;
    }

    /* ============================================================
     * HITBOXES DES PLAQUES
     * ============================================================ */

    /*
     * V47 : les deux modèles D21A simples sont maintenant physiquement
     * recentrés sur X = 8 et partagent exactement la même hauteur.
     *
     * NORTH :
     * X = -7.96 -> 23.96
     * Y =  7.80 -> 14.96
     * Z =  6.00 ->  8.00
     *
     * La hitbox ne dépend donc plus du côté de la flèche.
     */
    private static final VoxelShape SIMPLE_NORTH =
            Block.box(-7.96, 7.80, 6.00, 23.96, 14.96, 8.00);

    private static final VoxelShape SIMPLE_EAST =
            Block.box(8.00, 7.80, -7.96, 10.00, 14.96, 23.96);

    private static final VoxelShape SIMPLE_SOUTH =
            Block.box(-7.96, 7.80, 8.00, 23.96, 14.96, 10.00);

    private static final VoxelShape SIMPLE_WEST =
            Block.box(6.00, 7.80, -7.96, 8.00, 14.96, 23.96);

    private static final VoxelShape DOUBLE_NORTH =
            Block.box(-7.96, 6.08, 6.00, 23.96, 14.96, 8.00);

    private static final VoxelShape DOUBLE_EAST =
            Block.box(8.00, 6.08, -7.96, 10.00, 14.96, 23.96);

    private static final VoxelShape DOUBLE_SOUTH =
            Block.box(-7.96, 6.08, 8.00, 23.96, 14.96, 10.00);

    private static final VoxelShape DOUBLE_WEST =
            Block.box(6.00, 6.08, -7.96, 8.00, 14.96, 23.96);

    public static VoxelShape getPanelShape(
            Direction facing,
            boolean arrowRight,
            boolean doubleLine
    ) {
        if (doubleLine) {
            return switch (facing) {
                case EAST -> DOUBLE_EAST;
                case SOUTH -> DOUBLE_SOUTH;
                case WEST -> DOUBLE_WEST;
                default -> DOUBLE_NORTH;
            };
        }

        return switch (facing) {
            case EAST -> SIMPLE_EAST;
            case SOUTH -> SIMPLE_SOUTH;
            case WEST -> SIMPLE_WEST;
            default -> SIMPLE_NORTH;
        };
    }
}
