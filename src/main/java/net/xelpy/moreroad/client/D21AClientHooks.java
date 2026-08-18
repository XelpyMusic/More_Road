package net.xelpy.moreroad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.client.screen.D21A2EditScreen;

public final class D21AClientHooks {

    private D21AClientHooks() {
    }

    /*
     * D21A et D21A2 ouvrent désormais le même éditeur : le format simple
     * ou double est choisi indépendamment pour chaque panneau.
     */
    public static void openEditor(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        openMixedEditor(pos, panels);
    }

    public static void openEditorTwoLines(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        openMixedEditor(pos, panels);
    }

    private static void openMixedEditor(
            BlockPos pos,
            D21APanelData[] panels
    ) {
        Minecraft.getInstance()
                .gui
                .setScreen(
                        new D21A2EditScreen(
                                pos,
                                panels
                        )
                );
    }
}
