package net.xelpy.moreroad.block.custom;

/**
 * En-tête optionnel du panneau directionnel modulable — distinct des
 * cartouches de route ET des lignes de destination (voir le schéma fourni :
 * cartouches au-dessus, en-tête entre les deux, destinations en dessous).
 *
 * Désactivé par défaut (panneau vierge). "sameAsPanel" évite d'exposer un
 * réglage de couleur redondant quand le joueur veut simplement le même fond
 * que le reste du panneau.
 */
public record GenericSignHeader(
        boolean enabled,
        String text,
        boolean sameAsPanel,
        MotorwaySignColor color,
        GenericSignAlignment alignment,
        RoadTextFont font
) {
    public GenericSignHeader {
        text = text == null ? "" : text;
        color = sanitizeColor(color);
        alignment = alignment == null ? GenericSignAlignment.CENTER : alignment;
        font = font == null ? RoadTextFont.L1 : font;
    }

    private static MotorwaySignColor sanitizeColor(MotorwaySignColor color) {
        return switch (color) {
            case GREEN, WHITE -> color;
            case null, default -> MotorwaySignColor.BLUE;
        };
    }

    /** Couleur réellement affichée, une fois "même couleur que le panneau" résolu. */
    public MotorwaySignColor effectiveColor(MotorwaySignColor panelBackground) {
        return this.sameAsPanel ? panelBackground : this.color;
    }

    /**
     * Même règle L1/L2 que le reste du mod, appliquée à la couleur
     * effectivement affichée (donc aussi quand "même couleur que le
     * panneau" est actif et que ce fond change).
     */
    public GenericSignHeader withFontForBackground(MotorwaySignColor panelBackground) {
        MotorwaySignColor effective = effectiveColor(panelBackground);
        RoadTextFont resolved = effective.isLight()
                ? RoadTextFont.forceForLightBackground(this.font)
                : RoadTextFont.forceForDarkBackground(this.font);
        return resolved == this.font ? this : new GenericSignHeader(
                this.enabled, this.text, this.sameAsPanel, this.color, this.alignment, resolved
        );
    }

    public static GenericSignHeader blank() {
        return new GenericSignHeader(false, "", true, MotorwaySignColor.BLUE, GenericSignAlignment.CENTER, RoadTextFont.L1);
    }
}
