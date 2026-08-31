package net.xelpy.moreroad.block.custom;

/**
 * Une ligne de destination du panneau directionnel modulable.
 *
 * La flèche est un véritable dessin réglementaire choisi parmi
 * {@link GenericArrowShape} (extrait des SVG officiels), jamais une flèche
 * unique tournée artificiellement pour simuler les autres — seul un miroir
 * horizontal ({@link #arrowMirrored()}) est appliqué, ce qui reproduit un
 * vrai reflet réglementaire (voir D62d) sans jamais déformer le dessin.
 */
public record GenericDestinationRow(
        boolean enabled,
        String text,
        GenericSignAlignment alignment,
        RoadTextFont font,
        boolean arrowEnabled,
        GenericArrowShape arrowShape,
        boolean arrowMirrored,
        D61AArrowPosition arrowPosition,
        boolean symbolEnabled,
        GenericSignSymbol symbol,
        D61AArrowPosition symbolPosition
) {

    public GenericDestinationRow {
        text = text == null ? "" : text;
        alignment = alignment == null ? GenericSignAlignment.CENTER : alignment;
        font = font == null ? RoadTextFont.L1 : font;
        arrowShape = arrowShape == null ? GenericArrowShape.NONE : arrowShape;
        arrowEnabled = arrowEnabled && arrowShape != GenericArrowShape.NONE;
        arrowPosition = arrowPosition == null ? D61AArrowPosition.RIGHT : arrowPosition;
        symbol = symbol == null ? GenericSignSymbol.NONE : symbol;
        symbolEnabled = symbolEnabled && symbol != GenericSignSymbol.NONE;
        symbolPosition = symbolPosition == null ? D61AArrowPosition.LEFT : symbolPosition;
    }

    /**
     * La police n'est pas indépendante du fond : L1 pour un fond clair
     * (blanc), L2 pour un vrai texte blanc sur fond foncé (bleu/vert). Même
     * règle que le reste du mod (voir MotorwaySignPanelData), appliquée ici
     * par le conteneur (GenericDirectionalSignData) qui connaît le fond.
     */
    public GenericDestinationRow withFontForBackground(MotorwaySignColor background) {
        RoadTextFont resolved = background != null && background.isLight()
                ? RoadTextFont.forceForLightBackground(this.font)
                : RoadTextFont.forceForDarkBackground(this.font);
        return resolved == this.font ? this : new GenericDestinationRow(
                this.enabled, this.text, this.alignment, resolved,
                this.arrowEnabled, this.arrowShape, this.arrowMirrored, this.arrowPosition,
                this.symbolEnabled, this.symbol, this.symbolPosition
        );
    }

    public static GenericDestinationRow blank() {
        return new GenericDestinationRow(
                false, "", GenericSignAlignment.CENTER, RoadTextFont.L1,
                false, GenericArrowShape.NONE, false, D61AArrowPosition.RIGHT,
                false, GenericSignSymbol.NONE, D61AArrowPosition.LEFT
        );
    }

    /** Une destination possède un contenu réel au-delà de son activation. */
    public boolean hasContent() {
        return !this.text.isBlank() || this.arrowEnabled || this.symbolEnabled;
    }
}
