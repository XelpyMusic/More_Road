package net.xelpy.moreroad.block.custom;

/**
 * Une ligne de panonceau dans le support générique.
 */
public record PanonceauEntry(
        boolean enabled,
        PanonceauVariant variant,
        String value
) {
    public PanonceauEntry {
        variant = variant == null ? PanonceauVariant.M1 : variant;
        value = value == null ? "" : value;
    }

    public static PanonceauEntry defaultFirst() {
        return new PanonceauEntry(true, PanonceauVariant.M1, PanonceauVariant.M1.defaultValue());
    }

    public static PanonceauEntry disabled() {
        return new PanonceauEntry(false, PanonceauVariant.M1, PanonceauVariant.M1.defaultValue());
    }
}
