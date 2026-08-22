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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;
import net.xelpy.moreroad.client.PanonceauClientHooks;

/**
 * Support générique pour les panonceaux M.
 *
 * Un seul bloc peut afficher jusqu'à trois panonceaux, ce qui évite d'espacer
 * artificiellement les plaques d'un bloc Minecraft entier.
 */
public class PanonceauBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<PanonceauBlock> CODEC = simpleCodec(PanonceauBlock::new);

    private static final VoxelShape NORTH_SOUTH = Shapes.or(
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0),
            Block.box(0.5, 1.0, 5.5, 15.5, 15.8, 10.5)
    );

    private static final VoxelShape EAST_WEST = Shapes.or(
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0),
            Block.box(5.5, 1.0, 0.5, 10.5, 15.8, 15.5)
    );

    public PanonceauBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, Direction.NORTH)
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
            case EAST, WEST -> EAST_WEST;
            default -> NORTH_SOUTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        if (above.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = above.getValue(HorizontalDirectionalBlock.FACING);
        } else if (below.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = below.getValue(HorizontalDirectionalBlock.FACING);
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PanonceauBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof PanonceauBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            PanonceauClientHooks.openEditor(pos, blockEntity.getEntries());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
