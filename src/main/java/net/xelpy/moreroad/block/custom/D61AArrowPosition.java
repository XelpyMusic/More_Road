package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

public enum D61AArrowPosition implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    D61AArrowPosition(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public D61AArrowPosition opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    public static D61AArrowPosition fromSerializedName(String value) {
        return "left".equals(value) ? LEFT : RIGHT;
    }
}
