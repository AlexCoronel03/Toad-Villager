package com.alexcoronel.toadmod.client;

import com.alexcoronel.toadmod.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ToadModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(ToadModel.LAYER, ToadModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.TOAD, ToadRenderer::new);
        ToadPreview.init();
    }
}
