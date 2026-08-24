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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.DA31CBlockEntity;
import net.xelpy.moreroad.client.DA31CClientHooks;

/**
 * Grand panneau autoroutier DA31C.
 *
 * V11 :
 * - 1 à 4 lignes avec hauteur de plaque automatique et plus compacte ;
 * - trois cartouches dynamiques rendus avec les modèles officiels du mod ;
 * - deux flèches indépendantes choisies dans l'éditeur ;
 * - les supports de portique restent indépendants.
 */
public class DA31CBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<DA31CBlock> CODEC = simpleCodec(DA31CBlock::new);

    public static final EnumProperty<CartoucheType> CARTOUCHE_TOP =
            EnumProperty.create("cartouche_top", CartoucheType.class);

    /* Conservés sous leurs anciens noms pour ne pas casser les mondes V6/V9. */
    public static final EnumProperty<CartoucheType> CARTOUCHE_LEFT =
            EnumProperty.create("cartouche_left", CartoucheType.class);

    public static final EnumProperty<CartoucheType> CARTOUCHE_RIGHT =
            EnumProperty.create("cartouche_right", CartoucheType.class);

    public static final IntegerProperty LINE_COUNT =
            IntegerProperty.create("line_count", 1, 4);

    private static final double MIN_PANEL_X = -16.0D;
    private static final double MAX_PANEL_X = 32.0D;
    private static final double PANEL_MAX_Y = 26.0D;
    private static final double PANEL_MIN_Z = 6.5D;
    private static final double PANEL_MAX_Z = 9.5D;

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DA31CBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(CARTOUCHE_TOP, CartoucheType.NONE)
                        .setValue(CARTOUCHE_LEFT, CartoucheType.NONE)
                        .setValue(CARTOUCHE_RIGHT, CartoucheType.NONE)
                        .setValue(LINE_COUNT, 2)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        double minY = getPanelMinY(state.getValue(LINE_COUNT));
        return panelShape(state.getValue(FACING), minY);
    }

    private static VoxelShape panelShape(Direction facing, double minY) {
        return switch (facing) {
            case SOUTH -> Block.box(
                    16.0D - MAX_PANEL_X,
                    minY,
                    16.0D - PANEL_MAX_Z,
                    16.0D - MIN_PANEL_X,
                    PANEL_MAX_Y,
                    16.0D - PANEL_MIN_Z
            );
            case EAST -> Block.box(
                    16.0D - PANEL_MAX_Z,
                    minY,
                    MIN_PANEL_X,
                    16.0D - PANEL_MIN_Z,
                    PANEL_MAX_Y,
                    MAX_PANEL_X
            );
            case WEST -> Block.box(
                    PANEL_MIN_Z,
                    minY,
                    16.0D - MAX_PANEL_X,
                    PANEL_MAX_Z,
                    PANEL_MAX_Y,
                    16.0D - MIN_PANEL_X
            );
            default -> Block.box(
                    MIN_PANEL_X,
                    minY,
                    PANEL_MIN_Z,
                    MAX_PANEL_X,
                    PANEL_MAX_Y,
                    PANEL_MAX_Z
            );
        };
    }

    public static double getPanelMinY(int lineCount) {
        return switch (Math.max(1, Math.min(4, lineCount))) {
            case 1 -> 8.0D;
            case 2 -> 4.0D;
            case 3 -> 0.0D;
            default -> -4.0D;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                FACING,
                CARTOUCHE_TOP,
                CARTOUCHE_LEFT,
                CARTOUCHE_RIGHT,
                LINE_COUNT
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DA31CBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof DA31CBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            DA31CClientHooks.openEditor(
                    pos,
                    blockEntity.getLine1(),
                    blockEntity.getLine2(),
                    blockEntity.getLine3(),
                    blockEntity.getLine4(),
                    blockEntity.getCartoucheTopType(),
                    blockEntity.getCartoucheTopText(),
                    blockEntity.getCartoucheLeftType(),
                    blockEntity.getCartoucheLeftText(),
                    blockEntity.getCartoucheRightType(),
                    blockEntity.getCartoucheRightText(),
                    blockEntity.getArrowLeftType(),
                    blockEntity.getArrowRightType()
            );
        }

        return InteractionResult.SUCCESS;
    }
}
