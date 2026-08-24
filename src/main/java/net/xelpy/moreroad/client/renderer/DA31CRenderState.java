package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.CartoucheType;

public class DA31CRenderState extends BlockEntityRenderState {

    public String line1 = "";
    public String line2 = "";

    public CartoucheType cartoucheLeftType = CartoucheType.NONE;
    public String cartoucheLeftText = "";

    public CartoucheType cartoucheRightType = CartoucheType.NONE;
    public String cartoucheRightText = "";

    public Direction facing = Direction.NORTH;
}
