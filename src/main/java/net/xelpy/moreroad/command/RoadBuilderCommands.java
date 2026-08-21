package net.xelpy.moreroad.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.xelpy.moreroad.item.RoadBuilderItem;

/**
 * Commandes du constructeur de route.
 *
 * /moreroad undo : restaure la dernière route construite par le joueur.
 */
public final class RoadBuilderCommands {

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
        );
    }

    private static int undo(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return RoadBuilderItem.undoLastRoad(player);
    }
}
