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
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.client.D21AClientHooks;

public class D21A2Block
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<D21A2Block> CODEC =
            simpleCodec(D21A2Block::new);

    /*
     * Espacement historique des ensembles composés uniquement de D21A2.
     * D21APanelLayout le conserve automatiquement pour un poteau 100 %
     * double, tout en calculant un espacement adapté pour les mélanges.
     */
    public static final double PANEL_VERTICAL_STEP = 0.64D;

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

    public D21A2Block(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(D21ABlock.TYPE, D21AType.WHITE)
                        .setValue(D21ABlock.ARROW_RIGHT, false)
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
        VoxelShape result = getPoleShape(facing);

        if (level.getBlockEntity(pos) instanceof D21ABlockEntity blockEntity) {
            D21APanelData[] panels = blockEntity.getPanels();

            for (int i = 0; i < panels.length; i++) {
                D21APanelData panel = panels[i];

                if (!panel.enabled()) {
                    continue;
                }

                double yOffset =
                        D21APanelLayout.getPanelYOffset(
                                panels,
                                i
                        );

                VoxelShape panelShape =
                        D21APanelLayout.getPanelShape(
                                facing,
                                panel.arrowRight(),
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
                double cartoucheBottomY = CartoucheLayout.getD21BottomY(panels);

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

        /*
         * Fallback pendant le chargement : un bloc D21A2 neuf reste double.
         */
        return Shapes.or(
                result,
                D21APanelLayout.getPanelShape(
                        facing,
                        state.getValue(D21ABlock.ARROW_RIGHT),
                        true
                )
        );
    }

    public static double getPanelYOffset(
            int enabledCount,
            int activeIndex
    ) {
        if (enabledCount <= 1) {
            return 0.0D;
        }

        return (
                (enabledCount - 1) / 2.0D
                        - activeIndex
        ) * PANEL_VERTICAL_STEP;
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
                .setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new D21ABlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof D21ABlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            D21AClientHooks.openEditorTwoLines(
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
                D21ABlock.TYPE,
                D21ABlock.ARROW_RIGHT
        );
    }
}
