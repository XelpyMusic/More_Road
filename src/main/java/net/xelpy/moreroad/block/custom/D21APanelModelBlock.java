package net.xelpy.moreroad.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Bloc interne sans item, utilisé uniquement comme porte-modèle par le BER D21A.
 * Il permet au BlockModelResolver de résoudre les apparences des panneaux
 * (gauche/droite x blanc/vert/bleu x logo autoroute oui/non)
 * indépendamment du vrai bloc D21A.
 */
public class D21APanelModelBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<D21APanelModelBlock> CODEC =
            simpleCodec(D21APanelModelBlock::new);

    public static final EnumProperty<D21AType> TYPE =
            EnumProperty.create("type", D21AType.class);

    public static final BooleanProperty ARROW_RIGHT =
            BooleanProperty.create("arrow_right");

    public static final BooleanProperty AUTOROUTE_LOGO =
            BooleanProperty.create("autoroute_logo");

    public D21APanelModelBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(TYPE, D21AType.WHITE)
                        .setValue(ARROW_RIGHT, false)
                        .setValue(AUTOROUTE_LOGO, false)
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
                TYPE,
                ARROW_RIGHT,
                AUTOROUTE_LOGO
        );
    }
}
