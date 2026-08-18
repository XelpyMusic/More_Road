package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Bloc interne sans item, utilisé uniquement comme porte-modèle par le BER D61A.
 * Il permet de résoudre les variantes blanc / vert des panneaux D61A.
 */
public class D61APanelModelBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<D61APanelModelBlock> CODEC =
            simpleCodec(D61APanelModelBlock::new);

    public static final EnumProperty<D21AType> TYPE =
            EnumProperty.create("type", D21AType.class);

    public D61APanelModelBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(TYPE, D21AType.WHITE)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                TYPE
        );
    }
}
