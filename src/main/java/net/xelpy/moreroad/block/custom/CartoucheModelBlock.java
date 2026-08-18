package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Bloc interne sans BlockItem. Il sert uniquement de porte-modèle pour les
 * cartouches E41/E45, E42, E43, E44 et E47 dans les BlockEntityRenderer.
 */
public class CartoucheModelBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<CartoucheModelBlock> CODEC =
            simpleCodec(CartoucheModelBlock::new);

    public static final EnumProperty<CartoucheType> TYPE =
            EnumProperty.create("type", CartoucheType.class);

    public CartoucheModelBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(TYPE, CartoucheType.NONE)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                TYPE
        );
    }
}
