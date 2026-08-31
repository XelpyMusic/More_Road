package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Dessin réel d'une flèche réglementaire, extrait des SVG officiels
 * (D31b ex.2, D31e, DA41a/D62c, DA41f, D62d) plutôt que simulé par rotation
 * d'une unique flèche générique : le dessin, les courbures et les
 * proportions changent réellement d'une variante à l'autre.
 *
 * Chaque valeur correspond à deux textures transparentes (noir/blanc, voir
 * {@code textures/block/motorway_sign/arrows/}) et à un rapport largeur/
 * hauteur propre à sa forme d'origine. Une variante symétrique (miroir
 * horizontal) est un vrai reflet d'un dessin réglementaire existant (voir
 * D62d, où les deux flèches d'un même panneau sont des reflets exacts l'une
 * de l'autre) — {@link GenericDestinationRow#arrowMirrored()} l'applique
 * sans jamais déformer le dessin d'origine.
 */
public enum GenericArrowShape implements StringRepresentable {
    NONE("none", "Aucune", 1.0F),
    /** D31b — exemple 2 : flèche diagonale arrondie, montante. */
    DIAGONAL_ROUNDED("diagonal_rounded", "Diagonale (arrondie)", 511.0F / 512.0F),
    /** D31e : flèche diagonale à pointe nette, descendante. */
    DIAGONAL_SHARP("diagonal_sharp", "Diagonale (nette)", 512.0F / 511.0F),
    /** DA41f : flèche coudée à angle droit. */
    BENT_RIGHT_ANGLE("bent_right_angle", "Coudée (angle droit)", 457.0F / 512.0F),
    /** DA41a / D62c : chevron large, vertical. */
    CHEVRON("chevron", "Chevron", 512.0F / 360.0F),
    /** D62d : flèche diagonale à crochet. */
    HOOKED_DIAGONAL("hooked_diagonal", "Diagonale à crochet", 451.0F / 512.0F);

    private final String serializedName;
    private final String displayName;
    private final float aspectRatio;

    GenericArrowShape(String serializedName, String displayName, float aspectRatio) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.aspectRatio = aspectRatio;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /** Largeur / hauteur du dessin d'origine, à respecter pour ne jamais le déformer. */
    public float aspectRatio() {
        return this.aspectRatio;
    }

    public GenericArrowShape next() {
        GenericArrowShape[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static GenericArrowShape fromSerializedName(String value) {
        if (value != null) {
            for (GenericArrowShape shape : values()) {
                if (shape.serializedName.equals(value)) {
                    return shape;
                }
            }
        }
        return NONE;
    }
}
