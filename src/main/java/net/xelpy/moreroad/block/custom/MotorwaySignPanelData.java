package net.xelpy.moreroad.block.custom;

/** Une plaque libre du panneau autoroutier modulaire. */
public record MotorwaySignPanelData(
        boolean enabled,
        int lineCount,
        String line1,
        String line2,
        String line3,
        String line4,
        String distance1,
        String distance2,
        String distance3,
        String distance4,
        RoadTextFont line1Font,
        RoadTextFont line2Font,
        RoadTextFont line3Font,
        RoadTextFont line4Font,
        MotorwaySignColor background,
        CartoucheType cartoucheType,
        String cartoucheText,
        MotorwaySignGraphic graphic
) {

    public MotorwaySignPanelData {
        lineCount = Math.max(1, Math.min(4, lineCount));
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        line3 = line3 == null ? "" : line3;
        line4 = line4 == null ? "" : line4;
        distance1 = distance1 == null ? "" : distance1;
        distance2 = distance2 == null ? "" : distance2;
        distance3 = distance3 == null ? "" : distance3;
        distance4 = distance4 == null ? "" : distance4;
        line1Font = line1Font == null ? RoadTextFont.L1 : line1Font;
        line2Font = line2Font == null ? RoadTextFont.L1 : line2Font;
        line3Font = line3Font == null ? RoadTextFont.L1 : line3Font;
        line4Font = line4Font == null ? RoadTextFont.L1 : line4Font;
        background = background == null ? MotorwaySignColor.BLUE : background;
        /*
         * Les registres autoroutiers peuvent être en standard blanc (L2) OU
         * en italique (L4) sur fond bleu/vert. On ne transforme donc en L2
         * que l'ancien choix L1 lorsqu'un fond devient foncé ; L4 est
         * conservée et sera teintée en blanc par le renderer.
         */
        line1Font = background.isLight()
                ? RoadTextFont.forceForLightBackground(line1Font)
                : (line1Font == RoadTextFont.L1 ? RoadTextFont.L2 : line1Font);
        line2Font = background.isLight()
                ? RoadTextFont.forceForLightBackground(line2Font)
                : (line2Font == RoadTextFont.L1 ? RoadTextFont.L2 : line2Font);
        line3Font = background.isLight()
                ? RoadTextFont.forceForLightBackground(line3Font)
                : (line3Font == RoadTextFont.L1 ? RoadTextFont.L2 : line3Font);
        line4Font = background.isLight()
                ? RoadTextFont.forceForLightBackground(line4Font)
                : (line4Font == RoadTextFont.L1 ? RoadTextFont.L2 : line4Font);
        cartoucheType = cartoucheType == null ? CartoucheType.NONE : cartoucheType;
        cartoucheText = cartoucheText == null ? "" : cartoucheText;
        graphic = graphic == null ? MotorwaySignGraphic.NONE : graphic;
    }

    public static MotorwaySignPanelData defaultPanel() {
        return new MotorwaySignPanelData(
                false, 1,
                "", "", "", "", "", "", "", "",
                RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1,
                MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
        );
    }

    public static MotorwaySignPanelData disabled() {
        return new MotorwaySignPanelData(
                false, 1,
                "", "", "", "", "", "", "", "",
                RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1,
                MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
        );
    }

    public MotorwaySignPanelData(
            boolean enabled,
            boolean doubleLine,
            String line1,
            String line2,
            String distance1,
            String distance2,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            MotorwaySignColor background,
            CartoucheType cartoucheType,
            String cartoucheText,
            MotorwaySignGraphic graphic
    ) {
        this(
                enabled, doubleLine ? 2 : 1,
                line1, line2, "", "",
                distance1, distance2, "", "",
                line1Font, line2Font, RoadTextFont.L1, RoadTextFont.L1,
                background, cartoucheType, cartoucheText, graphic
        );
    }

    public boolean doubleLine() {
        return this.lineCount >= 2;
    }

    /** Indique si cette entrée décrit une vraie pancarte, au-delà de son cartouche global. */
    public boolean hasPanelContent() {
        return !this.line1.isBlank() || !this.line2.isBlank()
                || !this.line3.isBlank() || !this.line4.isBlank()
                || !this.distance1.isBlank() || !this.distance2.isBlank()
                || !this.distance3.isBlank() || !this.distance4.isBlank()
                || this.graphic != MotorwaySignGraphic.NONE;
    }

    public String line(int index) {
        return switch (index) {
            case 0 -> this.line1;
            case 1 -> this.line2;
            case 2 -> this.line3;
            case 3 -> this.line4;
            default -> "";
        };
    }

    public String distance(int index) {
        return switch (index) {
            case 0 -> this.distance1;
            case 1 -> this.distance2;
            case 2 -> this.distance3;
            case 3 -> this.distance4;
            default -> "";
        };
    }

    public RoadTextFont font(int index) {
        return switch (index) {
            case 0 -> this.line1Font;
            case 1 -> this.line2Font;
            case 2 -> this.line3Font;
            case 3 -> this.line4Font;
            default -> RoadTextFont.L1;
        };
    }

    public static MotorwaySignGraphic parseGraphic(String value) {
        if (value != null) {
            for (MotorwaySignGraphic graphic : MotorwaySignGraphic.values()) {
                if (graphic.name().equalsIgnoreCase(value)) {
                    return graphic;
                }
            }
        }
        return MotorwaySignGraphic.NONE;
    }
}
