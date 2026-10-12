package com.alexcoronel.toadmod;

import com.alexcoronel.toadmod.registry.ModEntities;
import com.alexcoronel.toadmod.registry.ModItems;
import com.alexcoronel.toadmod.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class ToadMod implements ModInitializer {
    public static final String MOD_ID = "toadmod";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModEntities.init();
        ModItems.init();
        ToadSpawning.init();
    }
}
