package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.CartoucheType;

public class EB10RenderState extends BlockEntityRenderState {

    public String line1 = "";
    public String line2 = "";

    public CartoucheType cartoucheType = CartoucheType.NONE;
    public String cartoucheText = "";

    public final BlockModelRenderState cartoucheModel =
            new BlockModelRenderState();

    public final BlockModelRenderState cartoucheSupportModel =
            new BlockModelRenderState();

    public double cartoucheSupportOffsetX = 0.0D;
    public double cartoucheSupportOffsetZ = 0.0D;
    public double cartoucheSupportPoleTopY = 1.0D;

    public Direction facing = Direction.NORTH;
}