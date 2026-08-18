package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;

public class D61ARenderState extends BlockEntityRenderState {

    public final String[] line1 = new String[D61ABlockEntity.MAX_PANELS];
    public final String[] line2 = new String[D61ABlockEntity.MAX_PANELS];
    public final String[] distance1 = new String[D61ABlockEntity.MAX_PANELS];
    public final String[] distance2 = new String[D61ABlockEntity.MAX_PANELS];

    public final D21AType[] panelTypes = new D21AType[D61ABlockEntity.MAX_PANELS];
    public final boolean[] enabled = new boolean[D61ABlockEntity.MAX_PANELS];
    public final boolean[] doubleLines = new boolean[D61ABlockEntity.MAX_PANELS];
    public final boolean[] autorouteLogos = new boolean[D61ABlockEntity.MAX_PANELS];

    public final boolean[] arrowEnabled = new boolean[D61ABlockEntity.MAX_PANELS];
    public final D61AArrowPosition[] arrowPositions = new D61AArrowPosition[D61ABlockEntity.MAX_PANELS];
    public final D61AArrowDirection[] arrowDirections = new D61AArrowDirection[D61ABlockEntity.MAX_PANELS];

    public CartoucheType cartoucheType = CartoucheType.NONE;
    public String cartoucheText = "";

    public final BlockModelRenderState cartoucheModel =
            new BlockModelRenderState();

    public final BlockModelRenderState cartoucheSupportModel =
            new BlockModelRenderState();

    public double cartoucheSupportOffsetX = 0.0D;
    public double cartoucheSupportOffsetZ = 0.0D;
    public double cartoucheSupportPoleTopY = 1.0D;

    public final BlockModelRenderState[] panelModels = {
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState()
    };

    public final BlockModelRenderState[] arrowModels = {
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState()
    };

    public Direction facing = Direction.NORTH;
}
