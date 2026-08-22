package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;

public class PanonceauRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public final PanonceauEntry[] entries = new PanonceauEntry[PanonceauBlockEntity.MAX_PANONCEAUX];
}
