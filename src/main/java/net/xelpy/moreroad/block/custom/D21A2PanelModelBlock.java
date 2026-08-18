package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class D21A2PanelModelBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<D21A2PanelModelBlock> CODEC =
            simpleCodec(D21A2PanelModelBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public D21A2PanelModelBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(D21APanelModelBlock.TYPE, D21AType.WHITE)
                        .setValue(D21APanelModelBlock.ARROW_RIGHT, false)
                        .setValue(D21APanelModelBlock.AUTOROUTE_LOGO, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                D21APanelModelBlock.TYPE,
                D21APanelModelBlock.ARROW_RIGHT,
                D21APanelModelBlock.AUTOROUTE_LOGO
        );
    }
}
