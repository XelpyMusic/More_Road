package net.xelpy.moreroad.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;

import java.util.function.Supplier;

public class MoreRoadBlockEntities {

    /*
     * ============================================================
     * REGISTRE
     * ============================================================
     */

    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    MoreRoad.MODID
            );


    /*
     * ============================================================
     * EB10 / EB20
     * ============================================================
     */

    public static final Supplier<BlockEntityType<EB10BlockEntity>>
            EB10 =
            BLOCK_ENTITY_TYPES.register(
                    "eb10",
                    () -> new BlockEntityType<>(
                            EB10BlockEntity::new,
                            false,
                            MoreRoadBlocks.EB10.get()
                    )
            );


    /*
     * ============================================================
     * D21A
     * ============================================================
     */

    public static final Supplier<BlockEntityType<D21ABlockEntity>>
            D21A =
            BLOCK_ENTITY_TYPES.register(
                    "d21a",
                    () -> new BlockEntityType<>(
                            D21ABlockEntity::new,
                            false,
                            MoreRoadBlocks.D21A.get(),
                            MoreRoadBlocks.D21A2.get()
                    )
            );

    public static final Supplier<BlockEntityType<D61ABlockEntity>>
            D61A =
            BLOCK_ENTITY_TYPES.register(
                    "d61a",
                    () -> new BlockEntityType<>(
                            D61ABlockEntity::new,
                            false,
                            MoreRoadBlocks.D61A.get(),
                            MoreRoadBlocks.D61A2.get()
                    )
            );

    /*
     * ============================================================
     * D42b
     * ============================================================
     */

    public static final Supplier<BlockEntityType<D42bBlockEntity>>
            D42B =
            BLOCK_ENTITY_TYPES.register(
                    "d42b",
                    () -> new BlockEntityType<>(
                            D42bBlockEntity::new,
                            false,
                            MoreRoadBlocks.D42B.get()
                    )
            );


    /*
     * ============================================================
     * ENREGISTREMENT
     * ============================================================
     */

    public static void register(IEventBus eventBus) {

        BLOCK_ENTITY_TYPES.register(
                eventBus
        );
    }
}