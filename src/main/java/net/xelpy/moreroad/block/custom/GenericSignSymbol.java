package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Pictogramme réglementaire pouvant accompagner une ligne de destination.
 *
 * Volontairement limité aux assets déjà présents dans le mod (logo autoroute
 * et symbole de sortie) : pas de pictogramme "maison" approximatif — un
 * nouveau symbole n'est ajouté ici qu'une fois sa véritable texture fournie.
 */
public enum GenericSignSymbol implements StringRepresentable {
    NONE("none", "Aucun"),
    AUTOROUTE("autoroute", "Autoroute"),
    EXIT("exit", "Sortie");

    private final String serializedName;
    private final String displayName;

    GenericSignSymbol(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public GenericSignSymbol next() {
        return switch (this) {
            case NONE -> AUTOROUTE;
            case AUTOROUTE -> EXIT;
            case EXIT -> NONE;
        };
    }

    public static GenericSignSymbol fromSerializedName(String value) {
        if (value != null) {
            for (GenericSignSymbol symbol : values()) {
                if (symbol.serializedName.equals(value)) {
                    return symbol;
                }
            }
        }
        return NONE;
    }
}
