package com.alexcoronel.toadmod;

import com.alexcoronel.toadmod.entity.ToadEntity;
import com.alexcoronel.toadmod.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Village population: when a chunk containing the start of a village is generated for the first time, 1-3 toads
 * are placed inside it ({@link #populateVillage}). Vanilla passive-mob spawning after generation is extremely
 * rare, so the pool entry below only adds an occasional extra toad later. Both are restricted to villages.
 *
 * Natural spawning: rare, inside village structures only, and never more than {@link #MAX_NEARBY} toads within
 * {@link #CHECK_RADIUS} blocks, so toads add flavour to villages without replacing villagers.
 */
public final class ToadSpawning {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToadMod.MOD_ID);
    private static final int MAX_NEARBY = 3;
    private static final double CHECK_RADIUS = 64.0;

    private ToadSpawning() {
    }

    public static void init() {
        SpawnPlacements.register(ModEntities.TOAD, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ToadSpawning::checkSpawnRules);

        ServerChunkEvents.CHUNK_GENERATE.register(ToadSpawning::onChunkGenerate);

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.DESERT,
                        Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SNOWY_TAIGA),
                MobCategory.CREATURE, ModEntities.TOAD, 6, 1, 2);
    }

    private static boolean checkSpawnRules(EntityType<ToadEntity> type, ServerLevelAccessor level,
                                           MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.CHUNK_GENERATION) {
            return false; // villages are not guaranteed to be fully generated yet
        }
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (reason != MobSpawnType.NATURAL) {
            return true; // spawn eggs, /summon, spawners, breeding
        }
        return level.getLevel().isVillage(pos) && countNearby(level, pos) < MAX_NEARBY;
    }

    private static int countNearby(ServerLevelAccessor level, BlockPos pos) {
        return level.getEntitiesOfClass(ToadEntity.class, new AABB(pos).inflate(CHECK_RADIUS)).size();
    }

    private static void onChunkGenerate(ServerLevel level, LevelChunk chunk) {
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid() || !level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE)) {
                continue;
            }
            LOGGER.info("Village start generated at {}, scheduling toads", chunk.getPos());
            // execute() would run inline when already on the server thread (we are inside chunk promotion),
            // so queue a real task for a later tick instead.
            MinecraftServer server = level.getServer();
            server.tell(new TickTask(server.getTickCount() + 20, () -> populateVillage(level, chunk, start)));
            return;
        }
    }

    private static void populateVillage(ServerLevel level, LevelChunk chunk, StructureStart start) {
        LOGGER.info("Populating village near {}", chunk.getPos());
        var box = start.getBoundingBox();
        int minX = Math.max(box.minX(), chunk.getPos().getMinBlockX());
        int maxX = Math.min(box.maxX(), chunk.getPos().getMaxBlockX());
        int minZ = Math.max(box.minZ(), chunk.getPos().getMinBlockZ());
        int maxZ = Math.min(box.maxZ(), chunk.getPos().getMaxBlockZ());
        if (minX > maxX || minZ > maxZ) {
            LOGGER.info("Village box {} does not intersect start chunk {}", box, chunk.getPos());
            return;
        }
        int wanted = 1 + level.random.nextInt(3);
        int placed = 0;
        for (int attempt = 0; attempt < 24 && placed < wanted; attempt++) {
            int x = minX + level.random.nextInt(maxX - minX + 1);
            int z = minZ + level.random.nextInt(maxZ - minZ + 1);
            // getHeight returns the top solid block; stand on top of it
            int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                continue;
            }
            if (!level.getFluidState(pos).isEmpty()
                    || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            ToadEntity toad = ModEntities.TOAD.create(level);
            if (toad == null) {
                return;
            }
            toad.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            toad.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null);
            if (level.noCollision(toad)) {
                level.addFreshEntity(toad);
                placed++;
            } else {
            }
        }
        LOGGER.info("Placed {}/{} toads in village chunk {}", placed, wanted, chunk.getPos());
    }
}
