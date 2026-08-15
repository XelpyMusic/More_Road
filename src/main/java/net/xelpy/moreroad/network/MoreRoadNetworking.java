package net.xelpy.moreroad.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;

public final class MoreRoadNetworking {

    private MoreRoadNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                UpdateEB10TextPayload.TYPE,
                UpdateEB10TextPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateEB10Text
        );
    }

    private static void handleUpdateEB10Text(
            UpdateEB10TextPayload payload,
            IPayloadContext context
    ) {
        var player = context.player();
        Level level = player.level();

        BlockPos pos = payload.pos();

        // Le chunk doit toujours être chargé.
        if (!level.hasChunkAt(pos)) {
            return;
        }

        // Évite qu'un client modifie un panneau très loin de lui.
        if (player.blockPosition().distManhattan(pos) > 8) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof EB10BlockEntity blockEntity)) {
            return;
        }

        String text = payload.cityName().strip();

        if (text.length() > 32) {
            text = text.substring(0, 32);
        }

        blockEntity.setCityName(text);

        BlockStateSync.update(level, pos, blockEntity);
    }

    private static final class BlockStateSync {

        private static void update(
                Level level,
                BlockPos pos,
                EB10BlockEntity blockEntity
        ) {
            var state = blockEntity.getBlockState();

            level.sendBlockUpdated(
                    pos,
                    state,
                    state,
                    Block.UPDATE_ALL
            );
        }
    }
}