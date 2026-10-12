package com.alexcoronel.toadmod.registry;

import com.alexcoronel.toadmod.ToadMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent TOAD_WALK = register("entity.toad.walk");
    public static final SoundEvent TOAD_HURT = register("entity.toad.hurt");
    public static final SoundEvent TOAD_DEATH = register("entity.toad.death");
    public static final SoundEvent TOAD_TRADE_YES = register("entity.toad.trade_yes");
    public static final SoundEvent TOAD_TRADE_NO = register("entity.toad.trade_no");
    /** Played when a player tries to interact with a toad that has no profession (vanilla plays villager "no"). */
    public static final SoundEvent TOAD_NO_JOB = register("entity.toad.no_job");

    private ModSounds() {
    }

    private static SoundEvent register(String path) {
        ResourceLocation id = ToadMod.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        // class-load trigger
    }
}
