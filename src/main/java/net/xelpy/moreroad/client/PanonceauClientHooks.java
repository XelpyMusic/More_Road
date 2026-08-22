package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.client.screen.PanonceauEditScreen;

public final class PanonceauClientHooks {

    private PanonceauClientHooks() {
    }

    public static void openEditor(BlockPos pos, PanonceauEntry[] entries) {
        Minecraft.getInstance().gui.setScreen(
                new PanonceauEditScreen(pos, entries)
        );
    }
}
