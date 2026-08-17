package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;
import net.xelpy.moreroad.client.EB10ClientHooks;

public class EB10Block extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<EB10Block> CODEC = simpleCodec(EB10Block::new);

    /*
     * false = EB10
     * true  = EB20
     */
    public static final BooleanProperty EB20 = BooleanProperty.create("eb20");

    private static final VoxelShape SHAPE_NORTH =
            Block.box(0, 0, 7, 16, 16, 10);

    private static final VoxelShape SHAPE_SOUTH =
            Block.box(0, 0, 6, 16, 16, 9);

    private static final VoxelShape SHAPE_EAST =
            Block.box(6, 0, 0, 9, 16, 16);

    private static final VoxelShape SHAPE_WEST =
            Block.box(7, 0, 0, 10, 16, 16);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public EB10Block(Properties properties) {
        super(properties);

        /*
         * Quand on pose le panneau :
         *
         * facing = nord par défaut
         * eb20   = false
         *
         * Donc EB10 est toujours le modèle initial.
         */
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(EB20, false)
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
                )
                .setValue(EB20, false);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, EB20);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new EB10BlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof EB10BlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            EB10ClientHooks.openEditor(
                    pos,
                    blockEntity.getLine1(),
                    blockEntity.getLine2(),
                    state.getValue(EB20)
            );
        }

        return InteractionResult.SUCCESS;
    }
}