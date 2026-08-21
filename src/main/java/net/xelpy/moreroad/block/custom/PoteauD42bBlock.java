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

/**
 * Double poteau spécifique au grand panneau D42b.
 */
public class PoteauD42bBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<PoteauD42bBlock> CODEC =
            simpleCodec(PoteauD42bBlock::new);

    /*
     * V102 : deux formes distinctes sont utilisées :
     *
     * - SELECTION : une boîte englobante continue qui traverse la cellule du
     *   bloc. Cela rend le double poteau sélectionnable/cassable depuis
     *   n'importe quel côté, même si ses deux montants visuels sont déportés
     *   en dehors de la cellule centrale ;
     * - COLLISION : uniquement les deux montants réels, afin de ne pas créer
     *   un mur invisible entre les poteaux.
     */
    private static final VoxelShape NORTH_SELECTION =
            Block.box(-6.95200, 0.0, 8.00000, 22.95200, 16.0, 10.16000);

    private static final VoxelShape SOUTH_SELECTION =
            Block.box(-6.95200, 0.0, 5.84000, 22.95200, 16.0, 8.00000);

    private static final VoxelShape EAST_SELECTION =
            Block.box(5.84000, 0.0, -6.95200, 8.00000, 16.0, 22.95200);

    private static final VoxelShape WEST_SELECTION =
            Block.box(8.00000, 0.0, -6.95200, 10.16000, 16.0, 22.95200);

    private static final VoxelShape NORTH_COLLISION = Shapes.or(
            Block.box(-6.95200, 0.0, 8.00000, -4.71200, 16.0, 10.16000),
            Block.box(20.71200, 0.0, 8.00000, 22.95200, 16.0, 10.16000)
    );

    private static final VoxelShape SOUTH_COLLISION = Shapes.or(
            Block.box(-6.95200, 0.0, 5.84000, -4.71200, 16.0, 8.00000),
            Block.box(20.71200, 0.0, 5.84000, 22.95200, 16.0, 8.00000)
    );

    private static final VoxelShape EAST_COLLISION = Shapes.or(
            Block.box(5.84000, 0.0, -6.95200, 8.00000, 16.0, -4.71200),
            Block.box(5.84000, 0.0, 20.71200, 8.00000, 16.0, 22.95200)
    );

    private static final VoxelShape WEST_COLLISION = Shapes.or(
            Block.box(8.00000, 0.0, -6.95200, 10.16000, 16.0, -4.71200),
            Block.box(8.00000, 0.0, 20.71200, 10.16000, 16.0, 22.95200)
    );

    public PoteauD42bBlock(Properties properties) {
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
            case SOUTH -> SOUTH_SELECTION;
            case EAST -> EAST_SELECTION;
            case WEST -> WEST_SELECTION;
            default -> NORTH_SELECTION;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_COLLISION;
            case EAST -> EAST_COLLISION;
            case WEST -> WEST_COLLISION;
            default -> NORTH_COLLISION;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());

        if (below.getBlock() instanceof PoteauD42bBlock || below.getBlock() instanceof D42bBlock) {
            facing = below.getValue(FACING);
        } else if (above.getBlock() instanceof PoteauD42bBlock || above.getBlock() instanceof D42bBlock) {
            facing = above.getValue(FACING);
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
