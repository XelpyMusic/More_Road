package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;

public class D42bRenderState extends BlockEntityRenderState {

    public Direction facing = Direction.NORTH;
    public String distanceText = "";

    public final D42bBranchData[] branches =
            new D42bBranchData[D42bBlockEntity.MAX_BRANCHES];

    public final BlockModelRenderState blackModel =
            new BlockModelRenderState();

    public final BlockModelRenderState greenModel =
            new BlockModelRenderState();

    public final BlockModelRenderState blueModel =
            new BlockModelRenderState();

    public final BlockModelRenderState circleModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowUpModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowUpRightModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowUpLeftModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowRightModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowLeftModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowDownLeftModel =
            new BlockModelRenderState();

    public final BlockModelRenderState arrowDownRightModel =
            new BlockModelRenderState();
}
