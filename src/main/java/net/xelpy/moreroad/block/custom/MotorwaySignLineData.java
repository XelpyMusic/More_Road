package net.xelpy.moreroad.block.custom;

/** Valeur persistante d'un champ texte du panneau autoroutier modulable. */
public record MotorwaySignLineData(
        String text,
        RoadTextFont font,
        MotorwaySignColor color
) {
    public MotorwaySignLineData {
        text = text == null ? "" : text;
        font = font == null ? RoadTextFont.L1 : font;
        color = color == null ? MotorwaySignColor.BLUE : color;
        /*
         * Sur les panneaux autoroutiers, L4 doit rester un vrai choix de
         * style même sur fond bleu/vert : le renderer teinte ses glyphes en
         * blanc. Seule L1 (standard sombre) doit donc devenir L2 sur fond
         * foncé. C'est important pour les distances en italique (ex. D41c)
         * et pour les villes des registres personnalisables.
         */
        font = color.isLight()
                ? RoadTextFont.forceForLightBackground(font)
                : (font == RoadTextFont.L1 ? RoadTextFont.L2 : font);
    }

    public static MotorwaySignLineData fromSlot(MotorwaySignSlot slot) {
        return new MotorwaySignLineData(
                slot.defaultText(),
                slot.defaultFont(),
                slot.defaultColor()
        );
    }

    /**
     * Valeur vide pour un nouveau panneau : le SVG reste uniquement le gabarit
     * graphique. On conserve toutefois la police et la couleur prévues par le
     * type de champ afin que l'éditeur parte avec les bons réglages.
     */
    public static MotorwaySignLineData blankForSlot(MotorwaySignSlot slot) {
        if (slot == null) {
            return empty();
        }
        return new MotorwaySignLineData(
                "",
                slot.defaultFont(),
                slot.defaultColor()
        );
    }

    public static MotorwaySignLineData empty() {
        return new MotorwaySignLineData("", RoadTextFont.L1, MotorwaySignColor.BLUE);
    }
}
