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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.client.D21AClientHooks;

public class D21ABlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<D21ABlock> CODEC =
            simpleCodec(D21ABlock::new);

    /*
     * Les deux propriétés sont conservées pour la compatibilité avec les
     * anciens mondes D21A. Dans le nouveau système multi-panneaux, les
     * configurations réelles sont stockées dans la BlockEntity.
     */
    public static final EnumProperty<D21AType> TYPE =
            EnumProperty.create("type", D21AType.class);

    public static final BooleanProperty ARROW_RIGHT =
            BooleanProperty.create("arrow_right");

    /*
     * Pas vertical entre deux panneaux actifs.
     * 0.52 bloc = 8.32 pixels Minecraft : avec une plaque d'environ
     * 7.16 px de haut, il reste environ 1.16 px de séparation.
     */
    public static final double PANEL_VERTICAL_STEP = 0.52D;

    /* ============================================================
     * POTEAU
     * ============================================================ */

    private static final VoxelShape POLE_NORTH =
            Block.box(7.0, 0.0, 8.0, 9.0, 16.0, 10.0);

    private static final VoxelShape POLE_SOUTH =
            Block.box(7.0, 0.0, 6.0, 9.0, 16.0, 8.0);

    private static final VoxelShape POLE_EAST =
            Block.box(6.0, 0.0, 7.0, 8.0, 16.0, 9.0);

    private static final VoxelShape POLE_WEST =
            Block.box(8.0, 0.0, 7.0, 10.0, 16.0, 9.0);

    /* ============================================================
     * PLAQUE - FLÈCHE GAUCHE
     * ============================================================ */

    private static final VoxelShape LEFT_PANEL_NORTH =
            Block.box(-6.96, 7.80, 6.00, 24.96, 14.96, 8.00);

    private static final VoxelShape LEFT_PANEL_EAST =
            Block.box(8.00, 7.80, -6.96, 10.00, 14.96, 24.96);

    private static final VoxelShape LEFT_PANEL_SOUTH =
            Block.box(-8.96, 7.80, 8.00, 22.96, 14.96, 10.00);

    private static final VoxelShape LEFT_PANEL_WEST =
            Block.box(6.00, 7.80, -8.96, 8.00, 14.96, 22.96);

    /* ============================================================
     * PLAQUE - FLÈCHE DROITE
     * ============================================================ */

    private static final VoxelShape RIGHT_PANEL_NORTH =
            Block.box(-9.96, 7.84, 6.00, 21.96, 15.00, 8.00);

    private static final VoxelShape RIGHT_PANEL_EAST =
            Block.box(8.00, 7.84, -9.96, 10.00, 15.00, 21.96);

    private static final VoxelShape RIGHT_PANEL_SOUTH =
            Block.box(-5.96, 7.84, 8.00, 25.96, 15.00, 10.00);

    private static final VoxelShape RIGHT_PANEL_WEST =
            Block.box(6.00, 7.84, -5.96, 8.00, 15.00, 25.96);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public D21ABlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(TYPE, D21AType.WHITE)
                        .setValue(ARROW_RIGHT, false)
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
            int enabledCount = blockEntity.getEnabledPanelCount();

            if (enabledCount <= 0) {
                return result;
            }

            int activeIndex = 0;

            for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
                D21APanelData panel = blockEntity.getPanel(i);

                if (!panel.enabled()) {
                    continue;
                }

                double yOffset =
                        getPanelYOffset(
                                enabledCount,
                                activeIndex
                        );

                VoxelShape panelShape =
                        getPanelShape(
                                facing,
                                panel.arrowRight()
                        ).move(
                                0.0D,
                                yOffset,
                                0.0D
                        );

                result = Shapes.or(result, panelShape);
                activeIndex++;
            }

            return result;
        }

        /*
         * Fallback pendant le chargement de la BlockEntity : on garde une
         * plaque unique selon l'ancien BlockState.
         */
        return Shapes.or(
                result,
                getPanelShape(
                        facing,
                        state.getValue(ARROW_RIGHT)
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

    private static VoxelShape getPanelShape(
            Direction facing,
            boolean arrowRight
    ) {
        if (arrowRight) {
            return switch (facing) {
                case EAST -> RIGHT_PANEL_EAST;
                case SOUTH -> RIGHT_PANEL_SOUTH;
                case WEST -> RIGHT_PANEL_WEST;
                default -> RIGHT_PANEL_NORTH;
            };
        }

        return switch (facing) {
            case EAST -> LEFT_PANEL_EAST;
            case SOUTH -> LEFT_PANEL_SOUTH;
            case WEST -> LEFT_PANEL_WEST;
            default -> LEFT_PANEL_NORTH;
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
        ) {
            facing = belowState.getValue(FACING);
        } else {
            BlockState aboveState =
                    level.getBlockState(pos.above());

            if (
                    aboveState.getBlock() instanceof PoteauBlock
                            || aboveState.getBlock() instanceof D21ABlock
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
            D21AClientHooks.openEditor(
                    pos,
                    blockEntity.getPanels()
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
                TYPE,
                ARROW_RIGHT
        );
    }
}
