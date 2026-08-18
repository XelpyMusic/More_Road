package net.xelpy.moreroad.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.entity.MoreRoadBlockEntities;
import net.xelpy.moreroad.client.renderer.D21ABlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.D61ABlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.EB10BlockEntityRenderer;

@EventBusSubscriber(
        modid = MoreRoad.MODID,
        value = Dist.CLIENT
)
public final class MoreRoadClientEvents {

    private MoreRoadClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.EB10.get(),
                EB10BlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.D21A.get(),
                D21ABlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.D61A.get(),
                D61ABlockEntityRenderer::new
        );
    }
}
