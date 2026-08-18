package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.B14Speed;
import net.xelpy.moreroad.client.screen.B14EditScreen;

public final class B14ClientHooks {

    private B14ClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            B14Speed currentSpeed
    ) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new B14EditScreen(
                                pos,
                                currentSpeed
                        )
                );
    }
}
