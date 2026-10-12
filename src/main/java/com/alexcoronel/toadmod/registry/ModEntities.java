package com.alexcoronel.toadmod.registry;

import com.alexcoronel.toadmod.ToadMod;
import com.alexcoronel.toadmod.entity.ToadEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;

public final class ModEntities {
    public static final EntityType<ToadEntity> TOAD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ToadMod.id("toad"),
            EntityType.Builder.of(ToadEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.25F)
                    .eyeHeight(0.8F)
                    .clientTrackingRange(10)
                    .build("toad"));

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(TOAD, Villager.createAttributes());
    }
}
