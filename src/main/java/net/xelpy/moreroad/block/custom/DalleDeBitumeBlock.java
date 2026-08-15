package net.xelpy.moreroad.block.custom;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DalleDeBitumeBlock extends SlabBlock {

    public DalleDeBitumeBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(1f, 10f).noOcclusion());
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }
}