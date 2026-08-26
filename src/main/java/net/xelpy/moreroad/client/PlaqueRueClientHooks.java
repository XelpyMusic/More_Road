package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.client.screen.PlaqueRueEditScreen;

public final class PlaqueRueClientHooks {

    private PlaqueRueClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            String line1,
            String line2,
            RoadTextFont line1Font,
            RoadTextFont line2Font
    ) {
        Minecraft.getInstance().gui.setScreen(
                new PlaqueRueEditScreen(pos, line1, line2, line1Font, line2Font)
        );
    }
}
