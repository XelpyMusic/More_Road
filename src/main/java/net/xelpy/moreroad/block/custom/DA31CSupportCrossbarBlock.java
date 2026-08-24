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

/** Segment modulaire de traverse de portique, prévu pour recevoir un DA31C. */
public class DA31CSupportCrossbarBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<DA31CSupportCrossbarBlock> CODEC = simpleCodec(DA31CSupportCrossbarBlock::new);

    /*
     * V7 : la géométrie visuelle avance vers le panneau afin de réduire
     * fortement le vide entre la plaque et le portique, mais la hitbox reste
     * strictement contenue dans le bloc de traverse. Cela conserve la pose
     * modulaire de plusieurs segments côte à côte sans blocage.
     */
    private static final VoxelShape NORTH_SOUTH = Block.box(0.0D, 0.0D, 4.5D, 16.0D, 14.0D, 11.5D);
    private static final VoxelShape EAST_WEST = Block.box(4.5D, 0.0D, 0.0D, 11.5D, 14.0D, 16.0D);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DA31CSupportCrossbarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing == Direction.EAST || facing == Direction.WEST ? EAST_WEST : NORTH_SOUTH;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
