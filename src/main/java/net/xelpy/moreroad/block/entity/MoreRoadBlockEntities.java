package net.xelpy.moreroad.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;

import java.util.function.Supplier;

public class MoreRoadBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MoreRoad.MODID);

    public static final Supplier<BlockEntityType<EB10BlockEntity>> EB10 =
            BLOCK_ENTITY_TYPES.register("eb10",
                    () -> new BlockEntityType<>(
                            EB10BlockEntity::new,
                            false,
                            MoreRoadBlocks.EB10.get()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}