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

    public static final Supplier<CreativeModeTab> TAB2 = CREATIVE_MODE_TAB.register("tab2",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(MoreRoadItems.LOGO2.get()))
                    .title(Component.translatable("creativetab.moreroad.logo2"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(MoreRoadBlocks.POTEAU);
                        output.accept(MoreRoadBlocks.BITUME);
                        output.accept(MoreRoadBlocks.DALLEDEBITUME);
                        output.accept(MoreRoadBlocks.LIGNECEDEZLEPASSAGE);
                        output.accept(MoreRoadBlocks.FLECHE);
                        output.accept(MoreRoadBlocks.LIGNECONTINUE);
                        output.accept(MoreRoadBlocks.LIGNEDEDISSUASION);
                        output.accept(MoreRoadBlocks.LIGNEDERIVE);
                        output.accept(MoreRoadBlocks.LIGNEDERIVECONTINUE);
                        output.accept(MoreRoadBlocks.LIGNEDESTOP);
                        output.accept(MoreRoadBlocks.LIGNEDISCONTINUE);
                        output.accept(MoreRoadBlocks.PASSAGEPIETON);
                        output.accept(MoreRoadBlocks.GLISSIEREDESECURITE);
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
                        output.accept(MoreRoadBlocks.POTEAUELECTRIQUE);
                        output.accept(MoreRoadBlocks.SUPPORTDESCABLESELECTRIQUES);
                        output.accept(MoreRoadBlocks.CABLESELECTRIQUES);
                        output.accept(MoreRoadBlocks.BORNEINCENDIE);
                        output.accept(MoreRoadBlocks.BORNEECLAIRAGE);
                        output.accept(MoreRoadBlocks.BARRIEREDESECURITE);
                        output.accept(MoreRoadBlocks.POUBELLE);
                        output.accept(MoreRoadBlocks.TABLEDEPIQUENIQUE);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
