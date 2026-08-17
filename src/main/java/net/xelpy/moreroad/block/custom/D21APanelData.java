package net.xelpy.moreroad.block.custom;

public record D21APanelData(
        boolean enabled,
        String destination,
        String distance,
        D21AType type,
        boolean arrowRight,
        boolean autorouteLogo
) {

    public D21APanelData {
        destination = destination == null ? "" : destination;
        distance = distance == null ? "" : distance;
        type = type == null ? D21AType.WHITE : type;

        /*
         * Le pictogramme autoroute est proposé uniquement sur les
         * panneaux verts et bleus. Un panneau blanc force donc la valeur
         * à false, même si une ancienne donnée ou un paquet réseau demande
         * accidentellement le contraire.
         */
        autorouteLogo =
                type != D21AType.WHITE
                        && autorouteLogo;
    }

    public static D21APanelData disabled() {
        return new D21APanelData(
                false,
                "",
                "",
                D21AType.WHITE,
                false,
                false
        );
    }

    public static D21APanelData firstPanelDefault() {
        return new D21APanelData(
                true,
                "",
                "",
                D21AType.WHITE,
                false,
                false
        );
    }
}
