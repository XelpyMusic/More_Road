package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.client.screen.D21AEditScreen;

public final class D21AClientHooks {

    private D21AClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new D21AEditScreen(
                                pos,
                                panels
                        )
                );
    }
}
