package net.xelpy.moreroad.block.custom;

import java.util.Arrays;

/**
 * État complet d'un panneau directionnel modulable : un fond, un en-tête
 * optionnel, une pile de destinations et jusqu'à trois cartouches de route.
 *
 * Volontairement plat et data-driven (pas de préréglage figé, pas de texture
 * SVG figée par modèle) : le moteur générique (renderer + géométrie) ne lit
 * que cet état, jamais un identifiant de préréglage.
 */
public record GenericDirectionalSignData(
        MotorwaySignColor background,
        GenericSignHeader header,
        GenericDestinationRow[] rows,
        GenericRouteCartoucheData[] cartouches
) {
    public static final int MAX_ROWS = 6;
    public static final int MAX_CARTOUCHES = 3;

    public GenericDirectionalSignData {
        background = sanitizeBackground(background);
        header = (header == null ? GenericSignHeader.blank() : header).withFontForBackground(background);
        rows = normalizeRows(rows, background);
        cartouches = normalizeCartouches(cartouches);
    }

    private static MotorwaySignColor sanitizeBackground(MotorwaySignColor color) {
        return switch (color) {
            case GREEN, WHITE -> color;
            case null, default -> MotorwaySignColor.BLUE;
        };
    }

    private static GenericDestinationRow[] normalizeRows(GenericDestinationRow[] source, MotorwaySignColor background) {
        GenericDestinationRow[] result = new GenericDestinationRow[MAX_ROWS];
        for (int i = 0; i < MAX_ROWS; i++) {
            GenericDestinationRow row = source != null && i < source.length && source[i] != null
                    ? source[i]
                    : GenericDestinationRow.blank();
            result[i] = row.withFontForBackground(background);
        }
        return result;
    }

    private static GenericRouteCartoucheData[] normalizeCartouches(GenericRouteCartoucheData[] source) {
        GenericRouteCartoucheData[] result = new GenericRouteCartoucheData[MAX_CARTOUCHES];
        for (int i = 0; i < MAX_CARTOUCHES; i++) {
            result[i] = source != null && i < source.length && source[i] != null
                    ? source[i]
                    : GenericRouteCartoucheData.blank();
        }
        return result;
    }

    public static GenericDirectionalSignData blank() {
        return new GenericDirectionalSignData(MotorwaySignColor.BLUE, null, null, null);
    }

    /**
     * Lignes réellement utiles (activées ET avec un contenu) : une ligne
     * activée mais laissée vide ne doit ni réserver de hauteur, ni créer un
     * grand vide visuel — même filtre utilisé par la géométrie (hitbox) et
     * le renderer, pour qu'ils ne divergent jamais.
     */
    public int enabledRowCount() {
        int count = 0;
        for (GenericDestinationRow row : this.rows) {
            if (row.enabled() && row.hasContent()) {
                count++;
            }
        }
        return count;
    }

    public int visibleCartoucheCount() {
        int count = 0;
        for (GenericRouteCartoucheData cartouche : this.cartouches) {
            if (cartouche.isVisible()) {
                count++;
            }
        }
        return count;
    }

    public GenericDirectionalSignData withRow(int index, GenericDestinationRow row) {
        GenericDestinationRow[] copy = Arrays.copyOf(this.rows, this.rows.length);
        copy[index] = row;
        return new GenericDirectionalSignData(this.background, this.header, copy, this.cartouches);
    }

    public GenericDirectionalSignData withCartouche(int index, GenericRouteCartoucheData cartouche) {
        GenericRouteCartoucheData[] copy = Arrays.copyOf(this.cartouches, this.cartouches.length);
        copy[index] = cartouche;
        return new GenericDirectionalSignData(this.background, this.header, this.rows, copy);
    }

    public GenericDirectionalSignData withBackground(MotorwaySignColor background) {
        return new GenericDirectionalSignData(background, this.header, this.rows, this.cartouches);
    }

    public GenericDirectionalSignData withHeader(GenericSignHeader header) {
        return new GenericDirectionalSignData(this.background, header, this.rows, this.cartouches);
    }
}
