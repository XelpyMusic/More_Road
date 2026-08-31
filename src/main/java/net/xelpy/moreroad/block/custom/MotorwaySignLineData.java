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
         * La police n'est pas indépendante de la couleur de fond : L1/L4
         * dessinent un texte sombre prévu pour un fond clair, L2 dessine un
         * vrai texte blanc prévu pour un fond foncé (bleu, vert, rouge,
         * noir, marron, bleu métropolitain...). Forcé ici, dans le
         * constructeur, pour couvrir TOUTES les sources de données (valeurs
         * par défaut des préréglages, GUI, réseau, anciennes sauvegardes) —
         * pas seulement les changements de couleur faits à la main dans le
         * GUI, qui laissaient les couleurs par défaut des préréglages avec
         * une police jamais corrigée.
         */
        font = color.isLight()
                ? RoadTextFont.forceForLightBackground(font)
                : RoadTextFont.forceForDarkBackground(font);
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
