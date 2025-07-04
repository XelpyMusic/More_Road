package net.xelpy.moreroad;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.item.MoreRoadItems;
import org.slf4j.Logger;

@Mod(MoreRoad.MODID)
public class MoreRoad {
    public static final String MODID = "moreroad";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreRoad(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        MoreRoadBlocks.register(modEventBus);
        MoreRoadItems.register(modEventBus);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(MoreRoadItems.TEST);
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(MoreRoadBlocks.A1A);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}