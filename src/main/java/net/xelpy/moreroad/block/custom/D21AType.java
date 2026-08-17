package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

public enum D21AType implements StringRepresentable {

    WHITE("white"),
    GREEN("green"),
    BLUE("blue");

    private final String name;

    D21AType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}