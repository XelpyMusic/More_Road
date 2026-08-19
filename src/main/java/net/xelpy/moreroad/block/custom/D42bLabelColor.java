package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Couleur d'encart d'une mention du D42b.
 */
public enum D42bLabelColor implements StringRepresentable {
    NONE("none", "Sans fond"),
    GREEN("green", "Vert"),
    BLUE("blue", "Bleu");

    private final String serializedName;
    private final String displayName;

    D42bLabelColor(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String displayName() {
        return this.displayName;
    }

    public D42bLabelColor next() {
        return switch (this) {
            case NONE -> GREEN;
            case GREEN -> BLUE;
            case BLUE -> NONE;
        };
    }

    public static D42bLabelColor fromSerializedName(String value) {
        if ("green".equals(value)) {
            return GREEN;
        }
        if ("blue".equals(value)) {
            return BLUE;
        }
        return NONE;
    }
}
