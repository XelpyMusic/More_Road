package net.xelpy.moreroad.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;

public class MoreRoadItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreRoad.MODID);

    public static final DeferredItem<Item> LOGO =
            ITEMS.registerItem("logo", Item::new);

    public static final DeferredItem<Item> LOGO2 =
            ITEMS.registerItem("logo2", Item::new);

    public static final DeferredItem<Item> ROAD_BUILDER =
            ITEMS.registerItem("road_builder", RoadBuilderItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
