package net.xelpy.moreroad;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.xelpy.moreroad.block.MoreRoadBlocks;
import net.xelpy.moreroad.block.entity.MoreRoadBlockEntities;
import net.xelpy.moreroad.command.RoadBuilderCommands;
import net.xelpy.moreroad.item.MoreRoadCreativeModeTabs;
import net.xelpy.moreroad.item.MoreRoadCreativeModeTabs2;
import net.xelpy.moreroad.item.MoreRoadItems;
import net.xelpy.moreroad.network.MoreRoadNetworking;
import org.slf4j.Logger;

@Mod(MoreRoad.MODID)
public class MoreRoad {
    public static final String MODID = "moreroad";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreRoad(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(MoreRoadNetworking::register);

        NeoForge.EVENT_BUS.register(this);

        MoreRoadCreativeModeTabs.register(modEventBus);
        MoreRoadCreativeModeTabs2.register(modEventBus);

        MoreRoadItems.register(modEventBus);
        MoreRoadBlocks.register(modEventBus);
        MoreRoadBlockEntities.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RoadBuilderCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
