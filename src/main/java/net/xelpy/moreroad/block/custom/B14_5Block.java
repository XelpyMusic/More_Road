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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.client.B14ClientHooks;

/**
 * B14 unifié.
 *
 * L'ID historique "b14_5" est volontairement conservé afin d'éviter de
 * casser les mondes existants et l'enregistrement du bloc. La propriété
 * SPEED choisit ensuite le modèle B14 réellement affiché.
 */
public class B14_5Block extends HorizontalDirectionalBlock {

    public static final MapCodec<B14_5Block> CODEC =
            simpleCodec(B14_5Block::new);

    public static final EnumProperty<B14Speed> SPEED =
            EnumProperty.create("speed", B14Speed.class);

    private static final VoxelShape SHAPE_NORTH =
            Block.box(0, 0, 7, 16, 16, 10);

    private static final VoxelShape SHAPE_SOUTH =
            Block.box(0, 0, 6, 16, 16, 9);

    private static final VoxelShape SHAPE_EAST =
            Block.box(6, 0, 0, 9, 16, 16);

    private static final VoxelShape SHAPE_WEST =
            Block.box(7, 0, 0, 10, 16, 16);

    public B14_5Block(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(SPEED, B14Speed.KMH_5)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            B14ClientHooks.openEditor(
                    pos,
                    state.getValue(SPEED)
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                SPEED
        );
    }
}
