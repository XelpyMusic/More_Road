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
        boolean doubleLine,
        RoadTextFont line1Font,
        RoadTextFont line2Font,
        boolean line1Spacing,
        boolean line2Spacing
) {

    public D21APanelData {
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        distance1 = distance1 == null ? "" : distance1;
        distance2 = distance2 == null ? "" : distance2;
        type = type == null ? D21AType.WHITE : type;
        line1Font = line1Font == null ? RoadTextFont.L1 : line1Font;
        line2Font = line2Font == null ? RoadTextFont.L1 : line2Font;

        /*
         * Sur un vrai panneau, l'espacement entre les lettres n'est pas une
         * option : il fait partie de la police. Toujours actif, sans réglage
         * à faire dans le GUI (les champs restent pour la compatibilité NBT
         * et réseau des anciennes versions, mais leur valeur n'a plus
         * d'effet ici).
         */
        line1Spacing = true;
        line2Spacing = true;

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
     * Compatibilité V61 et versions antérieures : si aucune police n'est
     * précisée, les deux lignes utilisent caracteres_l1.
     */
    public D21APanelData(
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
        this(
                enabled,
                line1,
                line2,
                distance1,
                distance2,
                type,
                arrowRight,
                autorouteLogo,
                doubleLine,
                RoadTextFont.L1,
                RoadTextFont.L1,
                false,
                false
        );
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
                false,
                RoadTextFont.L1,
                RoadTextFont.L1,
                false,
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
                doubleLine,
                RoadTextFont.L1,
                RoadTextFont.L1,
                false,
                false
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
                doubleLine,
                RoadTextFont.L1,
                RoadTextFont.L1,
                false,
                false
        );
    }
}
