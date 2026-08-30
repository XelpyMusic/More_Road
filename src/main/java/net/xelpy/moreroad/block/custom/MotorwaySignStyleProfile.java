package net.xelpy.moreroad.block.custom;

/**
 * Règles de mise en page communes aux panneaux autoroutiers modulables.
 *
 * L'objectif est d'éviter les corrections dispersées modèle par modèle :
 * le renderer, l'éditeur, le réseau et la géométrie lisent tous ce même
 * profil. Les rares particularités réglementaires restent donc regroupées
 * ici, et une correction de famille profite à tous les panneaux concernés.
 */
public record MotorwaySignStyleProfile(
        float addedTextScale,
        float addedLineStep,
        float addedLeftMargin,
        float addedRightMargin,
        float addedDistanceGap,
        float addedOpticalYOffset,
        boolean allowsCustomDistances,
        boolean allowsCustomCartouche,
        boolean forceBlueCustomPanels,
        boolean normalizeMainDestinationStack,
        /*
         * Les registres supplémentaires (onglets "Registre N" de l'éditeur
         * générique) s'ajoutent au-dessus du panneau, quel que soit le
         * modèle. Ça a du sens pour la plupart, mais pas pour un panneau au
         * dessin réglementaire figé comme D44 : les masquer évite d'exposer
         * une fonctionnalité qui ne correspond à aucun cas d'usage réel.
         */
        boolean allowsExtraPanels
) {

    private static final float DEFAULT_TEXT_SCALE = 0.044F;

    public static MotorwaySignStyleProfile forPreset(MotorwaySignPreset preset) {
        MotorwaySignPreset safePreset = preset == null ? MotorwaySignPreset.FREEFORM : preset;

        float textScale = preferredAddedTextScale(safePreset);
        float lineStep = preferredAddedLineStep(safePreset, textScale);
        float leftMargin = switch (safePreset) {
            case D31B_EX1 -> 0.13F;
            case D31B_EX2 -> 0.09F;
            case D63C -> 0.34F;
            default -> 0.32F;
        };
        float rightMargin = safePreset == MotorwaySignPreset.D63C ? 0.38F : 0.28F;
        float distanceGap = safePreset == MotorwaySignPreset.D63C ? 0.34F : 0.30F;
        float opticalYOffset = safePreset == MotorwaySignPreset.D63C ? -0.045F : -0.055F;

        return new MotorwaySignStyleProfile(
                textScale,
                lineStep,
                leftMargin,
                rightMargin,
                distanceGap,
                opticalYOffset,
                safePreset != MotorwaySignPreset.D31B_EX1
                        && safePreset != MotorwaySignPreset.D31B_EX2,
                safePreset != MotorwaySignPreset.D31B_EX1
                        && safePreset != MotorwaySignPreset.D31B_EX2,
                safePreset == MotorwaySignPreset.D61B,
                safePreset == MotorwaySignPreset.D31B_EX2 || safePreset == MotorwaySignPreset.D31B_EX1,
                safePreset != MotorwaySignPreset.D44
        );
    }

    /**
     * Taille de référence des lames ajoutées. Pour les modèles reproduits
     * exactement depuis les SVG, la valeur est alignée sur la taille visuelle
     * du panneau principal afin que les ajouts ne paraissent plus appartenir
     * à un autre système graphique.
     */
    private static float preferredAddedTextScale(MotorwaySignPreset preset) {
        return switch (preset) {
            case FREEFORM -> 0.060F;
            case D31B_EX1 -> 0.044F;
            case D31B_EX2 -> 0.063F;
            case D31D -> 0.054F;
            case D31E -> 0.062F;
            case D32A, D32A_DC -> 0.048F;
            case D32B -> 0.049F;
            case D41A -> 0.054F;
            case D41B -> 0.058F;
            case D41C -> 0.056F;
            case D61B -> 0.060F;
            case D62A -> 0.073F;
            case D62B -> 0.066F;
            case D62D_TOP -> 0.054F;
            case D62D_BOTTOM -> 0.053F;
            case D63C -> 0.064F;
            case D63D -> 0.070F;
            case D71 -> 0.040F;
            case D72 -> 0.039F;
            case DA31A -> 0.053F;
            case DA31B -> 0.055F;
            case DA31D -> 0.049F;
            case DA31E -> 0.060F;
            case DA31F -> 0.073F;
            case DA32A, DA32A_DC -> 0.056F;
            case DA32B, DA32B_DC -> 0.071F;
            default -> DEFAULT_TEXT_SCALE;
        };
    }

    private static float preferredAddedLineStep(MotorwaySignPreset preset, float textScale) {
        if (preset == MotorwaySignPreset.D63C) {
            return 0.68F;
        }
        float proportional = 0.45F * textScale / DEFAULT_TEXT_SCALE;
        return clamp(proportional, 0.40F, 0.58F);
    }

    public float addedPanelHeight(int lineCount, MotorwaySignGraphic graphic) {
        int safeCount = Math.max(1, Math.min(4, lineCount));
        float perLine = Math.max(0.40F, this.addedLineStep - 0.05F);
        float height = 0.48F + perLine * safeCount;
        if (usesBottomArrow(graphic)) {
            height += 0.50F;
        }
        return height;
    }

    public MotorwaySignColor sanitizeCustomBackground(MotorwaySignColor color) {
        if (this.forceBlueCustomPanels) {
            return MotorwaySignColor.BLUE;
        }
        return color == MotorwaySignColor.GREEN || color == MotorwaySignColor.WHITE
                ? color
                : MotorwaySignColor.BLUE;
    }

    /**
     * Un cartouche routier bleu est un E47 : son bleu est volontairement
     * distinct de l'ancien bleu autoroutier utilisé pour les grands fonds.
     */
    public static MotorwaySignColor visualRoadCartoucheColor(MotorwaySignColor color) {
        MotorwaySignColor safeColor = color == null ? MotorwaySignColor.BLUE : color;
        return safeColor == MotorwaySignColor.BLUE || safeColor == MotorwaySignColor.METROPOLITAN_BLUE
                ? MotorwaySignColor.METROPOLITAN_BLUE
                : safeColor;
    }


    private static boolean usesBottomArrow(MotorwaySignGraphic graphic) {
        return graphic == MotorwaySignGraphic.DOWN || graphic == MotorwaySignGraphic.DOWN_DOUBLE;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
