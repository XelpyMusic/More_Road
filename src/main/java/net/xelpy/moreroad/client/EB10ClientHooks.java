package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.client.screen.EB10EditScreen;

public final class EB10ClientHooks {

    private EB10ClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            String currentName
    ) {
        Minecraft.getInstance().gui.setScreen(
                new EB10EditScreen(pos, currentName)
        );
    }
}