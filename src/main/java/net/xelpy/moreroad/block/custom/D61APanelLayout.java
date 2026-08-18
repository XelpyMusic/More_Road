package net.xelpy.moreroad.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mise en page commune aux D61A simples et doubles.
 *
 * Chaque emplacement peut être simple (1 ligne) ou double (2 lignes),
 * ce qui permet de mélanger plusieurs plaques sur le même poteau.
 */
public final class D61APanelLayout {

    private D61APanelLayout() {
    }

    private static final double SIMPLE_HEIGHT = 4.56D / 16.0D;
    private static final double DOUBLE_HEIGHT = 8.43781D / 16.0D;

    private static final double SIMPLE_BASE_CENTER = 12.71D / 16.0D;
    private static final double DOUBLE_BASE_CENTER = 10.701495D / 16.0D;

    /*
     * Les D61 réels sont montés très près les uns des autres.
     * On utilise donc ici un petit jeu visuel fixe au lieu de reprendre
     * l'ancien pas vertical du système D21A.
     *
     * 0.04 bloc = 0.64 pixel de modèle entre deux simples.
     * 0.05 bloc = 0.80 pixel de modèle entre deux doubles.
     */
    private static final double SIMPLE_GAP = 0.040D;
    private static final double DOUBLE_GAP = 0.050D;

    /*
     * D61A simple : dimensions exactes du nouveau modèle Blockbench.
     *
     * Modèle NORTH :
     * X = -8.00 -> 24.00
     * Y = 10.43 -> 14.99
     * Z =  5.97 ->  7.98
     */
    private static final VoxelShape SIMPLE_NORTH =
            Block.box(-8.00, 10.43, 5.97, 24.00, 14.99, 7.98);

    private static final VoxelShape SIMPLE_EAST =
            Block.box(8.02, 10.43, -8.00, 10.03, 14.99, 24.00);

    private static final VoxelShape SIMPLE_SOUTH =
            Block.box(-8.00, 10.43, 8.02, 24.00, 14.99, 10.03);

    private static final VoxelShape SIMPLE_WEST =
            Block.box(5.97, 10.43, -8.00, 7.98, 14.99, 24.00);

    /*
     * D61A double : dimensions exactes du nouveau modèle Blockbench.
     *
     * Modèle NORTH :
     * X = -7.92040 -> 23.92040
     * Y =  6.48259 -> 14.92040
     * Z =  6.00000 ->  8.00000
     */
    private static final VoxelShape DOUBLE_NORTH =
            Block.box(-7.92040, 6.48259, 6.00000, 23.92040, 14.92040, 8.00000);

    private static final VoxelShape DOUBLE_EAST =
            Block.box(8.00000, 6.48259, -7.92040, 10.00000, 14.92040, 23.92040);

    private static final VoxelShape DOUBLE_SOUTH =
            Block.box(-7.92040, 6.48259, 8.00000, 23.92040, 14.92040, 10.00000);

    private static final VoxelShape DOUBLE_WEST =
            Block.box(6.00000, 6.48259, -7.92040, 8.00000, 14.92040, 23.92040);

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

        double cursorTop = stackCenter + totalHeight / 2.0D;

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

    public static VoxelShape getPanelShape(
            Direction facing,
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
        double firstGap = firstDouble ? DOUBLE_GAP : SIMPLE_GAP;
        double secondGap = secondDouble ? DOUBLE_GAP : SIMPLE_GAP;
        return (firstGap + secondGap) / 2.0D;
    }
}
