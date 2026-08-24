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

    /*
     * Hitbox synchronisée avec le dernier modèle Blockbench validé.
     *
     * La plaque NORTH est à Z = 6 -> 7. On garde juste une très légère marge
     * de confort côté sélection, mais sans réavancer artificiellement le
     * volume comme lors de la passe précédente.
     * La largeur/hauteur couvrent toujours la zone maximale des 3 plaques
     * dynamiques M1 -> M12 / TXT.
     */
    private static final VoxelShape POLE_SHAPE =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    private static final VoxelShape NORTH = Shapes.or(
            POLE_SHAPE,
            Block.box(0.5, 0.8, 5.95, 15.5, 15.55, 7.00)
    );

    private static final VoxelShape SOUTH = Shapes.or(
            POLE_SHAPE,
            Block.box(0.5, 0.8, 9.00, 15.5, 15.55, 10.05)
    );

    private static final VoxelShape EAST = Shapes.or(
            POLE_SHAPE,
            Block.box(9.00, 0.8, 0.5, 10.05, 15.55, 15.5)
    );

    private static final VoxelShape WEST = Shapes.or(
            POLE_SHAPE,
            Block.box(5.95, 0.8, 0.5, 7.00, 15.55, 15.5)
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
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
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
