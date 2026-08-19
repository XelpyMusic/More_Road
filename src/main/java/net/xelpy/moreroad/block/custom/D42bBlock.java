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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;
import net.xelpy.moreroad.client.D42bClientHooks;

/**
 * D42b - présignalisation diagrammatique de giratoire.
 *
 * Le modèle JSON ne contient que la plaque et ses deux poteaux supérieurs.
 * Le diagramme, les flèches, les encarts et les textes sont rendus
 * dynamiquement par la BlockEntityRenderer.
 */
public class D42bBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<D42bBlock> CODEC =
            simpleCodec(D42bBlock::new);

    /* Bornes V77 : panneau agrandi, sans dépasser la plage valide des modèles. */
    private static final VoxelShape NORTH =
            Block.box(-12.60800, -0.92452, 5.84000, 28.60800, 31.90000, 10.16000);

    private static final VoxelShape SOUTH =
            Block.box(-12.60800, -0.92452, 5.84000, 28.60800, 31.90000, 10.16000);

    private static final VoxelShape EAST =
            Block.box(5.84000, -0.92452, -12.60800, 10.16000, 31.90000, 28.60800);

    private static final VoxelShape WEST =
            Block.box(5.84000, -0.92452, -12.60800, 10.16000, 31.90000, 28.60800);

    public D42bBlock(Properties properties) {
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
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            default -> NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());

        if (below.getBlock() instanceof PoteauD42bBlock) {
            facing = below.getValue(FACING);
        } else if (above.getBlock() instanceof PoteauD42bBlock) {
            facing = above.getValue(FACING);
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new D42bBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof D42bBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            D42bClientHooks.openEditor(
                    pos,
                    blockEntity.getBranches(),
                    blockEntity.getDistanceText()
            );
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
