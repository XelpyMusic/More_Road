package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class E31bBlock extends AbstractE31Block {

    public static final MapCodec<E31bBlock> CODEC = simpleCodec(E31bBlock::new);

    public E31bBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
