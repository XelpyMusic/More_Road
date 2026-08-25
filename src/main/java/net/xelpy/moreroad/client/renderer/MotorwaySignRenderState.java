package net.xelpy.moreroad.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;

public class MotorwaySignRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean mountedOnCrossbar;
    public boolean customMode;
    public MotorwaySignPreset preset = MotorwaySignPreset.D31B_EX1;
    public final MotorwaySignLineData[] lines = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
    public final MotorwaySignPanelData[] customPanels =
            new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    public final BlockModelRenderState[] cartoucheModels =
            new BlockModelRenderState[CartoucheType.values().length];
    public final BlockModelRenderState d61CartoucheSupportModel = new BlockModelRenderState();
    public final BlockModelRenderState d61PoleModel = new BlockModelRenderState();
    public final BlockModelRenderState d61FootModel = new BlockModelRenderState();
    public int d61PoleLightCoords;
    public int d61PoleBlocksBelow;
    public boolean d61FootBelow;

    public MotorwaySignRenderState() {
        for (int index = 0; index < this.cartoucheModels.length; index++) {
            this.cartoucheModels[index] = new BlockModelRenderState();
        }
    }
}
