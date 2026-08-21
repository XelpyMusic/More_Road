package net.xelpy.moreroad.block.custom;

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
import net.xelpy.moreroad.block.entity.E31BlockEntity;
import net.xelpy.moreroad.client.E31ClientHooks;

/**
 * Base commune aux panneaux E31a et E31b.
 *
 * Les deux panneaux utilisent un seul texte personnalisable rendu en
 * caractères L4. Le modèle possède déjà son poteau.
 */
public abstract class AbstractE31Block extends HorizontalDirectionalBlock
        implements EntityBlock {

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape POLE_EAST =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape POLE_WEST =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    private static final VoxelShape E31A_PANEL_NORTH =
            Block.box(-4.0, 10.84, 5.0, 20.0, 15.0, 7.0);
    private static final VoxelShape E31A_PANEL_SOUTH =
            Block.box(-4.0, 10.84, 9.0, 20.0, 15.0, 11.0);
    private static final VoxelShape E31A_PANEL_EAST =
            Block.box(9.0, 10.84, -4.0, 11.0, 15.0, 20.0);
    private static final VoxelShape E31A_PANEL_WEST =
            Block.box(5.0, 10.84, -4.0, 7.0, 15.0, 20.0);

    private static final VoxelShape E31B_PANEL_NORTH =
            Block.box(-4.0, 11.63, 5.5, 20.0, 15.0, 7.0);
    private static final VoxelShape E31B_PANEL_SOUTH =
            Block.box(-4.0, 11.63, 9.0, 20.0, 15.0, 10.5);
    private static final VoxelShape E31B_PANEL_EAST =
            Block.box(9.0, 11.63, -4.0, 10.5, 15.0, 20.0);
    private static final VoxelShape E31B_PANEL_WEST =
            Block.box(5.5, 11.63, -4.0, 7.0, 15.0, 20.0);

    private final boolean waterName;

    protected AbstractE31Block(Properties properties, boolean waterName) {
        super(properties);
        this.waterName = waterName;

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    public boolean isWaterName() {
        return this.waterName;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction facing = state.getValue(FACING);
        VoxelShape panel = getPanelShape(facing);
        VoxelShape pole = switch (facing) {
            case SOUTH -> POLE_SOUTH;
            case EAST -> POLE_EAST;
            case WEST -> POLE_WEST;
            default -> POLE_NORTH;
        };

        return Shapes.or(panel, pole);
    }

    private VoxelShape getPanelShape(Direction facing) {
        if (this.waterName) {
            return switch (facing) {
                case SOUTH -> E31B_PANEL_SOUTH;
                case EAST -> E31B_PANEL_EAST;
                case WEST -> E31B_PANEL_WEST;
                default -> E31B_PANEL_NORTH;
            };
        }

        return switch (facing) {
            case SOUTH -> E31A_PANEL_SOUTH;
            case EAST -> E31A_PANEL_EAST;
            case WEST -> E31A_PANEL_WEST;
            default -> E31A_PANEL_NORTH;
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new E31BlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof E31BlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            E31ClientHooks.openEditor(
                    pos,
                    blockEntity.getText(),
                    this.waterName
            );
        }

        return InteractionResult.SUCCESS;
    }
}
