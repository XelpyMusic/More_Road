package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/** Flèches sélectionnables pour les deux sorties du panneau DA31C. */
public enum DA31CArrowType implements StringRepresentable {

    NONE("none", "Aucune"),
    DOWN("down", "Bas"),
    LEFT("left", "Gauche"),
    RIGHT("right", "Droite");

    private final String serializedName;
    private final String displayName;

    DA31CArrowType(String serializedName, String displayName) {
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

    public DA31CArrowType next() {
        DA31CArrowType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static DA31CArrowType fromSerializedName(String value) {
        if (value != null) {
            for (DA31CArrowType type : values()) {
                if (type.serializedName.equals(value)) {
                    return type;
                }
            }
        }
        return DOWN;
    }
}
