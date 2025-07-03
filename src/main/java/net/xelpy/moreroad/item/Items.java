package net.xelpy.moreroad.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;

public class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreRoad.MOD_ID);

    public static final DeferredItem<Item> A1A = ITEMS.registerItem("a1a", Item::new, new Item.Properties());



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
