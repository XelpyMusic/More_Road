package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;

public class GenericDirectionalSignRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public GenericDirectionalSignData data = GenericDirectionalSignData.blank();
    /** Même modèle de support que le D31b/D61B/MotorwaySign (voir SUPPORT_DA31C_POTEAU), étiré en hauteur. */
    public final BlockModelRenderState poleModel = new BlockModelRenderState();
}
