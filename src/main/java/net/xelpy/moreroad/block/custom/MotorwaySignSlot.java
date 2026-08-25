package net.xelpy.moreroad.block.custom;

/** Définition d'un champ éditable appartenant à un préréglage. */
public record MotorwaySignSlot(
        String label,
        String defaultText,
        RoadTextFont defaultFont,
        MotorwaySignColor defaultColor,
        int panelGroup,
        MotorwaySignRole role
) {
    public MotorwaySignSlot {
        label = label == null ? "Texte" : label;
        defaultText = defaultText == null ? "" : defaultText;
        defaultFont = defaultFont == null ? RoadTextFont.L1 : defaultFont;
        defaultColor = defaultColor == null ? MotorwaySignColor.BLUE : defaultColor;
        role = role == null ? MotorwaySignRole.DESTINATION : role;
    }
}
