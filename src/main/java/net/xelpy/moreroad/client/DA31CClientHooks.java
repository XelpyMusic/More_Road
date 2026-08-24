package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.client.screen.DA31CEditScreen;

public final class DA31CClientHooks {

    private DA31CClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            String line1,
            String line2,
            CartoucheType cartoucheLeftType,
            String cartoucheLeftText,
            CartoucheType cartoucheRightType,
            String cartoucheRightText
    ) {
        Minecraft.getInstance().gui.setScreen(
                new DA31CEditScreen(
                        pos,
                        line1,
                        line2,
                        cartoucheLeftType,
                        cartoucheLeftText,
                        cartoucheRightType,
                        cartoucheRightText
                )
        );
    }
}
