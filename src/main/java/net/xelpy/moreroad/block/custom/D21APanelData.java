package net.xelpy.moreroad.block.custom;

public record D21APanelData(
        boolean enabled,
        String line1,
        String line2,
        String distance1,
        String distance2,
        D21AType type,
        boolean arrowRight,
        boolean autorouteLogo,
        boolean doubleLine
) {

    public D21APanelData {
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        distance1 = distance1 == null ? "" : distance1;
        distance2 = distance2 == null ? "" : distance2;
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

    /*
     * Constructeur de compatibilité avec les fichiers qui créaient encore
     * un panneau sans préciser son format. Dans ce cas, il reste simple.
     */
    public D21APanelData(
            boolean enabled,
            String line1,
            String line2,
            String distance1,
            String distance2,
            D21AType type,
            boolean arrowRight,
            boolean autorouteLogo
    ) {
        this(
                enabled,
                line1,
                line2,
                distance1,
                distance2,
                type,
                arrowRight,
                autorouteLogo,
                false
        );
    }

    /*
     * Compatibilité avec l'ancien code D21A à une seule destination.
     */
    public String destination() {
        return this.line1;
    }

    /*
     * Compatibilité avec l'ancien code D21A à une seule distance.
     * Si seule la distance de la ligne 2 existe, elle devient la distance
     * unique utilisable par un panneau simple.
     */
    public String distance() {
        if (!this.distance1.isBlank()) {
            return this.distance1;
        }

        return this.distance2;
    }

    public static D21APanelData disabled() {
        return disabled(false);
    }

    public static D21APanelData disabled(boolean doubleLine) {
        return new D21APanelData(
                false,
                "",
                "",
                "",
                "",
                D21AType.WHITE,
                false,
                false,
                doubleLine
        );
    }

    public static D21APanelData firstPanelDefault() {
        return firstPanelDefault(false);
    }

    public static D21APanelData firstPanelDefault(boolean doubleLine) {
        return new D21APanelData(
                true,
                "",
                "",
                "",
                "",
                D21AType.WHITE,
                false,
                false,
                doubleLine
        );
    }
}
