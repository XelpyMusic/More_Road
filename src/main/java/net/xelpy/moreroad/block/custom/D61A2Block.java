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
import net.xelpy.moreroad.block.entity.D61ABlockEntity;
import net.xelpy.moreroad.client.D61AClientHooks;

public class D61A2Block
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<D61A2Block> CODEC =
            simpleCodec(D61A2Block::new);

    public static final double PANEL_VERTICAL_STEP = 0.64D;

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 8.0, 9.0, 16.0, 10.0);

    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 6.0, 9.0, 16.0, 8.0);

    private static final VoxelShape POLE_EAST =
            Block.box(6.0, 0.0, 7.0, 8.0, 16.0, 9.0);

    private static final VoxelShape POLE_WEST =
            Block.box(8.0, 0.0, 7.0, 10.0, 16.0, 9.0);

    public D61A2Block(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(D61ABlock.TYPE, D21AType.WHITE)
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
        Direction facing = state.getValue(FACING);
        VoxelShape result = getPoleShape(facing);

        if (level.getBlockEntity(pos) instanceof D61ABlockEntity blockEntity) {
            D61APanelData[] panels = blockEntity.getPanels();

            for (int i = 0; i < panels.length; i++) {
                D61APanelData panel = panels[i];

                if (!panel.enabled()) {
                    continue;
                }

                double yOffset =
                        D61APanelLayout.getPanelYOffset(
                                panels,
                                i
                        );

                VoxelShape panelShape =
                        D61APanelLayout.getPanelShape(
                                facing,
                                panel.doubleLine()
                        ).move(
                                0.0D,
                                yOffset,
                                0.0D
                        );

                result = Shapes.or(result, panelShape);
            }

            if (
                    blockEntity.getCartoucheType() != null
                            && blockEntity.getCartoucheType().isVisible()
            ) {
                double cartoucheBottomY = CartoucheLayout.getD61BottomY(panels);

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

        return Shapes.or(
                result,
                D61APanelLayout.getPanelShape(
                        facing,
                        true
                )
        );
    }

    private static VoxelShape getPoleShape(Direction facing) {
        return switch (facing) {
            case EAST -> POLE_EAST;
            case SOUTH -> POLE_SOUTH;
            case WEST -> POLE_WEST;
            default -> POLE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction facing =
                context
                        .getHorizontalDirection()
                        .getOpposite();

        BlockState belowState = level.getBlockState(pos.below());

        if (isCompatiblePoleOrPanel(belowState.getBlock())) {
            facing = belowState.getValue(FACING);
        } else {
            BlockState aboveState = level.getBlockState(pos.above());

            if (isCompatiblePoleOrPanel(aboveState.getBlock())) {
                facing = aboveState.getValue(FACING);
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing);
    }

    private static boolean isCompatiblePoleOrPanel(Block block) {
        return block instanceof PoteauBlock
                || block instanceof D61ABlock
                || block instanceof D61A2Block
                || block instanceof D21ABlock
                || block instanceof D21A2Block
                || block instanceof EB10Block;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new D61ABlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof D61ABlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            D61AClientHooks.openEditorTwoLines(
                    pos,
                    blockEntity.getPanels(),
                    blockEntity.getCartoucheType(),
                    blockEntity.getCartoucheText()
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
                D61ABlock.TYPE
        );
    }
}
