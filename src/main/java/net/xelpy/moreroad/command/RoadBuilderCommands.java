package net.xelpy.moreroad.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.custom.MotorwaySignBlock;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.item.RoadBuilderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Commandes de développement More Road.
 *
 * /moreroad undo              : restaure la dernière route construite.
 * /moreroad panels_test       : pose les 62 panneaux autoroutiers en grille.
 * /moreroad panels_test clear : supprime la dernière grille créée par le joueur.
 */
public final class RoadBuilderCommands {

    private static final int TEST_COLUMNS = 8;
    private static final int TEST_COLUMN_SPACING = 9;
    private static final int TEST_ROW_SPACING = 9;
    private static final int TEST_START_DISTANCE = 8;
    private static final Map<UUID, List<BlockPos>> LAST_PANEL_TEST = new ConcurrentHashMap<>();

    private RoadBuilderCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("moreroad")
                        .then(
                                Commands.literal("undo")
                                        .executes(context -> undo(context.getSource()))
                        )
                        .then(
                                Commands.literal("panels_test")
                                        .executes(context -> createPanelsTest(context.getSource()))
                                        .then(
                                                Commands.literal("clear")
                                                        .executes(context -> clearPanelsTest(context.getSource()))
                                        )
                        )
        );
    }

    private static int undo(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return RoadBuilderItem.undoLastRoad(player);
    }

    /**
     * Pose tout le catalogue à valeurs par défaut. Cette grille sert de banc
     * de contrôle visuel après une correction commune : on peut vérifier une
     * famille entière en quelques secondes au lieu de replacer 62 blocs à la
     * main.
     */
    private static int createPanelsTest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level(); // MC 26.2: ServerPlayer#serverLevel() is unavailable
        clearStoredGrid(level, player.getUUID());

        Direction forward = player.getDirection();
        Direction right = switch (forward) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
        Direction facing = forward.getOpposite();
        BlockPos origin = player.blockPosition().relative(forward, TEST_START_DISTANCE);

        List<BlockPos> positions = new ArrayList<>();
        MotorwaySignPreset[] presets = java.util.Arrays.stream(MotorwaySignPreset.values())
                .filter(preset -> preset != MotorwaySignPreset.FREEFORM)
                .toArray(MotorwaySignPreset[]::new);
        for (int index = 0; index < presets.length; index++) {
            int row = index / TEST_COLUMNS;
            int column = index % TEST_COLUMNS;
            BlockPos pos = origin
                    .relative(right, column * TEST_COLUMN_SPACING)
                    .relative(forward, row * TEST_ROW_SPACING);
            int lift = 0;
            while (!level.getBlockState(pos).isAir() && lift < 32) {
                pos = pos.above();
                lift++;
            }
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            BlockState state = MoreRoadBlocks.MOTORWAY_SIGN.get()
                    .defaultBlockState()
                    .setValue(MotorwaySignBlock.FACING, facing);
            level.setBlock(pos, state, Block.UPDATE_ALL);

            if (level.getBlockEntity(pos) instanceof MotorwaySignBlockEntity blockEntity) {
                MotorwaySignPreset preset = presets[index];
                MotorwaySignLineData[] lines = new MotorwaySignLineData[
                        MotorwaySignBlockEntity.MAX_SLOTS
                ];
                for (int slot = 0; slot < lines.length; slot++) {
                    lines[slot] = slot < preset.getSlotCount()
                            ? MotorwaySignLineData.blankForSlot(preset.getSlot(slot))
                            : MotorwaySignLineData.empty();
                }
                MotorwaySignPanelData[] panels = new MotorwaySignPanelData[
                        MotorwaySignBlockEntity.MAX_CUSTOM_PANELS
                ];
                for (int panel = 0; panel < panels.length; panel++) {
                    panels[panel] = MotorwaySignPanelData.disabled();
                }
                blockEntity.setConfiguration(preset, lines, false, panels);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
            positions.add(pos.immutable());
        }

        LAST_PANEL_TEST.put(player.getUUID(), List.copyOf(positions));
        source.sendSuccess(
                () -> Component.literal(
                        "More Road : " + positions.size()
                                + " panneaux autoroutiers posés pour le contrôle visuel."
                ),
                false
        );
        return positions.size();
    }

    private static int clearPanelsTest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int removed = clearStoredGrid((ServerLevel) player.level(), player.getUUID()); // MC 26.2
        source.sendSuccess(
                () -> Component.literal(
                        "More Road : " + removed + " panneau(x) de test supprimé(s)."
                ),
                false
        );
        return removed;
    }

    private static int clearStoredGrid(ServerLevel level, UUID playerId) {
        List<BlockPos> positions = LAST_PANEL_TEST.remove(playerId);
        if (positions == null || positions.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(MoreRoadBlocks.MOTORWAY_SIGN.get())) {
                level.removeBlock(pos, false);
                removed++;
            }
        }
        return removed;
    }
}
