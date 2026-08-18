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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class J4bBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<J4bBlock> CODEC = simpleCodec(J4bBlock::new);

    /*
     * Hitbox recalée sur le NOUVEAU modèle J4b du src(7).
     * J4b possède les mêmes dimensions physiques que J4a.
     *
     * Plaque : X -8 -> 24, Y 0.32441 -> 11, Z 6 -> 8
     * Poteau : X  7 ->  9, Y 0       -> 12, Z 8 -> 10
     */
    private static final VoxelShape PANEL_NORTH =
            Block.box(-8.0, 0.32441, 6.0, 24.0, 11.0, 8.0);

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 8.0, 9.0, 12.0, 10.0);

    private static final VoxelShape PANEL_SOUTH =
            Block.box(-8.0, 0.32441, 8.0, 24.0, 11.0, 10.0);

    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 6.0, 9.0, 12.0, 8.0);

    private static final VoxelShape PANEL_EAST =
            Block.box(8.0, 0.32441, -8.0, 10.0, 11.0, 24.0);

    private static final VoxelShape POLE_EAST =
            Block.box(6.0, 0.0, 7.0, 8.0, 12.0, 9.0);

    private static final VoxelShape PANEL_WEST =
            Block.box(6.0, 0.32441, -8.0, 8.0, 11.0, 24.0);

    private static final VoxelShape POLE_WEST =
            Block.box(8.0, 0.0, 7.0, 10.0, 12.0, 9.0);

    private static final VoxelShape SHAPE_NORTH =
            Shapes.or(PANEL_NORTH, POLE_NORTH);

    private static final VoxelShape SHAPE_SOUTH =
            Shapes.or(PANEL_SOUTH, POLE_SOUTH);

    private static final VoxelShape SHAPE_EAST =
            Shapes.or(PANEL_EAST, POLE_EAST);

    private static final VoxelShape SHAPE_WEST =
            Shapes.or(PANEL_WEST, POLE_WEST);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public J4bBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
