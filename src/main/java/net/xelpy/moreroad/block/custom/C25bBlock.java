package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class C25bBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<C25bBlock> CODEC = simpleCodec(C25bBlock::new);

    /*
     * Hitbox recalée sur le nouveau modèle Blockbench C25b :
     * X : -2.80 -> 18.80
     * Y :  0.00 -> 31.48875
     * Z :  6.37750 -> 10.00
     *
     * Les variantes EAST / SOUTH / WEST correspondent à la rotation Y
     * appliquée par le blockstate autour du centre du bloc.
     */
    private static final VoxelShape SHAPE_NORTH =
            Block.box(-2.8, 0.0, 5.3775, 18.8, 31.48875, 9.0);

    private static final VoxelShape SHAPE_EAST =
            Block.box(7.3775, 0.0, -2.8, 11.0, 31.48875, 18.8);

    private static final VoxelShape SHAPE_SOUTH =
            Block.box(-2.8, 0.0, 7.0, 18.8, 31.48875, 10.6225);

    private static final VoxelShape SHAPE_WEST =
            Block.box(5.0, 0.0, -2.8, 8.6225, 31.48875, 18.8);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public C25bBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos,
            CollisionContext pContext
    ) {
        return switch (pState.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        pContext.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> pBuilder
    ) {
        pBuilder.add(FACING);
    }
}
