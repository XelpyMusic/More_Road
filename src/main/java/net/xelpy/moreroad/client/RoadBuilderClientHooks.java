package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.client.screen.RoadBuilderPreviewScreen;

public final class RoadBuilderClientHooks {

    private RoadBuilderClientHooks() {
    }

    public static void openEditor(
            BlockPos start,
            BlockPos control,
            BlockPos end
    ) {
        Minecraft.getInstance().gui.setScreen(
                new RoadBuilderPreviewScreen(
                        start,
                        control,
                        end
                )
        );
    }
}
