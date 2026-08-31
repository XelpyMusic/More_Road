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
import net.xelpy.moreroad.block.entity.GenericDirectionalSignBlockEntity;
import net.xelpy.moreroad.client.GenericDirectionalSignClientHooks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Panneau directionnel modulable générique : un seul bloc, posé vierge,
 * dont le contenu (destinations, cartouches, flèches, symboles) est composé
 * intégralement dans l'éditeur — voir {@link GenericDirectionalSignData}.
 */
public class GenericDirectionalSignBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<GenericDirectionalSignBlock> CODEC = simpleCodec(GenericDirectionalSignBlock::new);
    private static final ConcurrentHashMap<GenericDirectionalSignGeometry, VoxelShape[]> SHAPE_CACHE =
            new ConcurrentHashMap<>();
    private static final float POST_HALF_WIDTH = 3.0F / 32.0F;
    private static final float PANEL_HALF_DEPTH = 3.0F / 32.0F;

    public GenericDirectionalSignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private static GenericDirectionalSignGeometry getGeometry(BlockGetter level, BlockPos pos) {
        GenericDirectionalSignData data = level.getBlockEntity(pos) instanceof GenericDirectionalSignBlockEntity sign
                ? sign.getData()
                : GenericDirectionalSignData.blank();
        return GenericDirectionalSignGeometry.forData(data);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        GenericDirectionalSignGeometry geometry = getGeometry(level, pos);
        VoxelShape[] shapes = SHAPE_CACHE.computeIfAbsent(geometry, GenericDirectionalSignBlock::createShapes);
        return shapes[directionIndex(state.getValue(FACING))];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape[] createShapes(GenericDirectionalSignGeometry geometry) {
        float halfWidth = geometry.width() / 2.0F;
        float panelBottom = geometry.panelBottom();
        float panelTop = panelBottom + geometry.height();
        VoxelShape[] result = new VoxelShape[4];
        result[0] = northSouthShape(halfWidth, panelBottom, panelTop, geometry.supportTop());
        result[1] = result[0];
        result[2] = eastWestShape(halfWidth, panelBottom, panelTop, geometry.supportTop());
        result[3] = result[2];
        return result;
    }

    private static VoxelShape northSouthShape(float halfWidth, float panelBottom, float panelTop, float supportTop) {
        VoxelShape panel = Block.box(
                (0.5F - halfWidth) * 16.0F, panelBottom * 16.0F, (0.5F - PANEL_HALF_DEPTH) * 16.0F,
                (0.5F + halfWidth) * 16.0F, panelTop * 16.0F, (0.5F + PANEL_HALF_DEPTH) * 16.0F
        );
        VoxelShape post = Block.box(
                (0.5F - POST_HALF_WIDTH) * 16.0F, 0.0F, (0.5F - POST_HALF_WIDTH) * 16.0F,
                (0.5F + POST_HALF_WIDTH) * 16.0F, supportTop * 16.0F, (0.5F + POST_HALF_WIDTH) * 16.0F
        );
        return Shapes.or(panel, post);
    }

    private static VoxelShape eastWestShape(float halfWidth, float panelBottom, float panelTop, float supportTop) {
        VoxelShape panel = Block.box(
                (0.5F - PANEL_HALF_DEPTH) * 16.0F, panelBottom * 16.0F, (0.5F - halfWidth) * 16.0F,
                (0.5F + PANEL_HALF_DEPTH) * 16.0F, panelTop * 16.0F, (0.5F + halfWidth) * 16.0F
        );
        VoxelShape post = Block.box(
                (0.5F - POST_HALF_WIDTH) * 16.0F, 0.0F, (0.5F - POST_HALF_WIDTH) * 16.0F,
                (0.5F + POST_HALF_WIDTH) * 16.0F, supportTop * 16.0F, (0.5F + POST_HALF_WIDTH) * 16.0F
        );
        return Shapes.or(panel, post);
    }

    private static int directionIndex(Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case EAST -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GenericDirectionalSignBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof GenericDirectionalSignBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            GenericDirectionalSignClientHooks.openEditor(pos, blockEntity.getData());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
