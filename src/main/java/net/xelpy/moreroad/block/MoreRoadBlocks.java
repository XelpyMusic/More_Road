// Dans le fichier /block/MoreRoadBlocks.java
package net.xelpy.moreroad.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.*;
import net.xelpy.moreroad.item.MoreRoadItems;

import java.util.function.Function;
import java.util.function.Supplier;

public class MoreRoadBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MoreRoad.MODID);

    public static final DeferredBlock<Block> A1A = registerBlock("a1a",
            (properties) -> new A1aBlock(properties.noOcclusion().requiresCorrectToolForDrops().sound(SoundType.IRON)));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        MoreRoadItems.ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }
}