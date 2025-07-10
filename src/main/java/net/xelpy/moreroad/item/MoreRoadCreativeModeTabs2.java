package net.xelpy.moreroad.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.MoreRoadBlocks;

import java.util.function.Supplier;

public class MoreRoadCreativeModeTabs2 {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreRoad.MODID);

    public static final Supplier<CreativeModeTab> MORE_ROAD_TAB2 = CREATIVE_MODE_TAB.register("more_road_tab2",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(MoreRoadItems.LOGO2.get()))
                    .title(Component.translatable("Mobilier Routier"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(MoreRoadBlocks.POTEAU);
                        output.accept(MoreRoadBlocks.PIEDLAMPADAIRE);
                        output.accept(MoreRoadBlocks.POTEAULAMPADAIRE);
                        output.accept(MoreRoadBlocks.LUMINAIRELAMPADAIRE);
                        output.accept(MoreRoadBlocks.PIEDLAMPADAIREDOUBLE);
                        output.accept(MoreRoadBlocks.POTEAULAMPADAIREDOUBLE);
                        output.accept(MoreRoadBlocks.LUMINAIRELAMPADAIREDOUBLE);
                        output.accept(MoreRoadBlocks.FEUTRICOLORE);
                        output.accept(MoreRoadBlocks.BANC);
                        output.accept(MoreRoadBlocks.CONE);
                        output.accept(MoreRoadBlocks.BLOCDEGUIDAGE);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
