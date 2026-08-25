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
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.client.MotorwaySignClientHooks;

import java.util.concurrent.ConcurrentHashMap;

/** Point d'ancrage unique du catalogue de panneaux autoroutiers modulables. */
public class MotorwaySignBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<MotorwaySignBlock> CODEC = simpleCodec(MotorwaySignBlock::new);
    private static final ConcurrentHashMap<ShapeKey, VoxelShape[]> SHAPE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ShapeKey, VoxelShape[]> INTERACTION_SHAPE_CACHE = new ConcurrentHashMap<>();

    public MotorwaySignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MotorwaySignGeometry geometry = getGeometry(state, level, pos);
        ShapeKey shapeKey = shapeKey(level, pos, geometry);
        VoxelShape[] shapes = INTERACTION_SHAPE_CACHE.computeIfAbsent(
                shapeKey,
                ignored -> createInteractionShapes(geometry)
        );
        return shapes[directionIndex(state.getValue(FACING))];
    }

    private static VoxelShape getPhysicalShape(BlockState state, BlockGetter level, BlockPos pos) {
        MotorwaySignGeometry geometry = getGeometry(state, level, pos);
        ShapeKey shapeKey = shapeKey(level, pos, geometry);
        VoxelShape[] shapes = SHAPE_CACHE.computeIfAbsent(shapeKey, ignored -> createShapes(geometry));
        return shapes[directionIndex(state.getValue(FACING))];
    }

    private static MotorwaySignGeometry getGeometry(BlockState state, BlockGetter level, BlockPos pos) {
        MotorwaySignBlockEntity blockEntity = level.getBlockEntity(pos) instanceof MotorwaySignBlockEntity sign
                ? sign
                : null;
        MotorwaySignPreset preset = blockEntity != null ? blockEntity.getPreset() : MotorwaySignPreset.D31B_EX1;
        Direction facing = state.getBlock() instanceof MotorwaySignBlock
                ? state.getValue(FACING)
                : Direction.NORTH;
        boolean mountedOnCrossbar = isMountedOnCrossbar(level, pos, facing);
        if (preset == MotorwaySignPreset.D61B) {
            mountedOnCrossbar = false;
        }
        return MotorwaySignGeometry.forComposite(
                preset,
                blockEntity != null ? blockEntity.getLines() : null,
                blockEntity != null ? blockEntity.getCustomPanels() : null,
                mountedOnCrossbar
        );
    }

    /**
     * Le panneau est posé devant la traverse, exactement comme le DA31C : sa
     * face regarde le joueur et son dos touche le segment situé derrière lui.
     */
    public static boolean isMountedOnCrossbar(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos behind = pos.relative(facing.getOpposite());
        return isCrossbar(level, behind);
    }

    private static boolean isCrossbar(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DA31CSupportCrossbarBlock;
    }

    private static ShapeKey shapeKey(BlockGetter level, BlockPos pos, MotorwaySignGeometry geometry) {
        MotorwaySignPreset preset = level.getBlockEntity(pos) instanceof MotorwaySignBlockEntity sign
                ? sign.getPreset()
                : MotorwaySignPreset.D31B_EX1;
        return new ShapeKey(
                preset, geometry.width(), geometry.height(),
                geometry.mountedOnCrossbar(), geometry.supportTop()
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getPhysicalShape(state, level, pos);
    }

    private static VoxelShape[] createShapes(MotorwaySignGeometry geometry) {
        float halfWidth = geometry.width() / 2.0F;
        float panelTop = geometry.mountedOnCrossbar()
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                : geometry.panelBottom() + geometry.height();
        float panelBottom = geometry.mountedOnCrossbar()
                ? panelTop - geometry.height()
                : geometry.panelBottom();
        VoxelShape[] result = new VoxelShape[4];
        result[0] = createNorthSouthShape(halfWidth, panelBottom, panelTop, geometry, false);
        result[1] = createNorthSouthShape(halfWidth, panelBottom, panelTop, geometry, true);
        result[2] = createEastWestShape(halfWidth, panelBottom, panelTop, geometry, false);
        result[3] = createEastWestShape(halfWidth, panelBottom, panelTop, geometry, true);
        return result;
    }

    /**
     * Sélection continue sur toute l'enveloppe du panneau. La collision reste
     * limitée aux plaques et poteaux réels via {@link #getCollisionShape}.
     */
    private static VoxelShape[] createInteractionShapes(MotorwaySignGeometry geometry) {
        float halfWidth = geometry.width() / 2.0F;
        float top = geometry.mountedOnCrossbar()
                ? MotorwaySignGeometry.MOUNTED_PANEL_TOP
                : geometry.panelBottom() + geometry.height();
        float bottom = geometry.mountedOnCrossbar() ? top - geometry.height() : 0.0F;
        VoxelShape[] result = new VoxelShape[4];
        boolean centralGroundSupport = !geometry.mountedOnCrossbar();
        float forward = centralGroundSupport
                ? MotorwaySignGeometry.D61B_PANEL_FORWARD
                : 0.0F;
        float depthHalf = 3.0F / 32.0F;
        result[0] = Block.box(
                (0.5F - halfWidth) * 16.0F, bottom * 16.0F,
                (0.5F - forward - depthHalf) * 16.0F,
                (0.5F + halfWidth) * 16.0F, top * 16.0F,
                (0.5F - forward + depthHalf) * 16.0F
        );
        result[1] = Block.box(
                (0.5F - halfWidth) * 16.0F, bottom * 16.0F,
                (0.5F + forward - depthHalf) * 16.0F,
                (0.5F + halfWidth) * 16.0F, top * 16.0F,
                (0.5F + forward + depthHalf) * 16.0F
        );
        result[2] = Block.box(
                (0.5F + forward - depthHalf) * 16.0F, bottom * 16.0F,
                (0.5F - halfWidth) * 16.0F,
                (0.5F + forward + depthHalf) * 16.0F, top * 16.0F,
                (0.5F + halfWidth) * 16.0F
        );
        result[3] = Block.box(
                (0.5F - forward - depthHalf) * 16.0F, bottom * 16.0F,
                (0.5F - halfWidth) * 16.0F,
                (0.5F - forward + depthHalf) * 16.0F, top * 16.0F,
                (0.5F + halfWidth) * 16.0F
        );
        if (centralGroundSupport) {
            result[0] = Shapes.or(result[0], poleShapesNorthSouth(
                    geometry.width(), geometry.panelBottom(), geometry.supportTop()
            ));
            result[1] = Shapes.or(result[1], poleShapesNorthSouth(
                    geometry.width(), geometry.panelBottom(), geometry.supportTop()
            ));
            result[2] = Shapes.or(result[2], poleShapesEastWest(
                    geometry.width(), geometry.panelBottom(), geometry.supportTop()
            ));
            result[3] = Shapes.or(result[3], poleShapesEastWest(
                    geometry.width(), geometry.panelBottom(), geometry.supportTop()
            ));
        }
        return result;
    }

    private static VoxelShape createNorthSouthShape(
            float halfWidth,
            float bottom,
            float top,
            MotorwaySignGeometry geometry,
            boolean south
    ) {
        float forward = !geometry.mountedOnCrossbar()
                ? (south ? MotorwaySignGeometry.D61B_PANEL_FORWARD
                : -MotorwaySignGeometry.D61B_PANEL_FORWARD)
                : 0.0F;
        float panelDepthHalf = 3.0F / 32.0F;
        VoxelShape panel = Block.box(
                (0.5F - halfWidth) * 16.0F, bottom * 16.0F,
                (0.5F + forward - panelDepthHalf) * 16.0F,
                (0.5F + halfWidth) * 16.0F, top * 16.0F,
                (0.5F + forward + panelDepthHalf) * 16.0F
        );
        return geometry.mountedOnCrossbar() ? panel : Shapes.or(
                panel,
                poleShapesNorthSouth(geometry.width(), bottom, geometry.supportTop())
        );
    }

    private static VoxelShape createEastWestShape(
            float halfWidth,
            float bottom,
            float top,
            MotorwaySignGeometry geometry,
            boolean west
    ) {
        float forward = !geometry.mountedOnCrossbar()
                ? (west ? -MotorwaySignGeometry.D61B_PANEL_FORWARD
                : MotorwaySignGeometry.D61B_PANEL_FORWARD)
                : 0.0F;
        float panelDepthHalf = 3.0F / 32.0F;
        VoxelShape panel = Block.box(
                (0.5F + forward - panelDepthHalf) * 16.0F,
                bottom * 16.0F, (0.5F - halfWidth) * 16.0F,
                (0.5F + forward + panelDepthHalf) * 16.0F,
                top * 16.0F, (0.5F + halfWidth) * 16.0F
        );
        return geometry.mountedOnCrossbar() ? panel : Shapes.or(
                panel,
                poleShapesEastWest(geometry.width(), bottom, geometry.supportTop())
        );
    }

    private static VoxelShape poleShapesNorthSouth(
            float width,
            float panelBottom,
            float configuredSupportTop
    ) {
        float offset = 0.0F;
        float halfSection = 9.0F / 32.0F;
        float depthHalf = halfSection;
        float poleTop = configuredSupportTop;
        VoxelShape first = Block.box(
                (0.5F - offset - halfSection) * 16.0F, 0.0F,
                (0.5F - depthHalf) * 16.0F,
                (0.5F - offset + halfSection) * 16.0F, poleTop * 16.0F,
                (0.5F + depthHalf) * 16.0F
        );
        if (offset == 0.0F) {
            return first;
        }
        return Shapes.or(first, Block.box(
                (0.5F + offset - 0.07F) * 16.0F, 0.0F, 6.5F,
                (0.5F + offset + 0.07F) * 16.0F, (panelBottom + 0.10F) * 16.0F, 9.5F
        ));
    }

    private static VoxelShape poleShapesEastWest(
            float width,
            float panelBottom,
            float configuredSupportTop
    ) {
        float offset = 0.0F;
        float halfSection = 9.0F / 32.0F;
        float depthHalf = halfSection;
        float poleTop = configuredSupportTop;
        VoxelShape first = Block.box(
                (0.5F - depthHalf) * 16.0F, 0.0F,
                (0.5F - offset - halfSection) * 16.0F,
                (0.5F + depthHalf) * 16.0F, poleTop * 16.0F,
                (0.5F - offset + halfSection) * 16.0F
        );
        if (offset == 0.0F) {
            return first;
        }
        return Shapes.or(first, Block.box(
                6.5F, 0.0F, (0.5F + offset - 0.07F) * 16.0F,
                9.5F, (panelBottom + 0.10F) * 16.0F, (0.5F + offset + 0.07F) * 16.0F
        ));
    }

    private static int directionIndex(Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case EAST -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private record ShapeKey(
            MotorwaySignPreset preset,
            float width,
            float height,
            boolean mountedOnCrossbar,
            float supportTop
    ) {
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MotorwaySignBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof MotorwaySignBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            MotorwaySignClientHooks.openEditor(
                    pos,
                    blockEntity.getPreset(),
                    blockEntity.getLines(),
                    blockEntity.isCustomMode(),
                    blockEntity.getCustomPanels()
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
