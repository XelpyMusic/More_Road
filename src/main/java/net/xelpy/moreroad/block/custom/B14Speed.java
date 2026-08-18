package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Valeurs B14 actuellement disponibles dans More Road.
 *
 * Les noms sérialisés correspondent directement aux modèles existants :
 * b14_5, b14_10, b14_15, etc.
 */
public enum B14Speed implements StringRepresentable {

    KMH_5(5),
    KMH_10(10),
    KMH_15(15),
    KMH_20(20),
    KMH_30(30),
    KMH_40(40),
    KMH_45(45),
    KMH_50(50),
    KMH_60(60),
    KMH_70(70),
    KMH_80(80),
    KMH_90(90),
    KMH_100(100),
    KMH_110(110),
    KMH_130(130);

    private final int value;
    private final String serializedName;

    B14Speed(int value) {
        this.value = value;
        this.serializedName = Integer.toString(value);
    }

    public int value() {
        return this.value;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public static B14Speed fromSerializedName(String value) {
        if (value != null) {
            for (B14Speed speed : values()) {
                if (speed.getSerializedName().equals(value)) {
                    return speed;
                }
            }
        }

        return KMH_5;
    }
}
