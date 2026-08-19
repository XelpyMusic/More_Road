package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.client.screen.E31EditScreen;

public final class E31ClientHooks {

    private E31ClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            String currentText,
            boolean waterName
    ) {
        Minecraft.getInstance().gui.setScreen(
                new E31EditScreen(
                        pos,
                        currentText,
                        waterName
                )
        );
    }
}
