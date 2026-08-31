package net.xelpy.moreroad.block.custom;

/**
 * Un cartouche de numéro de route au-dessus du panneau directionnel
 * modulable. {@link CartoucheType#NONE} signifie "pas de cartouche à cet
 * emplacement" — pas besoin d'un booléen d'activation séparé.
 *
 * Couleurs volontairement restreintes à rouge/jaune/vert/bleu métropolitain
 * (E42/E43/E41_45/E47) : ni blanc, ni noir, ni marron pour un cartouche de
 * route, conformément aux panneaux réels.
 */
public record GenericRouteCartoucheData(CartoucheType type, String text) {

    public GenericRouteCartoucheData {
        text = text == null ? "" : text;
        type = sanitize(type);
    }

    private static CartoucheType sanitize(CartoucheType type) {
        if (type == null) {
            return CartoucheType.NONE;
        }
        return switch (type) {
            case E41_45, E42, E43, E47 -> type;
            default -> CartoucheType.NONE;
        };
    }

    public boolean isVisible() {
        return this.type != CartoucheType.NONE;
    }

    /**
     * Couleur de fond réelle du cartouche. Le bleu de route (E47) est
     * volontairement le bleu métropolitain, distinct du bleu autoroutier
     * historique utilisé pour le fond du panneau principal.
     */
    public MotorwaySignColor backgroundColor() {
        return switch (this.type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E47 -> MotorwaySignColor.METROPOLITAN_BLUE;
            default -> MotorwaySignColor.RED;
        };
    }

    public static GenericRouteCartoucheData blank() {
        return new GenericRouteCartoucheData(CartoucheType.NONE, "");
    }
}
