package net.xelpy.moreroad.block.custom;

/**
 * Classement fonctionnel du catalogue autoroutier.
 *
 * Le but est de présenter les modèles par usage dans le GUI plutôt que comme
 * une longue liste de références. Le rendu reste piloté par les presets afin
 * de conserver les géométries particulières quand elles sont nécessaires.
 */
public final class MotorwaySignCatalogInfo {

    public enum Family {
        CUSTOM("Libre", "Construis librement les registres, couleurs, textes, cartouches et symboles"),
        ADVANCED("Signalisation avancée", "Au point de sortie ou de changement de direction"),
        PRESIGNAL("Présignalisation", "Prépare une sortie ou une bifurcation avant le point de décision"),
        CONFIRMATION("Confirmation", "Confirme les destinations et les distances sur l'itinéraire"),
        COMPLEMENT("Complément", "Informations complémentaires, sorties et jalonnement"),
        LANE_ADVANCED("Affectation de voies", "Signalisation avancée suspendue ou affectée aux voies"),
        LANE_PRESIGNAL("Présignalisation sur voies", "Présignalisation suspendue ou affectée aux voies");

        private final String label;
        private final String description;

        Family(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() {
            return this.label;
        }

        public String description() {
            return this.description;
        }
    }

    private MotorwaySignCatalogInfo() {
    }

    public static Family family(MotorwaySignPreset preset) {
        MotorwaySignPreset safe = preset == null ? MotorwaySignPreset.FREEFORM : preset;
        String name = safe.name();
        if (safe == MotorwaySignPreset.FREEFORM) {
            return Family.CUSTOM;
        }
        if (name.startsWith("DA31") || name.startsWith("DA32")) {
            return Family.LANE_ADVANCED;
        }
        if (name.startsWith("DA41") || name.startsWith("DA51") || name.startsWith("DA52")) {
            return Family.LANE_PRESIGNAL;
        }
        if (name.startsWith("D31") || name.startsWith("D32")) {
            return Family.ADVANCED;
        }
        int number = number(safe);
        if (number >= 41 && number <= 52) {
            return Family.PRESIGNAL;
        }
        if (number >= 61 && number <= 64) {
            return Family.CONFIRMATION;
        }
        return Family.COMPLEMENT;
    }

    /**
     * Les modèles ci-dessous gardent un dessin spécifique dans le renderer.
     * Ils restent néanmoins accessibles depuis le même bloc et le même
     * sélecteur : "spécial" décrit l'implémentation, pas un bloc distinct.
     */
    public static boolean usesSpecialArtwork(MotorwaySignPreset preset) {
        return switch (preset) {
            case D44, D62C, D64, D74A, D74B, D63C -> true;
            default -> false;
        };
    }

    public static String usage(MotorwaySignPreset preset) {
        if (preset == null) {
            return "Panneau autoroutier personnalisable";
        }
        return switch (preset) {
            case FREEFORM -> "Construction libre à partir de registres";
            case D31B_EX1, D31B_EX2, D31D, D31E -> "Signalisation avancée de sortie ou de bifurcation";
            case D41A -> "Présignalisation d'une sortie numérotée";
            case D41B -> "Présignalisation d'une sortie non numérotée";
            case D41C -> "Présignalisation d'une bifurcation autoroutière";
            case D44 -> "Présignalisation d'un village étape";
            case D61B -> "Confirmation courante sur autoroute";
            case D63C -> "Confirmation de la prochaine sortie";
            case DA31B -> "Affectation de voies de sortie non numérotée";
            default -> family(preset).description();
        };
    }

    private static int number(MotorwaySignPreset preset) {
        String name = preset.name();
        int index = name.startsWith("DA") ? 2 : 1;
        int value = 0;
        boolean found = false;
        while (index < name.length()) {
            char c = name.charAt(index);
            if (c < '0' || c > '9') {
                break;
            }
            found = true;
            value = value * 10 + (c - '0');
            index++;
        }
        return found ? value : -1;
    }
}
