package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/** Alignement horizontal du texte d'une ligne de destination, dans sa zone disponible. */
public enum GenericSignAlignment implements StringRepresentable {
    LEFT("left", "Gauche"),
    CENTER("center", "Centré"),
    RIGHT("right", "Droite");

    private final String serializedName;
    private final String displayName;

    GenericSignAlignment(String serializedName, String displayName) {
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

    public GenericSignAlignment next() {
        return switch (this) {
            case LEFT -> CENTER;
            case CENTER -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    public static GenericSignAlignment fromSerializedName(String value) {
        if (value != null) {
            for (GenericSignAlignment alignment : values()) {
                if (alignment.serializedName.equals(value)) {
                    return alignment;
                }
            }
        }
        return CENTER;
    }
}
