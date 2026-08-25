package net.xelpy.moreroad.block.custom;

/**
 * Couleurs normalisées utilisées par les plaques autoroutières modulables.
 * La couleur du texte est déduite du contraste réglementaire du fond.
 */
public enum MotorwaySignColor {

    WHITE("white", "Blanc", 0xFFF2F2EE, 0xFF111111),
    BLUE("blue", "Bleu", 0xFF063AA7, 0xFFFFFFFF),
    GREEN("green", "Vert", 0xFF14833B, 0xFFFFFFFF),
    YELLOW("yellow", "Jaune", 0xFFF0CF22, 0xFF111111),
    RED("red", "Rouge", 0xFFC51D20, 0xFFFFFFFF),
    BLACK("black", "Noir", 0xFF111111, 0xFFFFFFFF),
    BROWN("brown", "Marron", 0xFF70452C, 0xFFFFFFFF);

    private final String serializedName;
    private final String displayName;
    private final int argb;
    private final int textArgb;

    MotorwaySignColor(String serializedName, String displayName, int argb, int textArgb) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.argb = argb;
        this.textArgb = textArgb;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getArgb() {
        return this.argb;
    }

    public int getTextArgb() {
        return this.textArgb;
    }

    public boolean isLight() {
        return this == WHITE || this == YELLOW;
    }

    public MotorwaySignColor next() {
        MotorwaySignColor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static MotorwaySignColor fromSerializedName(String value) {
        if (value != null) {
            for (MotorwaySignColor color : values()) {
                if (color.serializedName.equals(value)) {
                    return color;
                }
            }
        }
        return BLUE;
    }
}
