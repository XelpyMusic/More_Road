package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.client.screen.MotorwaySignEditScreen;
import net.xelpy.moreroad.client.screen.MotorwayD61BEditScreen;
import net.xelpy.moreroad.client.screen.MotorwayD63CEditScreen;

public final class MotorwaySignClientHooks {

    private MotorwaySignClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] lines,
            boolean customMode,
            MotorwaySignPanelData[] customPanels
    ) {
        ScreenFactory.open(pos, preset, lines, customMode, customPanels);
    }

    private static final class ScreenFactory {
        private static void open(
                BlockPos pos,
                MotorwaySignPreset preset,
                MotorwaySignLineData[] lines,
                boolean customMode,
                MotorwaySignPanelData[] customPanels
        ) {
            if (preset == MotorwaySignPreset.D61B) {
                Minecraft.getInstance().gui.setScreen(
                        new MotorwayD61BEditScreen(pos, lines, customPanels)
                );
            } else if (preset == MotorwaySignPreset.D63C) {
                Minecraft.getInstance().gui.setScreen(
                        new MotorwayD63CEditScreen(pos, lines, customPanels)
                );
            } else {
                Minecraft.getInstance().gui.setScreen(
                        new MotorwaySignEditScreen(pos, preset, lines, customMode, customPanels)
                );
            }
        }

        private ScreenFactory() {
        }
    }
}
