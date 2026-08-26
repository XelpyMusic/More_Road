package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public class PlaqueRueRenderState extends BlockEntityRenderState {
    public String line1 = "";
    public String line2 = "";
    public RoadTextFont line1Font = RoadTextFont.L1;
    public RoadTextFont line2Font = RoadTextFont.L1;
    public Direction facing = Direction.NORTH;
    public AttachFace face = AttachFace.WALL;
}
