package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.client.screen.D61AEditScreen;

public final class D61AClientHooks {

    private D61AClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        openMixedEditor(pos, panels);
    }

    public static void openEditorTwoLines(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        openMixedEditor(pos, panels);
    }

    private static void openMixedEditor(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new D61AEditScreen(
                                pos,
                                panels
                        )
                );
    }
}
