package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;

public class D21ARenderState extends BlockEntityRenderState {

    public final String[] line1 =
            new String[D21ABlockEntity.MAX_PANELS];

    public final String[] line2 =
            new String[D21ABlockEntity.MAX_PANELS];

    public final String[] distance1 =
            new String[D21ABlockEntity.MAX_PANELS];

    public final String[] distance2 =
            new String[D21ABlockEntity.MAX_PANELS];

    public final D21AType[] panelTypes =
            new D21AType[D21ABlockEntity.MAX_PANELS];

    public final boolean[] arrowRights =
            new boolean[D21ABlockEntity.MAX_PANELS];

    public final boolean[] autorouteLogos =
            new boolean[D21ABlockEntity.MAX_PANELS];

    public final boolean[] enabled =
            new boolean[D21ABlockEntity.MAX_PANELS];

    public final boolean[] doubleLines =
            new boolean[D21ABlockEntity.MAX_PANELS];

    public final BlockModelRenderState[] panelModels = {
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState(),
            new BlockModelRenderState()
    };

    public Direction facing = Direction.NORTH;
}
