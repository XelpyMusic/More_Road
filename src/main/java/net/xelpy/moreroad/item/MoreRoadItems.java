package net.xelpy.moreroad.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;

public class MoreRoadItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreRoad.MODID);

    public static final DeferredItem<Item> LOGO = ITEMS.registerItem("logo",
            Item::new, new Item.Properties());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}