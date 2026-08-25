package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.DA31CArrowType;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public class DA31CRenderState extends BlockEntityRenderState {

    public String line1 = "";
    public String line2 = "";
    public String line3 = "";
    public String line4 = "";
    public RoadTextFont line1Font = RoadTextFont.L1;
    public RoadTextFont line2Font = RoadTextFont.L1;
    public RoadTextFont line3Font = RoadTextFont.L1;
    public RoadTextFont line4Font = RoadTextFont.L1;
    public int lineCount = 2;

    public CartoucheType cartoucheTopType = CartoucheType.NONE;
    public String cartoucheTopText = "";
    public CartoucheType cartoucheLeftType = CartoucheType.NONE;
    public String cartoucheLeftText = "";
    public CartoucheType cartoucheRightType = CartoucheType.NONE;
    public String cartoucheRightText = "";

    public DA31CArrowType arrowLeftType = DA31CArrowType.DOWN;
    public DA31CArrowType arrowRightType = DA31CArrowType.DOWN;

    public final BlockModelRenderState cartoucheTopModel = new BlockModelRenderState();
    public final BlockModelRenderState cartoucheLeftModel = new BlockModelRenderState();
    public final BlockModelRenderState cartoucheRightModel = new BlockModelRenderState();
    public final BlockModelRenderState cartoucheSupportModel = new BlockModelRenderState();

    public Direction facing = Direction.NORTH;
}
