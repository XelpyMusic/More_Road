package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Bloc interne sans BlockItem. Il sert uniquement à résoudre le modèle de
 * flèche noire ou blanche dans le renderer D61A.
 */
public class D61AArrowModelBlock extends Block {

    public static final MapCodec<D61AArrowModelBlock> CODEC =
            simpleCodec(D61AArrowModelBlock::new);

    public static final BooleanProperty BLACK =
            BooleanProperty.create("black");

    public D61AArrowModelBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(BLACK, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(BLACK);
    }
}
