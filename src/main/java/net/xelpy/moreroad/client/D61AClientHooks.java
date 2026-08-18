package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.client.screen.D61AEditScreen;

public final class D61AClientHooks {

    private D61AClientHooks() {
    }

    public static void openEditor(
            BlockPos pos,
            D61APanelData[] panels,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        openMixedEditor(pos, panels, cartoucheType, cartoucheText);
    }

    public static void openEditorTwoLines(
            BlockPos pos,
            D61APanelData[] panels,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        openMixedEditor(pos, panels, cartoucheType, cartoucheText);
    }

    private static void openMixedEditor(
            BlockPos pos,
            D61APanelData[] panels,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new D61AEditScreen(
                                pos,
                                panels,
                                cartoucheType,
                                cartoucheText
                        )
                );
    }
}
