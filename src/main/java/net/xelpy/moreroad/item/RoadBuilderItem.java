package net.xelpy.moreroad.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.xelpy.moreroad.client.RoadBuilderClientHooks;
import net.xelpy.moreroad.road.RoadBuilderGeometry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Constructeur de routes.
 *
 * Utilisation :
 * - clic droit 1 : début de la route ;
 * - clic droit 2 : point de contrôle de la courbe ;
 * - clic droit 3 : fin de la route et ouverture de la prévisualisation ;
 * - Maj + clic droit : annule la sélection en cours.
 *
 * La construction n'est lancée qu'après validation dans l'éditeur aérien.
 *
 * Profil généré :
 * T R R R R R T R R R R R T
 * soit 13 blocs de large :
 * - chaussée en minecraft:gray_concrete ;
 * - lignes de rive + ligne centrale en minecraft:white_concrete.
 *
 * La pente suit la règle du tutoriel : la ligne centrale sert de compteur et
 * la hauteur ne change que d'un bloc tous les 6 blocs parcourus.
 *
 * Après construction, /moreroad undo restaure la dernière route construite
 * par le joueur (historique conservé en mémoire jusqu'à la prochaine route ou
 * au redémarrage du serveur).
 */
public class RoadBuilderItem extends Item {

    private static final int CLEARANCE_HEIGHT = 4;
    private static final int MAX_EMBANKMENT_DEPTH = 10;
    private static final double MAX_POINT_DISTANCE = 512.0D;

    /**
     * Les deux cartes sont séparées volontairement. En solo, client et
     * serveur intégré partagent la même JVM ; une carte unique ferait donc
     * avancer la sélection deux fois à chaque clic.
     */
    private static final ConcurrentMap<UUID, Selection> CLIENT_SELECTIONS =
            new ConcurrentHashMap<>();

    private static final ConcurrentMap<UUID, Selection> SERVER_SELECTIONS =
            new ConcurrentHashMap<>();

    /** Dernière construction annulable, une seule par joueur. */
    private static final ConcurrentMap<UUID, UndoSnapshot> LAST_ROUTES =
            new ConcurrentHashMap<>();

    public RoadBuilderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return handleClientUse(context, player);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        return handleServerUse(context, serverPlayer);
    }

    private static InteractionResult handleClientUse(
            UseOnContext context,
            Player player
    ) {
        UUID playerId = player.getUUID();

        if (player.isShiftKeyDown()) {
            CLIENT_SELECTIONS.remove(playerId);
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos().immutable();
        ResourceKey<Level> dimension = context.getLevel().dimension();

        Selection selection = CLIENT_SELECTIONS.get(playerId);

        if (selection == null || !selection.dimension.equals(dimension)) {
            CLIENT_SELECTIONS.put(
                    playerId,
                    new Selection(dimension, clickedPos, null)
            );
            return InteractionResult.SUCCESS;
        }

        if (selection.control == null) {
            CLIENT_SELECTIONS.put(
                    playerId,
                    new Selection(dimension, selection.start, clickedPos)
            );
            return InteractionResult.SUCCESS;
        }

        CLIENT_SELECTIONS.remove(playerId);

        RoadBuilderClientHooks.openEditor(
                selection.start,
                selection.control,
                clickedPos
        );

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleServerUse(
            UseOnContext context,
            ServerPlayer serverPlayer
    ) {
        UUID playerId = serverPlayer.getUUID();

        if (serverPlayer.isShiftKeyDown()) {
            SERVER_SELECTIONS.remove(playerId);
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : sélection du constructeur de route annulée."
                    ),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos().immutable();
        ResourceKey<Level> dimension = context.getLevel().dimension();

        Selection selection = SERVER_SELECTIONS.get(playerId);

        if (selection == null || !selection.dimension.equals(dimension)) {
            SERVER_SELECTIONS.put(
                    playerId,
                    new Selection(dimension, clickedPos, null)
            );

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : point 1/3 enregistré en "
                                    + formatPos(clickedPos)
                                    + ". Choisis maintenant le point de contrôle du virage."
                    ),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        if (selection.control == null) {
            if (!isDistanceAllowed(selection.start, clickedPos)) {
                serverPlayer.sendSystemMessage(
                        Component.literal(
                                "More Road : ce point est trop éloigné du départ "
                                        + "(maximum 512 blocs)."
                        ),
                        true
                );
                return InteractionResult.SUCCESS;
            }

            SERVER_SELECTIONS.put(
                    playerId,
                    new Selection(dimension, selection.start, clickedPos)
            );

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : point 2/3 enregistré en "
                                    + formatPos(clickedPos)
                                    + ". Choisis maintenant la fin de la route."
                    ),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        if (!isDistanceAllowed(selection.control, clickedPos)) {
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : ce point est trop éloigné du point de contrôle "
                                    + "(maximum 512 blocs)."
                    ),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        SERVER_SELECTIONS.remove(playerId);

        serverPlayer.sendSystemMessage(
                Component.literal(
                        "More Road : point 3/3 enregistré. Ajuste maintenant la courbe "
                                + "dans la prévisualisation puis valide pour construire."
                ),
                true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Appelé uniquement côté serveur par le payload de validation de l'écran.
     */
    public static void buildFromEditor(
            ServerPlayer serverPlayer,
            BlockPos start,
            BlockPos control,
            BlockPos end
    ) {
        if (!isDistanceAllowed(start, control)
                || !isDistanceAllowed(control, end)) {
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : impossible de construire, deux points sont espacés "
                                    + "de plus de 512 blocs."
                    ),
                    false
            );
            return;
        }

        try {
            BuildResult result = buildRoad(
                    serverPlayer.level(),
                    start,
                    control,
                    end
            );

            if (!result.success) {
                serverPlayer.sendSystemMessage(
                        Component.literal("More Road : " + result.message),
                        false
                );
                return;
            }

            LAST_ROUTES.put(
                    serverPlayer.getUUID(),
                    new UndoSnapshot(
                            serverPlayer.level().dimension(),
                            result.originalStates
                    )
            );

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : route créée ("
                                    + RoadBuilderGeometry.ROAD_WIDTH
                                    + " blocs de large, pente max 1/"
                                    + RoadBuilderGeometry.HEIGHT_STEP_DISTANCE
                                    + ", "
                                    + result.surfaceBlocks
                                    + " blocs de chaussée). Utilise /moreroad undo pour l'annuler."
                    ),
                    false
            );
        } catch (Exception exception) {
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : erreur pendant la génération de la route."
                    ),
                    false
            );
        }
    }

    /**
     * Annule la dernière route construite par ce joueur.
     *
     * @return 1 si une route a été restaurée, 0 sinon.
     */
    public static int undoLastRoad(ServerPlayer serverPlayer) {
        UUID playerId = serverPlayer.getUUID();
        UndoSnapshot snapshot = LAST_ROUTES.get(playerId);

        if (snapshot == null) {
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : aucune route récente à annuler."
                    ),
                    false
            );
            return 0;
        }

        Level level = serverPlayer.level();

        if (!level.dimension().equals(snapshot.dimension)) {
            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "More Road : la dernière route a été créée dans une autre dimension. "
                                    + "Retourne dans cette dimension pour l'annuler."
                    ),
                    false
            );
            return 0;
        }

        for (BlockPos pos : snapshot.originalStates.keySet()) {
            if (!level.hasChunkAt(pos)) {
                serverPlayer.sendSystemMessage(
                        Component.literal(
                                "More Road : impossible d'annuler pour le moment : "
                                        + "une partie de la route est dans un chunk non chargé."
                        ),
                        false
                );
                return 0;
            }
        }

        // L'historique n'est supprimé qu'une fois toutes les vérifications
        // terminées, afin qu'une tentative impossible puisse être réessayée.
        LAST_ROUTES.remove(playerId);

        for (Map.Entry<BlockPos, BlockState> entry : snapshot.originalStates.entrySet()) {
            level.setBlock(
                    entry.getKey(),
                    entry.getValue(),
                    Block.UPDATE_ALL
            );
        }

        serverPlayer.sendSystemMessage(
                Component.literal(
                        "More Road : dernière route annulée ("
                                + snapshot.originalStates.size()
                                + " blocs restaurés)."
                ),
                false
        );

        return 1;
    }

    private static BuildResult buildRoad(
            Level level,
            BlockPos start,
            BlockPos control,
            BlockPos end
    ) {
        RoadBuilderGeometry.Geometry geometry =
                RoadBuilderGeometry.calculate(start, control, end);

        Set<BlockPos> surfacePositions = geometry.surfacePositions();
        Set<BlockPos> whiteLinePositions = geometry.whiteLinePositions();

        if (surfacePositions.isEmpty()) {
            return BuildResult.failure(
                    "aucune chaussée n'a pu être calculée."
            );
        }

        for (BlockPos pos : surfacePositions) {
            if (!level.hasChunkAt(pos)) {
                return BuildResult.failure(
                        "une partie de la route traverse un chunk non chargé. "
                                + "Rapproche-toi de la zone puis recommence."
                );
            }

            // L'undo mémorise les BlockState, mais pas les données internes
            // d'une BlockEntity. On refuse donc de remplacer directement un
            // coffre, panneau, machine, etc. afin de ne jamais perdre son NBT.
            if (level.getBlockEntity(pos) != null) {
                return BuildResult.failure(
                        "la chaussée traverse un bloc avec des données (coffre, panneau, etc.). "
                                + "Déplace légèrement la route avant de valider."
                );
            }
        }

        BlockState roadState = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(
                        "minecraft",
                        "gray_concrete"
                )
        ).defaultBlockState();

        BlockState lineState = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(
                        "minecraft",
                        "white_concrete"
                )
        ).defaultBlockState();

        Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();

        for (BlockPos surfacePos : surfacePositions) {
            clearAbove(
                    level,
                    surfacePos,
                    surfacePositions,
                    originalStates
            );
            fillBelow(
                    level,
                    surfacePos,
                    surfacePositions,
                    originalStates
            );
            setBlockRememberingOriginal(
                    level,
                    surfacePos,
                    roadState,
                    originalStates
            );
        }

        for (BlockPos linePos : whiteLinePositions) {
            setBlockRememberingOriginal(
                    level,
                    linePos,
                    lineState,
                    originalStates
            );
        }

        return BuildResult.success(
                surfacePositions.size(),
                Map.copyOf(originalStates)
        );
    }

    private static void clearAbove(
            Level level,
            BlockPos surfacePos,
            Set<BlockPos> surfacePositions,
            Map<BlockPos, BlockState> originalStates
    ) {
        for (
                int yOffset = 1;
                yOffset <= CLEARANCE_HEIGHT;
                yOffset++
        ) {
            BlockPos clearPos = surfacePos.above(yOffset);

            // Une portion de route plus haute peut se trouver au-dessus d'une
            // autre dans une pente. Elle ne doit jamais être effacée par le
            // dégagement de la section basse.
            if (surfacePositions.contains(clearPos)) {
                continue;
            }

            if (level.getBlockEntity(clearPos) != null) {
                continue;
            }

            setBlockRememberingOriginal(
                    level,
                    clearPos,
                    Blocks.AIR.defaultBlockState(),
                    originalStates
            );
        }
    }

    private static void fillBelow(
            Level level,
            BlockPos surfacePos,
            Set<BlockPos> surfacePositions,
            Map<BlockPos, BlockState> originalStates
    ) {
        BlockPos below = surfacePos.below();
        int depth = 0;

        while (
                depth < MAX_EMBANKMENT_DEPTH
                        && !surfacePositions.contains(below)
                        && level.getBlockState(below).isAir()
        ) {
            setBlockRememberingOriginal(
                    level,
                    below,
                    Blocks.DIRT.defaultBlockState(),
                    originalStates
            );

            below = below.below();
            depth++;
        }
    }

    private static void setBlockRememberingOriginal(
            Level level,
            BlockPos pos,
            BlockState newState,
            Map<BlockPos, BlockState> originalStates
    ) {
        BlockState currentState = level.getBlockState(pos);

        if (currentState.equals(newState)) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        originalStates.putIfAbsent(
                immutablePos,
                currentState
        );

        level.setBlock(
                pos,
                newState,
                Block.UPDATE_ALL
        );
    }

    private static boolean isDistanceAllowed(
            BlockPos first,
            BlockPos second
    ) {
        double dx = second.getX() - first.getX();
        double dy = second.getY() - first.getY();
        double dz = second.getZ() - first.getZ();

        return Math.sqrt(dx * dx + dy * dy + dz * dz)
                <= MAX_POINT_DISTANCE;
    }

    private static String formatPos(BlockPos pos) {
        return "X=" + pos.getX()
                + " Y=" + pos.getY()
                + " Z=" + pos.getZ();
    }

    private record Selection(
            ResourceKey<Level> dimension,
            BlockPos start,
            BlockPos control
    ) {
    }

    private record UndoSnapshot(
            ResourceKey<Level> dimension,
            Map<BlockPos, BlockState> originalStates
    ) {
    }

    private record BuildResult(
            boolean success,
            int surfaceBlocks,
            String message,
            Map<BlockPos, BlockState> originalStates
    ) {
        private static BuildResult success(
                int surfaceBlocks,
                Map<BlockPos, BlockState> originalStates
        ) {
            return new BuildResult(
                    true,
                    surfaceBlocks,
                    "",
                    originalStates
            );
        }

        private static BuildResult failure(String message) {
            return new BuildResult(
                    false,
                    0,
                    message,
                    Map.of()
            );
        }
    }
}
