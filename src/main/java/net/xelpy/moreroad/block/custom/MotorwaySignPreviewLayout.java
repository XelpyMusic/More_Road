package net.xelpy.moreroad.block.custom;

/**
 * Vue simplifiée d'un {@code ExactMappedArtwork} (registres + positions de
 * texte) utilisable par l'éditeur générique pour dessiner un aperçu 2D fidèle
 * à la structure réelle du panneau, au lieu de suppositions génériques
 * (pastille de sortie en haut, distance en bas...) qui ne correspondent pas
 * aux modèles à dessin exact.
 *
 * Type public dans block.custom (plutôt que client.renderer, où vivent les
 * vrais types ExactMappedArtwork/ExactBody/ExactTextPlacement) car
 * MotorwaySignEditScreen (client.screen) et MotorwaySignArtworkCatalog
 * (client.renderer) sont dans des packages différents.
 */
public record MotorwaySignPreviewLayout(
        float sourceWidth,
        float sourceHeight,
        float[] bodyX,
        float[] bodyY,
        float[] bodyWidth,
        float[] bodyHeight,
        int[] textSlotIndex,
        float[] textX,
        float[] textY,
        float[] textWidth,
        float[] textHeight
) {
    public int bodyCount() {
        return this.bodyX.length;
    }

    public int textCount() {
        return this.textSlotIndex.length;
    }
}
