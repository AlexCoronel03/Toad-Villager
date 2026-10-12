package com.alexcoronel.toadmod.registry;

import com.alexcoronel.toadmod.ToadMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
    public static final Item TOAD_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ToadMod.id("toad_spawn_egg"),
            new SpawnEggItem(ModEntities.TOAD, 0xF8F8FA, 0xDE2024, new Item.Properties()));

    private ModItems() {
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(TOAD_SPAWN_EGG));
    }
}
