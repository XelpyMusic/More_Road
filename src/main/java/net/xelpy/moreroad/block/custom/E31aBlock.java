package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class E31aBlock extends AbstractE31Block {

    public static final MapCodec<E31aBlock> CODEC = simpleCodec(E31aBlock::new);

    public E31aBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
