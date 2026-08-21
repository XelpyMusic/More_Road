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

public class FeuTricoloreBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FeuTricoloreBlock> CODEC = simpleCodec(FeuTricoloreBlock::new);
    /*
     * V102 : le modèle a été recentré d'une unité vers le nouveau centre
     * commun des poteaux. Les quatre hitboxes suivent exactement ce décalage.
     */
    private static final VoxelShape SHAPE_NORTH = Block.box(4.5, 0, 1.5, 11.5, 21, 11.5);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4.5, 0, 4.5, 11.5, 21, 14.5);
    private static final VoxelShape SHAPE_EAST = Block.box(4.5, 0, 4.5, 14.5, 21, 11.5);
    private static final VoxelShape SHAPE_WEST = Block.box(1.5, 0, 4.5, 11.5, 21, 11.5);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public FeuTricoloreBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}