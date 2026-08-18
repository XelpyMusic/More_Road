package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.client.screen.EB10EditScreen;

public final class EB10ClientHooks {

    private EB10ClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            String line1,
            String line2,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            boolean eb20,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        Minecraft.getInstance().gui.setScreen(
                new EB10EditScreen(
                        pos,
                        line1,
                        line2,
                        line1Font,
                        line2Font,
                        eb20,
                        cartoucheType,
                        cartoucheText
                )
        );
    }
}
