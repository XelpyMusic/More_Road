package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.PlaqueRueBlockEntity;
import net.xelpy.moreroad.client.PlaqueRueClientHooks;

/**
 * Plaque de rue personnalisable.
 *
 * Le même BlockItem gère les deux poses :
 * - clic sur le dessus d'un bloc / poteau_block : plaque sur support ;
 * - clic sur une face verticale : plaque murale, collée à la face choisie.
 */
public class PlaqueRueBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<PlaqueRueBlock> CODEC = simpleCodec(PlaqueRueBlock::new);
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    /* Modèle sur poteau : géométrie conservée du modèle utilisateur. */
    private static final VoxelShape STANDING_NORTH = Shapes.or(
            Block.box(2.0, 6.0, 7.0, 14.0, 12.0, 9.0),
            Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0)
    );
    private static final VoxelShape STANDING_EAST = Shapes.or(
            Block.box(7.0, 6.0, 2.0, 9.0, 12.0, 14.0),
            Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0)
    );
    private static final VoxelShape STANDING_SOUTH = STANDING_NORTH;
    private static final VoxelShape STANDING_WEST = STANDING_EAST;

    /*
     * Modèle mural : la plaque touche réellement la limite du bloc située
     * contre le mur. Il n'y a donc plus les ~3/4 de bloc de vide du premier
     * modèle mural.
     */
    private static final VoxelShape WALL_NORTH =
            Block.box(2.0, 5.0, 14.0, 14.0, 11.0, 16.0);
    private static final VoxelShape WALL_EAST =
            Block.box(0.0, 5.0, 2.0, 2.0, 11.0, 14.0);
    private static final VoxelShape WALL_SOUTH =
            Block.box(2.0, 5.0, 0.0, 14.0, 11.0, 2.0);
    private static final VoxelShape WALL_WEST =
            Block.box(14.0, 5.0, 2.0, 16.0, 11.0, 14.0);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public PlaqueRueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return null;
        }

        if (clickedFace.getAxis().isHorizontal()) {
            return this.defaultBlockState()
                    .setValue(FACING, clickedFace)
                    .setValue(FACE, AttachFace.WALL);
        }

        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FACE, AttachFace.FLOOR);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (state.getValue(FACE) == AttachFace.FLOOR) {
            return switch (state.getValue(FACING)) {
                case EAST -> STANDING_EAST;
                case SOUTH -> STANDING_SOUTH;
                case WEST -> STANDING_WEST;
                default -> STANDING_NORTH;
            };
        }

        return switch (state.getValue(FACING)) {
            case EAST -> WALL_EAST;
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            default -> WALL_NORTH;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(FACE) == AttachFace.FLOOR) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            return belowState.isFaceSturdy(level, belowPos, Direction.UP)
                    || belowState.getBlock() instanceof PoteauBlock;
        }

        Direction supportDirection = state.getValue(FACING).getOpposite();
        BlockPos supportPos = pos.relative(supportDirection);
        return level.getBlockState(supportPos)
                .isFaceSturdy(level, supportPos, state.getValue(FACING));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            Orientation orientation,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlaqueRueBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof PlaqueRueBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            PlaqueRueClientHooks.openEditor(
                    pos,
                    blockEntity.getLine1(),
                    blockEntity.getLine2(),
                    blockEntity.getLine1Font(),
                    blockEntity.getLine2Font()
            );
        }

        return InteractionResult.SUCCESS;
    }
}
