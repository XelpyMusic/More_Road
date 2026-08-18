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

public class J4Block extends HorizontalDirectionalBlock {

    public static final MapCodec<J4Block> CODEC = simpleCodec(J4Block::new);

    /*
     * Hitbox recalée sur le NOUVEAU modèle J4 du src(7).
     *
     * Le modèle contient deux parties distinctes :
     * - la plaque : X 0 -> 16, Y 0 -> 16, Z 6.98333 -> 8.98333 ;
     * - le poteau : X 7 -> 9, Y 0 -> 12, Z 9 -> 10.
     *
     * On conserve deux boîtes séparées pour ne pas englober inutilement
     * le vide situé derrière la plaque.
     */
    private static final VoxelShape PANEL_NORTH =
            Block.box(0.0, 0.0, 6.98333, 16.0, 16.0, 8.98333);

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 9.0, 9.0, 12.0, 10.0);

    private static final VoxelShape PANEL_SOUTH =
            Block.box(0.0, 0.0, 7.01667, 16.0, 16.0, 9.01667);

    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 6.0, 9.0, 12.0, 7.0);

    private static final VoxelShape PANEL_EAST =
            Block.box(7.01667, 0.0, 0.0, 9.01667, 16.0, 16.0);

    private static final VoxelShape POLE_EAST =
            Block.box(6.0, 0.0, 7.0, 7.0, 12.0, 9.0);

    private static final VoxelShape PANEL_WEST =
            Block.box(6.98333, 0.0, 0.0, 8.98333, 16.0, 16.0);

    private static final VoxelShape POLE_WEST =
            Block.box(9.0, 0.0, 7.0, 10.0, 12.0, 9.0);

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

    public J4Block(Properties properties) {
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
