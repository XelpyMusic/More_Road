package net.xelpy.moreroad.block.custom;

/** Valeur persistante d'un champ texte du panneau autoroutier modulable. */
public record MotorwaySignLineData(
        String text,
        RoadTextFont font,
        MotorwaySignColor color
) {
    public MotorwaySignLineData {
        text = text == null ? "" : text;
        font = font == null ? RoadTextFont.L1 : font;
        color = color == null ? MotorwaySignColor.BLUE : color;
    }

    public static MotorwaySignLineData fromSlot(MotorwaySignSlot slot) {
        return new MotorwaySignLineData(
                slot.defaultText(),
                slot.defaultFont(),
                slot.defaultColor()
        );
    }

    public static MotorwaySignLineData empty() {
        return new MotorwaySignLineData("", RoadTextFont.L1, MotorwaySignColor.BLUE);
    }
}
