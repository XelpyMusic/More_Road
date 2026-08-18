package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Types de cartouches de localisation utilisables au-dessus des panneaux
 * directionnels et des panneaux d'agglomération.
 *
 * Prend en charge E41/E45, E42, E43, E44 et E47.
 */
public enum CartoucheType implements StringRepresentable {

    NONE("none", "Aucun"),
    E41_45("e41_45", "E41 / E45 - Vert"),
    E42("e42", "E42 - Rouge"),
    E43("e43", "E43 - Jaune"),
    E44("e44", "E44 - Blanc"),
    E47("e47", "E47 - Bleu");

    private final String serializedName;
    private final String displayName;

    CartoucheType(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isVisible() {
        return this != NONE;
    }

    public CartoucheType next() {
        CartoucheType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static CartoucheType fromSerializedName(String value) {
        if (value != null) {
            for (CartoucheType type : values()) {
                if (type.getSerializedName().equals(value)) {
                    return type;
                }
            }
        }

        return NONE;
    }
}
