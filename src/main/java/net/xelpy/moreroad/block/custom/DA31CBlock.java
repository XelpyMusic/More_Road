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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.xelpy.moreroad.block.entity.DA31CBlockEntity;
import net.xelpy.moreroad.client.DA31CClientHooks;

/**
 * Grand panneau autoroutier DA31C.
 *
 * V6 : les cartouches sont de vrais éléments du blockstate (deux emplacements
 * indépendants), exactement comme le principe des autres panneaux modifiables.
 * La plaque possède en plus deux bras arrière suffisamment longs pour rejoindre
 * une traverse placée dans le bloc directement derrière le panneau.
 */
public class DA31CBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<DA31CBlock> CODEC = simpleCodec(DA31CBlock::new);

    public static final EnumProperty<CartoucheType> CARTOUCHE_LEFT =
            EnumProperty.create("cartouche_left", CartoucheType.class);

    public static final EnumProperty<CartoucheType> CARTOUCHE_RIGHT =
            EnumProperty.create("cartouche_right", CartoucheType.class);

    private static final double MIN_PANEL_X = -16.0D;
    private static final double MAX_PANEL_X = 32.0D;
    private static final double PANEL_MIN_Y = 1.0D;
    private static final double PANEL_MAX_Y = 26.0D;
    private static final double PANEL_MIN_Z = 6.5D;
    private static final double PANEL_MAX_Z = 9.5D;

    private static final VoxelShape PANEL_NORTH =
            Block.box(MIN_PANEL_X, PANEL_MIN_Y, 6.5D, MAX_PANEL_X, PANEL_MAX_Y, PANEL_MAX_Z);
    private static final VoxelShape PANEL_SOUTH =
            Block.box(16.0D - MAX_PANEL_X, PANEL_MIN_Y, 16.0D - PANEL_MAX_Z,
                    16.0D - MIN_PANEL_X, PANEL_MAX_Y, 16.0D - 6.5D);
    private static final VoxelShape PANEL_EAST =
            Block.box(16.0D - PANEL_MAX_Z, PANEL_MIN_Y, MIN_PANEL_X,
                    16.0D - 6.5D, PANEL_MAX_Y, MAX_PANEL_X);
    private static final VoxelShape PANEL_WEST =
            Block.box(6.5D, PANEL_MIN_Y, 16.0D - MAX_PANEL_X,
                    PANEL_MAX_Z, PANEL_MAX_Y, 16.0D - MIN_PANEL_X);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DA31CBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(CARTOUCHE_LEFT, CartoucheType.NONE)
                        .setValue(CARTOUCHE_RIGHT, CartoucheType.NONE)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> PANEL_SOUTH;
            case EAST -> PANEL_EAST;
            case WEST -> PANEL_WEST;
            default -> PANEL_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CARTOUCHE_LEFT, CARTOUCHE_RIGHT);
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
                    blockEntity.getCartoucheLeftType(),
                    blockEntity.getCartoucheLeftText(),
                    blockEntity.getCartoucheRightType(),
                    blockEntity.getCartoucheRightText()
            );
        }

        return InteractionResult.SUCCESS;
    }
}
