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
    L4("l4", "L4 - Italique", "caracteres_l4");

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
            case L1 -> L4;
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
}
