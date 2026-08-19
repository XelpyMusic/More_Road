package net.xelpy.moreroad.block.custom;

/**
 * Une branche de sortie du diagramme de giratoire D42b.
 *
 * Convention des angles :
 *  - 0°   = tout droit / haut du panneau ;
 *  - -90° = gauche ;
 *  - +90° = droite.
 */
public record D42bBranchData(
        boolean enabled,
        int angleDegrees,
        String line1,
        String line2,
        RoadTextFont line1Font,
        RoadTextFont line2Font,
        D42bLabelColor line1Color,
        D42bLabelColor line2Color
) {

    public static final int MIN_ANGLE = -170;
    public static final int MAX_ANGLE = 170;

    public D42bBranchData {
        angleDegrees = Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, angleDegrees));
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        line1Font = line1Font == null ? RoadTextFont.L1 : line1Font;
        line2Font = line2Font == null ? RoadTextFont.L1 : line2Font;
        line1Color = line1Color == null ? D42bLabelColor.NONE : line1Color;
        line2Color = line2Color == null ? D42bLabelColor.NONE : line2Color;
    }

    public static D42bBranchData defaultForIndex(int index) {
        return switch (index) {
            case 0 -> new D42bBranchData(
                    true,
                    -90,
                    "",
                    "",
                    RoadTextFont.L1,
                    RoadTextFont.L1,
                    D42bLabelColor.NONE,
                    D42bLabelColor.NONE
            );
            case 1 -> new D42bBranchData(
                    true,
                    0,
                    "",
                    "",
                    RoadTextFont.L1,
                    RoadTextFont.L1,
                    D42bLabelColor.NONE,
                    D42bLabelColor.NONE
            );
            case 2 -> new D42bBranchData(
                    true,
                    90,
                    "",
                    "",
                    RoadTextFont.L1,
                    RoadTextFont.L1,
                    D42bLabelColor.NONE,
                    D42bLabelColor.NONE
            );
            case 3 -> disabled(-45);
            case 4 -> disabled(45);
            default -> disabled(135);
        };
    }

    public static D42bBranchData disabled(int angleDegrees) {
        return new D42bBranchData(
                false,
                angleDegrees,
                "",
                "",
                RoadTextFont.L1,
                RoadTextFont.L1,
                D42bLabelColor.NONE,
                D42bLabelColor.NONE
        );
    }
}
