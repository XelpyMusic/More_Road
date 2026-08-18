package net.xelpy.moreroad.block.custom;

/**
 * Données propres au système D61A.
 *
 * Elles sont volontairement séparées de D21APanelData car le D61A possède
 * ses propres flèches directionnelles et son option de logo autoroute.
 */
public record D61APanelData(
        boolean enabled,
        String line1,
        String line2,
        String distance1,
        String distance2,
        D21AType type,
        boolean doubleLine,
        boolean arrowEnabled,
        D61AArrowPosition arrowPosition,
        D61AArrowDirection arrowDirection,
        boolean autorouteLogo,
        RoadTextFont line1Font,
        RoadTextFont line2Font
) {

    public D61APanelData {
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        distance1 = distance1 == null ? "" : distance1;
        distance2 = distance2 == null ? "" : distance2;
        line1Font = line1Font == null ? RoadTextFont.L1 : line1Font;
        line2Font = line2Font == null ? RoadTextFont.L1 : line2Font;

        type = switch (type == null ? D21AType.WHITE : type) {
            case GREEN -> D21AType.GREEN;
            case BLUE -> D21AType.BLUE;
            default -> D21AType.WHITE;
        };

        arrowPosition = arrowPosition == null
                ? D61AArrowPosition.RIGHT
                : arrowPosition;

        arrowDirection = arrowDirection == null
                ? D61AArrowDirection.UP
                : arrowDirection;

        // Comme sur les panneaux directionnels : jamais de logo sur blanc.
        autorouteLogo = type != D21AType.WHITE && autorouteLogo;
    }

    /*
     * Compatibilité V61 et versions antérieures : les lignes utilisent L1.
     */
    public D61APanelData(
            boolean enabled,
            String line1,
            String line2,
            String distance1,
            String distance2,
            D21AType type,
            boolean doubleLine,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
            D61AArrowDirection arrowDirection,
            boolean autorouteLogo
    ) {
        this(
                enabled,
                line1,
                line2,
                distance1,
                distance2,
                type,
                doubleLine,
                arrowEnabled,
                arrowPosition,
                arrowDirection,
                autorouteLogo,
                RoadTextFont.L1,
                RoadTextFont.L1
        );
    }

    public String destination() {
        return this.line1;
    }

    public String distance() {
        if (!this.distance1.isBlank()) {
            return this.distance1;
        }

        return this.distance2;
    }

    public static D61APanelData disabled() {
        return disabled(false);
    }

    public static D61APanelData disabled(boolean doubleLine) {
        return new D61APanelData(
                false,
                "",
                "",
                "",
                "",
                D21AType.WHITE,
                doubleLine,
                false,
                D61AArrowPosition.RIGHT,
                D61AArrowDirection.UP,
                false,
                RoadTextFont.L1,
                RoadTextFont.L1
        );
    }

    public static D61APanelData firstPanelDefault() {
        return firstPanelDefault(false);
    }

    public static D61APanelData firstPanelDefault(boolean doubleLine) {
        return new D61APanelData(
                true,
                "",
                "",
                "",
                "",
                D21AType.WHITE,
                doubleLine,
                false,
                D61AArrowPosition.RIGHT,
                D61AArrowDirection.UP,
                false,
                RoadTextFont.L1,
                RoadTextFont.L1
        );
    }
}
