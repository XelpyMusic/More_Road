package net.xelpy.moreroad.block.custom;

/**
 * Dimensions physiques du panneau directionnel modulable, partagées par le
 * renderer et la hitbox — même principe de source unique que
 * {@link MotorwaySignGeometry}, et volontairement sans aucune dépendance
 * client (pas de police, pas de texture) pour rester utilisable côté bloc.
 *
 * Échelle de référence reprise du VRAI D31b existant (voir
 * {@link MotorwaySignGeometry#forPreset}) plutôt qu'inventée : même
 * WORLD_SCALE, même hauteur de pose (POLE_PANEL_BOTTOM), et une largeur
 * minimale alignée sur D31b — exemple 1 (4,20 m, le plus petit des deux
 * gabarits D31b mesurés sur SVG) pour qu'un panneau à peu de contenu ne
 * paraisse plus miniature/carré comparé aux vrais panneaux du mod.
 */
public record GenericDirectionalSignGeometry(
        float width,
        float height,
        float panelBottom,
        float supportTop,
        /*
         * Chiffres "unités de conception" (avant l'échelle WORLD_SCALE)
         * partagés avec le renderer, pour que le corps réellement dessiné et
         * la hitbox ne divergent jamais (même principe de source unique que
         * MotorwaySignGeometry) : le renderer n'a jamais à refaire ce calcul
         * de son côté.
         */
        float designWidth,
        float designHeight,
        int enabledRowCount,
        int visibleCartoucheCount,
        boolean headerEnabled,
        float lineStep
) {
    public static final float WORLD_SCALE = MotorwaySignGeometry.WORLD_SCALE;
    public static final float PANEL_GAP = 0.075F;
    public static final float CARTOUCHE_HEIGHT = 0.55F;
    /** Largeur réservée sur le côté d'une ligne pour une flèche ou un symbole (utilisé par le renderer aussi). */
    public static final float ROW_ICON_RESERVE = 0.55F;
    /** Marge gauche/droite du panneau, cohérente entre la hitbox et le renderer. */
    public static final float ROW_MARGIN = 0.30F;
    /** Hauteur de la bande d'en-tête, une ligne de texte comme une destination simple. */
    public static final float HEADER_HEIGHT = 0.70F;
    private static final float PANEL_BOTTOM = MotorwaySignGeometry.POLE_PANEL_BOTTOM;
    /*
     * D31b — exemple 1 : largeur 4,20 m (voir MotorwaySignGeometry.forPreset,
     * case D31B_EX1). Reprise ici comme largeur MINIMALE du panneau
     * générique — pas une valeur arbitraire — pour que même un contenu
     * réduit reste à l'échelle des vrais panneaux directionnels du mod.
     * D31b — exemple 2 (6,00 m) reste couvert par la marge jusqu'à
     * MAX_PANEL_WIDTH pour un contenu plus riche (plusieurs destinations +
     * cartouches).
     */
    private static final float MIN_PANEL_WIDTH = 4.20F;
    private static final float MAX_PANEL_WIDTH = 6.80F;

    public static GenericDirectionalSignGeometry forData(GenericDirectionalSignData data) {
        GenericDirectionalSignData safe = data == null ? GenericDirectionalSignData.blank() : data;

        float rowWidth = 0.0F;
        int enabledRows = 0;
        for (GenericDestinationRow row : safe.rows()) {
            if (!row.enabled() || !row.hasContent()) {
                continue;
            }
            enabledRows++;
            float reserve = (row.arrowEnabled() ? ROW_ICON_RESERVE : 0.0F) + (row.symbolEnabled() ? ROW_ICON_RESERVE : 0.0F);
            rowWidth = Math.max(rowWidth, estimatedTextWidth(row.text(), safe.background()) + 0.72F + reserve);
        }

        boolean headerEnabled = safe.header().enabled();
        if (headerEnabled) {
            rowWidth = Math.max(rowWidth, estimatedTextWidth(safe.header().text(), safe.header().effectiveColor(safe.background())) + 0.72F);
        }
        if (enabledRows == 0 && !headerEnabled) {
            rowWidth = 0.0F;
        }

        float cartoucheWidth = 0.0F;
        int visibleCartouches = 0;
        for (GenericRouteCartoucheData cartouche : safe.cartouches()) {
            if (!cartouche.isVisible()) {
                continue;
            }
            visibleCartouches++;
            cartoucheWidth += clamp(estimatedTextWidth(cartouche.text(), MotorwaySignColor.WHITE) + 0.38F, 1.02F, 2.20F);
        }
        if (visibleCartouches > 1) {
            cartoucheWidth += PANEL_GAP * (visibleCartouches - 1);
        }

        float width = clamp(Math.max(rowWidth, cartoucheWidth), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);

        float lineStep = safe.background().isLight() ? 0.39F : 0.45F;
        float height = 0.0F;
        if (enabledRows > 0) {
            height += 0.46F + lineStep * enabledRows;
        }
        if (headerEnabled) {
            height += HEADER_HEIGHT + (enabledRows > 0 ? PANEL_GAP : 0.0F);
        }
        if (visibleCartouches > 0) {
            height += CARTOUCHE_HEIGHT + PANEL_GAP;
        }
        if (enabledRows == 0 && !headerEnabled) {
            /* Panneau vraiment vierge : hauteur minimale pour rester visible/manipulable. */
            height = 0.46F + lineStep;
        }

        float scaledHeight = height * WORLD_SCALE;
        return new GenericDirectionalSignGeometry(
                width * WORLD_SCALE,
                scaledHeight,
                PANEL_BOTTOM,
                PANEL_BOTTOM + scaledHeight / 2.0F,
                width,
                height,
                enabledRows,
                visibleCartouches,
                headerEnabled,
                lineStep
        );
    }

    private static float estimatedTextWidth(String text, MotorwaySignColor background) {
        String safeText = text == null ? "" : text;
        int characters = safeText.codePointCount(0, safeText.length());
        float averageCharacterWidth = background != null && background.isLight() ? 0.145F : 0.165F;
        return characters * averageCharacterWidth;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
