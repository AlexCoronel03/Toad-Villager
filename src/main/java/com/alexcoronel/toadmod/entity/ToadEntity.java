package com.alexcoronel.toadmod.entity;

import com.alexcoronel.toadmod.ToadCapColor;
import com.alexcoronel.toadmod.registry.ModEntities;
import com.alexcoronel.toadmod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Toad is a real {@link Villager} subclass, so it inherits the complete vanilla brain (schedule, beds, village
 * points of interest, panic, gossip, breeding, golem summoning) and the vanilla merchant framework. On top of
 * that it adds a persistent, synced cap colour and a few synced animation flags for the client model.
 */
public class ToadEntity extends Villager {
    private static final EntityDataAccessor<Integer> DATA_CAP_COLOR =
            SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLEEING =
            SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TRADING =
            SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);

    /** The walk clip plays at most once every 50-90 ticks (2.5-4.5 s) while moving, not on every footstep. */
    private static final int WALK_SOUND_MIN_INTERVAL_TICKS = 50;
    private static final int WALK_SOUND_EXTRA_RANDOM_TICKS = 40;

    private int nextWalkSoundTick;

    private static final String TAG_CAP_COLOR = "CapColor";

    public ToadEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        // Equal-probability colour chosen once, at construction. Loading from NBT overrides it, and clients
        // receive the server's value through the synced data, so it never changes after that.
        this.entityData.set(DATA_CAP_COLOR, this.random.nextInt(ToadCapColor.values().length));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CAP_COLOR, 0);
        builder.define(DATA_FLEEING, false);
        builder.define(DATA_TRADING, false);
    }

    public ToadCapColor getCapColor() {
        return ToadCapColor.byIndex(this.entityData.get(DATA_CAP_COLOR));
    }

    public void setCapColor(ToadCapColor color) {
        this.entityData.set(DATA_CAP_COLOR, color.ordinal());
    }

    /** Client-side animation flag: the toad is in the vanilla PANIC activity. */
    public boolean isFleeing() {
        return this.entityData.get(DATA_FLEEING);
    }

    /** Client-side animation flag: a player has the trade screen open. */
    public boolean isBeingTradedWith() {
        return this.entityData.get(DATA_TRADING);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.entityData.set(DATA_FLEEING, this.getBrain().isActive(Activity.PANIC));
        this.entityData.set(DATA_TRADING, this.isTrading());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(TAG_CAP_COLOR, (byte) this.getCapColor().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_CAP_COLOR)) {
            this.entityData.set(DATA_CAP_COLOR, (int) tag.getByte(TAG_CAP_COLOR));
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        // Vanilla behaviour: toads are born unemployed and take a profession from a nearby job-site block.
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    public ToadEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        ToadEntity baby = ModEntities.TOAD.create(level);
        if (baby != null) {
            baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()),
                    MobSpawnType.BREEDING, null);
        }
        return baby;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Keeps toads out of the animal mob cap and never despawns them (natural spawning is capped separately). */
    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // ---- sounds: Toad's own voice instead of the vanilla villager grunts ----

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.tickCount >= this.nextWalkSoundTick) {
            this.nextWalkSoundTick = this.tickCount + WALK_SOUND_MIN_INTERVAL_TICKS
                    + this.random.nextInt(WALK_SOUND_EXTRA_RANDOM_TICKS);
            this.playSound(ModSounds.TOAD_WALK, 0.7F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TOAD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TOAD_DEATH;
    }

    /** No idle voice has been provided, so Toad stays quiet instead of using the villager "hmm". */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    /** Vanilla plays "yes" when an offer becomes available/completes and "no" when the result is cleared. */
    @Override
    protected SoundEvent getTradeUpdatedSound(boolean isYesSound) {
        return isYesSound ? ModSounds.TOAD_TRADE_YES : ModSounds.TOAD_TRADE_NO;
    }

    /**
     * Vanilla's unemployed/refused interaction (Villager.setUnhappy) plays the villager "no" grunt through this
     * method; swap it for Toad's own "no job" voice.
     */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound == SoundEvents.VILLAGER_NO ? ModSounds.TOAD_NO_JOB : sound, volume, pitch);
    }
}
