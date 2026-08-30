package net.xelpy.moreroad.block.custom;

/**
 * Panonceaux CE affichables sous un panneau D44 (village étape).
 *
 * La réglementation autorise 3 ou 6 panonceaux CE sous un D44 ; ce mod laisse
 * le choix libre de chaque emplacement (jusqu'à {@link #MAX_SLOTS}), avec
 * {@link #NONE} pour masquer un emplacement. Les 3 premiers valent la même
 * réglementation que le D45 : voir MotorwaySignCatalogInfo.
 */
public enum MotorwaySignServiceIcon {

    NONE("none", "Aucun", null),
    INFO("info", "Information", "ce3a.png"),
    RESTAURANT("restaurant", "Restauration", "ce16.png"),
    LODGING("lodging", "Hébergement", "ce17.png"),
    PLAYGROUND("playground", "Aire de jeux", "ce23.png"),
    PAYMENT("payment", "Paiement par carte", "ce25.png"),
    CAMPER_VAN("camper_van", "Aire de service camping-car", "ce24.png"),
    FUEL("fuel", "Carburant", "ce15a.png"),
    TIRE_PRESSURE("tire_pressure", "Gonflage des pneus", "ce26.png");

    /** Nombre d'emplacements gérés par l'éditeur et le renderer (2 rangées de 3). */
    public static final int MAX_SLOTS = 6;

    private final String serializedName;
    private final String displayName;
    private final String textureFile;

    MotorwaySignServiceIcon(String serializedName, String displayName, String textureFile) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.textureFile = textureFile;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /** Nom de fichier sous textures/block/, ou null pour {@link #NONE}. */
    public String getTextureFile() {
        return this.textureFile;
    }

    public boolean isVisible() {
        return this != NONE;
    }

    public MotorwaySignServiceIcon next() {
        MotorwaySignServiceIcon[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static MotorwaySignServiceIcon fromSerializedName(String value) {
        if (value != null) {
            for (MotorwaySignServiceIcon icon : values()) {
                if (icon.serializedName.equals(value)) {
                    return icon;
                }
            }
        }
        return NONE;
    }

    /** Les 3 premiers emplacements par défaut d'un D44 fraîchement posé. */
    public static MotorwaySignServiceIcon[] defaults() {
        return new MotorwaySignServiceIcon[]{
                INFO, RESTAURANT, LODGING, NONE, NONE, NONE
        };
    }
}
