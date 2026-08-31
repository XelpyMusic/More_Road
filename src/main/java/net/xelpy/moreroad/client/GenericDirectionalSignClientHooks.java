package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;
import net.xelpy.moreroad.client.screen.GenericDirectionalSignEditScreen;

public final class GenericDirectionalSignClientHooks {

    private GenericDirectionalSignClientHooks() {
    }

    public static void openEditor(BlockPos pos, GenericDirectionalSignData data) {
        Minecraft.getInstance().gui.setScreen(new GenericDirectionalSignEditScreen(pos, data));
    }
}
