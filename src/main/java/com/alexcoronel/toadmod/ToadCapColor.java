package com.alexcoronel.toadmod;

import net.minecraft.util.StringRepresentable;

/** The six cap-patch colours. The ordinal is what is stored in NBT and synced to clients. */
public enum ToadCapColor implements StringRepresentable {
    RED("red"),
    GREEN("green"),
    MAGENTA("magenta"),
    BLUE("blue"),
    LIME("lime"),
    YELLOW("yellow");

    private static final ToadCapColor[] VALUES = values();

    private final String id;

    ToadCapColor(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static ToadCapColor byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}
