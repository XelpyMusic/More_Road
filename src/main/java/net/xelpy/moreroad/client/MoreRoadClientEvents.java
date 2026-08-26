package net.xelpy.moreroad.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.entity.MoreRoadBlockEntities;
import net.xelpy.moreroad.client.renderer.D21ABlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.DA31CBlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.D42bBlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.D61ABlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.EB10BlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.E31BlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.MotorwaySignBlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.PanonceauBlockEntityRenderer;
import net.xelpy.moreroad.client.renderer.PlaqueRueBlockEntityRenderer;

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
                MoreRoadBlockEntities.E31.get(),
                E31BlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.PLAQUE_RUE.get(),
                PlaqueRueBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.DA31C.get(),
                DA31CBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.MOTORWAY_SIGN.get(),
                MotorwaySignBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.EB10.get(),
                EB10BlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.D21A.get(),
                D21ABlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.D42B.get(),
                D42bBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.D61A.get(),
                D61ABlockEntityRenderer::new
        );


        event.registerBlockEntityRenderer(
                MoreRoadBlockEntities.PANONCEAU.get(),
                PanonceauBlockEntityRenderer::new
        );
    }
}
