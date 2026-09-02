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
        boolean allowsExtraPanels,
        /*
         * Onglet "Symbole" (choix d'une flèche/pictogramme) par registre :
         * inutile sur D31b (ex.1/ex.2), dont la seule flèche du panneau est
         * fixe et déjà affichée dans le registre principal — les registres
         * ajoutés (villes) n'ont jamais leur propre symbole sur le vrai
         * panneau.
         */
        boolean allowsCustomGraphic,
        /*
         * Choix de couleur par champ (bouton "Blanc"/"Vert"/"Bleu"...) dans
         * l'éditeur générique : inutile sur D44, qui reste toujours blanc
         * sur le vrai panneau (présignalisation de village étape).
         */
        boolean allowsPerFieldColor
) {

    private static final float DEFAULT_TEXT_SCALE = 0.044F;

    public static MotorwaySignStyleProfile forPreset(MotorwaySignPreset preset) {
        MotorwaySignPreset safePreset = preset == null ? MotorwaySignPreset.FREEFORM : preset;

        float textScale = preferredAddedTextScale(safePreset);
        float lineStep = preferredAddedLineStep(safePreset, textScale);
        float leftMargin = switch (safePreset) {
            /*
             * Signalé trop collé au bord gauche sur l'ex.2 (villes) comparé
             * à l'ex.1, qui a la même marge que le vrai panneau visé.
             */
            case D31B_EX1, D31B_EX2, D31D, D31E, D41A, D41B, D41C -> 0.13F;
            case D63C -> 0.34F;
            default -> 0.32F;
        };
        float rightMargin = safePreset == MotorwaySignPreset.D63C ? 0.38F : 0.28F;
        float distanceGap = safePreset == MotorwaySignPreset.D63C ? 0.34F : 0.30F;
        float opticalYOffset = switch (safePreset) {
            case D63C -> -0.045F;
            /*
             * Signalé trop haut / pas assez centré verticalement dans ses
             * registres (TOULOUSE/MONTAUBAN au-dessus, TULLE/BRIVE en
             * dessous) : décalage vers le bas un peu plus marqué que la
             * valeur par défaut.
             */
            case D31B_EX1, D31B_EX2 -> -0.075F;
            /* D41b : les caractères ressortaient encore trop haut dans les registres ajoutés. */
            case D41B, D41C -> -0.095F;
            default -> -0.055F;
        };

        return new MotorwaySignStyleProfile(
                textScale,
                lineStep,
                leftMargin,
                rightMargin,
                distanceGap,
                opticalYOffset,
                safePreset != MotorwaySignPreset.D31B_EX1
                        && safePreset != MotorwaySignPreset.D31B_EX2
                        && safePreset != MotorwaySignPreset.D31D
                        && safePreset != MotorwaySignPreset.D31E
                        && safePreset != MotorwaySignPreset.D41A
                        && safePreset != MotorwaySignPreset.D41B
                        && safePreset != MotorwaySignPreset.D41C,
                /*
                 * Signalé : le D31e porte déjà sa propre cartouche de
                 * numéro de route (registre du haut, dessin d'origine),
                 * comme D31b (ex.1/ex.2) exclus ci-dessus pour la même
                 * raison — inutile d'en exposer une deuxième sur "Panneau
                 * principal".
                 */
                safePreset != MotorwaySignPreset.D31B_EX1
                        && safePreset != MotorwaySignPreset.D31B_EX2
                        && safePreset != MotorwaySignPreset.D31E
                        && safePreset != MotorwaySignPreset.D32A
                        && safePreset != MotorwaySignPreset.D46A
                        && safePreset != MotorwaySignPreset.D47A
                        && safePreset != MotorwaySignPreset.D41A
                        && safePreset != MotorwaySignPreset.D41B
                        && safePreset != MotorwaySignPreset.D41C,
                safePreset == MotorwaySignPreset.D61B,
                false,
                safePreset != MotorwaySignPreset.D44
                        && safePreset != MotorwaySignPreset.D32A
                        && safePreset != MotorwaySignPreset.D46A
                        && safePreset != MotorwaySignPreset.D47A,
                /*
                 * Signalé : "Symbole" (choix d'une flèche/pictogramme) par
                 * registre ne sert à rien sur le D31d ni le D31e — leur
                 * seule flèche est fixe et déjà affichée sur le panneau
                 * principal, comme sur D31b (ex.1/ex.2) déjà exclus
                 * ci-dessus.
                 */
                safePreset != MotorwaySignPreset.D31B_EX1
                        && safePreset != MotorwaySignPreset.D31B_EX2
                        && safePreset != MotorwaySignPreset.D31D
                        && safePreset != MotorwaySignPreset.D31E
                        && safePreset != MotorwaySignPreset.D32A
                        && safePreset != MotorwaySignPreset.D46A
                        && safePreset != MotorwaySignPreset.D47A
                        && safePreset != MotorwaySignPreset.D41A
                        && safePreset != MotorwaySignPreset.D41B
                        && safePreset != MotorwaySignPreset.D41C,
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
            case D32A -> 0.048F;
            case D46A, D47A -> 0.054F;
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
        /*
         * Signalé trop serré verticalement entre les villes dès 3 lignes
         * remplies sur l'ex.2 : son texte (0.063) est nettement plus grand
         * que la référence (0.044), mais le plafond générique de 0.58
         * empêchait le pas de ligne de suivre cette taille proportionnellement,
         * ce qui resserrait les lignes par rapport à leur propre hauteur de
         * glyphe. L'ex.1 (0.044) ne touchait jamais ce plafond, d'où l'écart
         * de rendu entre les deux malgré la même formule.
         */
        if (preset == MotorwaySignPreset.D31B_EX1 || preset == MotorwaySignPreset.D31B_EX2
                || preset == MotorwaySignPreset.D31D || preset == MotorwaySignPreset.D31E
                || preset == MotorwaySignPreset.D41A || preset == MotorwaySignPreset.D41B
                || preset == MotorwaySignPreset.D41C) {
            /*
             * Encore un peu juste même après avoir suivi la proportion du
             * texte (ci-dessus) : léger supplément au-delà du strict
             * proportionnel pour aérer un peu plus les 3 lignes.
             *
             * Signalé sur le D31b ex.1 (registres ajoutés, notamment le
             * panneau bleu à 3 villes) : même symptôme de lignes trop
             * serrées. On lui applique donc le même supplément de 15 % que
             * les autres panneaux multi-lignes afin d'harmoniser l'espacement.
             * D31d/D31e conservent ce même correctif pour leurs registres
             * empilés.
             */
            return proportional * 1.15F;
        }
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
