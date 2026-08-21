package net.xelpy.moreroad.client.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.xelpy.moreroad.road.RoadBuilderGeometry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Prévisualisation locale du constructeur de route.
 *
 * Aucun paquet n'est envoyé au serveur ici : les blocs sont remplacés
 * uniquement dans le ClientLevel afin que le joueur voie directement la route
 * dans le vrai terrain pendant qu'il ajuste A / B / C.
 *
 * Couleurs de l'aperçu :
 * - gray concrete : chaussée ;
 * - white concrete : lignes finales.
 *
 * Les petits doublons blancs dans les virages sont maintenant conservés
 * volontairement pour obtenir une courbe visuellement plus lisse.
 */
public final class RoadBuilderWorldPreview {

    private static final int CLEARANCE_HEIGHT = 4;
    private static final int MAX_EMBANKMENT_DEPTH = 10;

    private static final Map<BlockPos, BlockState> ORIGINAL_STATES =
            new LinkedHashMap<>();

    /**
     * Etat visuel que l'aperçu doit imposer localement. Le serveur intégré /
     * distant reste autoritaire et peut renvoyer ses vrais blocs au client.
     * On conserve donc cette carte et on la réapplique périodiquement tant que
     * l'écran de prévisualisation est ouvert.
     */
    private static final Map<BlockPos, BlockState> PREVIEW_STATES =
            new LinkedHashMap<>();

    private RoadBuilderWorldPreview() {
    }

    public static void show(RoadBuilderGeometry.Geometry geometry) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            ORIGINAL_STATES.clear();
            return;
        }

        clear();

        BlockState roadState = getVanillaBlockState("gray_concrete");
        BlockState lineState = getVanillaBlockState("white_concrete");

        Set<BlockPos> surfacePositions = geometry.surfacePositions();
        Set<BlockPos> whiteLinePositions = geometry.whiteLinePositions();

        for (BlockPos surfacePos : surfacePositions) {
            if (!level.hasChunkAt(surfacePos)) {
                continue;
            }

            clearAbove(level, surfacePos);
            fillBelow(level, surfacePos, surfacePositions);
        }

        // On commence toujours par la chaussée complète.
        for (BlockPos surfacePos : surfacePositions) {
            if (!level.hasChunkAt(surfacePos)) {
                continue;
            }

            setPreviewBlock(level, surfacePos, roadState);
        }

        // Les lignes blanches sont posées en dernier afin d'avoir toujours la priorité.
        for (BlockPos linePos : whiteLinePositions) {
            if (!level.hasChunkAt(linePos)) {
                continue;
            }

            setPreviewBlock(level, linePos, lineState);
        }
    }

    /**
     * Réapplique uniquement les blocs d'aperçu qui ont été écrasés par une
     * synchronisation du serveur. C'est indispensable en solo comme en
     * multijoueur : le ClientLevel peut afficher nos blocs locaux pendant une
     * frame puis recevoir l'état réel du chunk et les faire disparaître.
     *
     * Cette méthode est appelée à chaque tick par RoadBuilderPreviewScreen.
     */
    public static void refresh() {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            ORIGINAL_STATES.clear();
            PREVIEW_STATES.clear();
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : PREVIEW_STATES.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState expectedState = entry.getValue();
            BlockState currentState = level.getBlockState(pos);

            if (!currentState.equals(expectedState)) {
                level.setBlock(
                        pos,
                        expectedState,
                        Block.UPDATE_ALL
                );
            }
        }
    }

    public static void clear() {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            ORIGINAL_STATES.clear();
            PREVIEW_STATES.clear();
            return;
        }

        // Désactive d'abord la réapplication automatique avant de restaurer
        // les vrais blocs du monde.
        PREVIEW_STATES.clear();

        for (Map.Entry<BlockPos, BlockState> entry : ORIGINAL_STATES.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            level.setBlock(
                    pos,
                    entry.getValue(),
                    Block.UPDATE_ALL
            );
        }

        ORIGINAL_STATES.clear();
    }

    private static BlockState getVanillaBlockState(String path) {
        return BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", path)
        ).defaultBlockState();
    }

    private static void clearAbove(
            ClientLevel level,
            BlockPos surfacePos
    ) {
        for (int yOffset = 1; yOffset <= CLEARANCE_HEIGHT; yOffset++) {
            BlockPos clearPos = surfacePos.above(yOffset);

            if (!level.hasChunkAt(clearPos)) {
                continue;
            }

            if (level.getBlockEntity(clearPos) != null) {
                continue;
            }

            setPreviewBlock(
                    level,
                    clearPos,
                    Blocks.AIR.defaultBlockState()
            );
        }
    }

    private static void fillBelow(
            ClientLevel level,
            BlockPos surfacePos,
            Set<BlockPos> surfacePositions
    ) {
        BlockPos below = surfacePos.below();
        int depth = 0;

        while (
                depth < MAX_EMBANKMENT_DEPTH
                        && level.hasChunkAt(below)
                        && !surfacePositions.contains(below)
                        && level.getBlockState(below).isAir()
        ) {
            if (level.getBlockEntity(below) != null) {
                break;
            }

            setPreviewBlock(
                    level,
                    below,
                    Blocks.DIRT.defaultBlockState()
            );

            below = below.below();
            depth++;
        }
    }

    private static void setPreviewBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState previewState
    ) {
        if (level.getBlockEntity(pos) != null) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        ORIGINAL_STATES.putIfAbsent(
                immutablePos,
                level.getBlockState(pos)
        );
        PREVIEW_STATES.put(immutablePos, previewState);

        if (!level.getBlockState(pos).equals(previewState)) {
            level.setBlock(
                    pos,
                    previewState,
                    Block.UPDATE_ALL
            );
        }
    }
}
