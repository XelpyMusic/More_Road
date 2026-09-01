package net.xelpy.moreroad.block.custom;

/**
 * Polices routières utilisables pour les destinations saisies par le joueur.
 *
 * NORMAL : police Minecraft normale, non italique et respectant la casse.
 * L1 : caractères routiers standards.
 * L4 : caractères routiers italiques, notamment utiles pour certaines
 *      indications locales (gare, lieu urbain, équipement, etc.).
 */
public enum RoadTextFont {

    NORMAL("normal", "Normal", ""),
    L1("l1", "L1 - Standard", "caracteres_l1"),
    L4("l4", "L4 - Italique", "caracteres_l4"),
    L2("L2", "L2 - Standard blanc", "caracteres_l2");

    private final String serializedName;
    private final String displayName;
    private final String resourceName;

    RoadTextFont(
            String serializedName,
            String displayName,
            String resourceName
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.resourceName = resourceName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    /**
     * Cycle historique utilisé par les autres panneaux du mod.
     * NORMAL est volontairement ignoré ici pour ne pas ajouter une troisième
     * police aux éditeurs existants qui doivent rester en L1/L4.
     */
    public RoadTextFont next() {
        return switch (this) {
            case L1,L2 -> L4;
            case L4, NORMAL -> L1;
        };
    }

    public static RoadTextFont fromSerializedName(String value) {
        if (value != null) {
            for (RoadTextFont font : values()) {
                if (font.getSerializedName().equals(value)) {
                    return font;
                }
            }
        }

        return L1;
    }

    /*
     * Aide partagée par tous les panneaux personnalisables : la police n'est
     * pas un choix libre indépendant de la couleur du fond, elle en découle
     * (L1/L4 dessinent un texte sombre prévu pour un fond clair, L2 dessine
     * un vrai texte blanc prévu pour un fond foncé — ce n'est pas juste L1
     * recoloré). Centralisé ici pour que chaque éditeur (D21A, D61A, D/DA
     * autoroutiers...) applique la même règle au lieu de la redéfinir.
     */

    /**
     * Police à utiliser quand le fond redevient clair : L2 (texte blanc)
     * n'a plus de sens dessus, on retombe sur L1. L4 (italique) n'est pas
     * concernée : elle reste utilisable aussi bien sur fond clair que
     * foncé.
     */
    public static RoadTextFont forceForLightBackground(RoadTextFont current) {
        return current == L2 ? L1 : current;
    }

    /**
     * Police à utiliser quand le fond devient foncé (bleu, vert, rouge,
     * noir, marron, bleu métropolitain...) : L1/L4 sont conçues pour un
     * texte sombre, on bascule sur L2.
     */
    public static RoadTextFont forceForDarkBackground(RoadTextFont current) {
        return (current == L1 || current == L4) ? L2 : current;
    }

    /**
     * Police suivante à afficher en cliquant sur le bouton de cycle, en
     * tenant compte du fond : sur un fond foncé, on saute directement à L2
     * au lieu de cycler par L1/L4 (qui ne sont pas prévues pour du texte
     * blanc) ; sinon, cycle normal.
     */
    public static RoadTextFont nextForBackground(RoadTextFont current, boolean darkBackground) {
        if (darkBackground && (current == L1 || current == L4)) {
            return L2;
        }
        return current.next();
    }

    /**
     * Les caractères routiers du mod (L1/L2/L4) sont rendus lettre par
     * lettre quand l'espacement réglementaire doit être respecté. NORMAL
     * reste la police Minecraft standard et n'utilise pas ce traitement.
     */
    public boolean usesRegulatoryLetterSpacing() {
        return this == L1 || this == L2 || this == L4;
    }

    public static boolean usesRegulatoryLetterSpacing(RoadTextFont font) {
        return font != null && font.usesRegulatoryLetterSpacing();
    }

    // L'espacement des lettres ne passe plus par l'insertion d'un caractère
    // (une espace ASCII était trop large, et l'espace fine U+2009 s'affichait
    // en glyphe manquant faute de police de repli) : chaque panneau qui a
    // besoin d'espacer ses caractères positionne desormais chaque lettre
    // individuellement avec un petit espace en pixels. Voir
    // D21ABlockEntityRenderer.submitAnchoredTrackedText (rendu 3D) et
    // SignEditorUi.drawCenteredTrackedPreviewText (apercu 2D).
}
