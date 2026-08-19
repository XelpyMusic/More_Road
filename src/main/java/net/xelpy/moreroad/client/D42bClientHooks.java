package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.client.screen.D42bEditScreen;

public final class D42bClientHooks {

    private D42bClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            D42bBranchData[] branches,
            String distanceText
    ) {
        Minecraft.getInstance().gui.setScreen(
                new D42bEditScreen(pos, branches, distanceText)
        );
    }
}
