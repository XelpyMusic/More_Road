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
import net.minecraft.world.phys.shapes.Shapes;
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

    /*
     * Le panneau EB10/EB20 a désormais son propre poteau visuel,
     * comme les D21A. La hitbox combine donc :
     *
     * - le segment de poteau centré ;
     * - le panneau, aligné sur les dimensions réelles du modèle.
     *
     * Dimensions exactes relevées dans les modèles Blockbench :
     *
     * EB10 : X -8 -> 24, Y 3.65 -> 15, Z 6 -> 8
     * EB20 : X -8 -> 24, Y 3.65 -> 15, Z 5.8 -> 8
     */
    private static final VoxelShape EB10_PANEL_NORTH =
            Block.box(-8.0, 3.65, 5.0, 24.0, 15.0, 7.0);

    private static final VoxelShape EB10_PANEL_SOUTH =
            Block.box(-8.0, 3.65, 9.0, 24.0, 15.0, 11.0);

    private static final VoxelShape EB10_PANEL_EAST =
            Block.box(9.0, 3.65, -8.0, 11.0, 15.0, 24.0);

    private static final VoxelShape EB10_PANEL_WEST =
            Block.box(5.0, 3.65, -8.0, 7.0, 15.0, 24.0);

    private static final VoxelShape EB20_PANEL_NORTH =
            Block.box(-8.0, 3.65, 4.8, 24.0, 15.0, 7.0);

    private static final VoxelShape EB20_PANEL_SOUTH =
            Block.box(-8.0, 3.65, 9.0, 24.0, 15.0, 11.2);

    private static final VoxelShape EB20_PANEL_EAST =
            Block.box(9.0, 3.65, -8.0, 11.2, 15.0, 24.0);

    private static final VoxelShape EB20_PANEL_WEST =
            Block.box(4.8, 3.65, -8.0, 7.0, 15.0, 24.0);

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    private static final VoxelShape POLE_EAST =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    private static final VoxelShape POLE_WEST =
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public EB10Block(Properties properties) {
        super(properties);

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
        Direction facing = state.getValue(FACING);
        boolean eb20 = state.getValue(EB20);

        VoxelShape panelShape = getPanelShape(facing, eb20);
        VoxelShape poleShape = getPoleShape(facing);
        VoxelShape result = Shapes.or(poleShape, panelShape);

        if (level.getBlockEntity(pos) instanceof EB10BlockEntity blockEntity
                && blockEntity.getCartoucheType() != null
                && blockEntity.getCartoucheType().isVisible()) {
            double cartoucheBottomY = CartoucheLayout.getEBBottomY();

            CartoucheLayout.PoleAnchor poleAnchor =
                    CartoucheLayout.findNearestPoleAnchor(
                            level,
                            pos,
                            facing,
                            cartoucheBottomY
                    );

            result = Shapes.or(
                    result,
                    CartoucheLayout.getSupportShape(
                            facing,
                            poleAnchor,
                            cartoucheBottomY
                    )
            );

            result = Shapes.or(
                    result,
                    CartoucheLayout.getCartoucheShape(
                            facing,
                            cartoucheBottomY
                    )
            );
        }

        return result;
    }

    private static VoxelShape getPanelShape(Direction facing, boolean eb20) {
        return switch (facing) {
            case SOUTH -> eb20 ? EB20_PANEL_SOUTH : EB10_PANEL_SOUTH;
            case EAST -> eb20 ? EB20_PANEL_EAST : EB10_PANEL_EAST;
            case WEST -> eb20 ? EB20_PANEL_WEST : EB10_PANEL_WEST;
            default -> eb20 ? EB20_PANEL_NORTH : EB10_PANEL_NORTH;
        };
    }

    private static VoxelShape getPoleShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> POLE_SOUTH;
            case EAST -> POLE_EAST;
            case WEST -> POLE_WEST;
            default -> POLE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction facing =
                context.getHorizontalDirection().getOpposite();

        BlockState belowState =
                level.getBlockState(pos.below());

        if (
                belowState.getBlock() instanceof PoteauBlock
                        || belowState.getBlock() instanceof D21ABlock
                        || belowState.getBlock() instanceof D21A2Block
                        || belowState.getBlock() instanceof EB10Block
        ) {
            facing = belowState.getValue(FACING);
        } else {
            BlockState aboveState =
                    level.getBlockState(pos.above());

            if (
                    aboveState.getBlock() instanceof PoteauBlock
                            || aboveState.getBlock() instanceof D21ABlock
                            || aboveState.getBlock() instanceof D21A2Block
                            || aboveState.getBlock() instanceof EB10Block
            ) {
                facing = aboveState.getValue(FACING);
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
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
                    blockEntity.getLine1Font(),
                    blockEntity.getLine2Font(),
                    state.getValue(EB20),
                    blockEntity.getCartoucheType(),
                    blockEntity.getCartoucheText()
            );
        }

        return InteractionResult.SUCCESS;
    }
}
