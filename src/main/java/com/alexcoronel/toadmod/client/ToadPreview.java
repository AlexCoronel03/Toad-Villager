package com.alexcoronel.toadmod.client;

import com.alexcoronel.toadmod.ToadCapColor;
import com.alexcoronel.toadmod.entity.ToadEntity;
import com.alexcoronel.toadmod.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;

/**
 * Development-only visual verification harness. Completely inert unless the JVM is started with
 * {@code -Dtoadmod.preview=true} (see the {@code toadPreview} Gradle property in build.gradle.kts).
 *
 * It builds a small flat stage, spawns one toad per cap colour, takes screenshots from the front, side,
 * three-quarter and back, opens the vanilla trading screen, logs an NBT save/load round trip and then quits.
 */
final class ToadPreview {
    private static final String PREFIX = "[ToadPreview] ";

    private final Minecraft mc = Minecraft.getInstance();
    private final List<ToadEntity> toads = new ArrayList<>();
    private int ticks;
    private BlockPos origin;
    private int step;
    private int stepWait;
    private int titleTicks;
    private int frameIdx;
    private String pendingShot;
    private ToadEntity walker;
    private net.minecraft.world.entity.monster.Zombie zombie;
    private net.minecraft.world.phys.Vec3 walkerStart;
    private BlockPos villagePos;
    private boolean villageReady;
    private long spawnT0 = -1;
    private int toadsAtStart;
    private int spawnPolls;
    private volatile boolean spawnDone;
    private long lastLoggedWindow = -1;
    private static final long SPAWN_WINDOW_TICKS = 400;
    private int villageWaits;
    private volatile boolean jobDone;
    private int jobPolls;
    private volatile boolean lossDone;
    private int lossPolls;
    private boolean worldRequested;

    static void init() {
        if (!Boolean.getBoolean("toadmod.preview")) {
            return;
        }
        ToadPreview preview = new ToadPreview();
        if ("village".equals(System.getProperty("toadmod.previewMode"))) {
            preview.step = 20; // skip straight to the village spawn test
        }
        if ("job".equals(System.getProperty("toadmod.previewMode"))) {
            preview.step = 30; // skip straight to the job-site test
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> preview.tick());

    }

    private void log(String message) {
        System.out.println(PREFIX + message);
    }

    private boolean soundListenerAdded;

    private void tick() {
        if (!soundListenerAdded && mc.getSoundManager() != null) {
            soundListenerAdded = true;
            mc.getSoundManager().addListener((sound, accessor, range) -> {
                String id = sound.getLocation().toString();
                if (id.startsWith("toadmod:") || id.contains("villager")) {
                    System.out.println(PREFIX + "SOUND played: " + id);
                }
            });
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen && !worldRequested) {
            if (++titleTicks > 60) {
                worldRequested = true;
                log("creating test world");
                net.minecraft.world.level.LevelSettings settings = new net.minecraft.world.level.LevelSettings(
                        "ToadPreview", GameType.CREATIVE, false, net.minecraft.world.Difficulty.PEACEFUL, true,
                        new GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("ToadPreview" + System.currentTimeMillis(), settings,
                        new net.minecraft.world.level.levelgen.WorldOptions(1234L, true, false),
                        net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions, mc.screen);
            }
            return;
        }
        if (mc.level == null || mc.player == null || server == null) {
            return;
        }
        ticks++;
        mc.options.pauseOnLostFocus = false;
        if (ticks == 80) {
            server.execute(() -> buildStage(server));
            return;
        }
        if (ticks < 140 || origin == null) {
            return;
        }
        if (stepWait > 0) {
            stepWait--;
            if (stepWait == 0 && pendingShot != null) {
                shot(pendingShot);
                pendingShot = null;
                stepWait = 1;
            }
            return;
        }
        runStep(server);
    }

    private void buildStage(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);

        // A raised, open-sky platform: the spawn point can be underground or in water.
        BlockPos o = new BlockPos(player.blockPosition().getX(), 160, player.blockPosition().getZ());
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -4; dz <= 14; dz++) {
                level.setBlockAndUpdate(o.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) {
                    level.setBlockAndUpdate(o.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        ToadCapColor[] colors = ToadCapColor.values();
        for (int i = 0; i < colors.length; i++) {
            ToadEntity toad = ModEntities.TOAD.create(level);
            toad.setCapColor(colors[i]);
            toad.moveTo(o.getX() + 0.5 + (i - 2.5) * 1.5, o.getY(), o.getZ() + 6.5, 180.0F, 0.0F);
            toad.setYBodyRot(180.0F);
            toad.setYHeadRot(180.0F);
            toad.setNoAi(true);
            toad.finalizeSpawn(level, level.getCurrentDifficultyAt(toad.blockPosition()), MobSpawnType.COMMAND, null);
            toad.setCapColor(colors[i]);
            toad.setVillagerData(toad.getVillagerData().setProfession(
                    net.minecraft.world.entity.npc.VillagerProfession.FARMER));
            level.addFreshEntity(toad);
            toads.add(toad);
        }

        // NBT round trip: the cap colour must survive save + load.
        boolean roundTripOk = true;
        for (ToadEntity toad : toads) {
            CompoundTag tag = new CompoundTag();
            toad.saveWithoutId(tag);
            ToadEntity copy = ModEntities.TOAD.create(level);
            copy.load(tag);
            roundTripOk &= copy.getCapColor() == toad.getCapColor();
        }
        log("NBT round trip cap colour preserved for all six: " + roundTripOk);
        log("Toad profession: " + toads.get(0).getVillagerData().getProfession()
                + ", offers: " + toads.get(0).getOffers().size());

        player.teleportTo(level, o.getX() + 0.5, (double) o.getY(), o.getZ() + 0.5, java.util.Set.of(), 0.0F, 6.0F);
        this.origin = o;
        mc.execute(() -> mc.options.hideGui = true);
        log("stage built at " + o);
    }

    private void shot(String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> log("saved " + name));
    }

    private void facing(MinecraftServer server, ToadEntity toad, float yaw) {
        server.execute(() -> {
            toad.setYRot(yaw);
            toad.setYBodyRot(yaw);
            toad.setYHeadRot(yaw);
            toad.yRotO = yaw;
        });
    }

    private void moveCamera(MinecraftServer server, double x, double z, float pitch) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        server.execute(() -> player.teleportTo(player.serverLevel(), x, (double) origin.getY(), z,
                java.util.Set.of(), 0.0F, pitch));
    }

    /** Completes a real trade through the vanilla MerchantMenu and checks usage, XP and persistence. */
    private void tradeTest(MinecraftServer server, ToadEntity toad) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (!(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu)) {
            log("TRADE FAIL: merchant menu is not open (" + player.containerMenu + ")");
            return;
        }
        var offers = toad.getOffers();
        log("offers shown: " + offers.size());
        int xpBefore = toad.getVillagerXp();
        for (int i = 0; i < offers.size(); i++) {
            net.minecraft.world.item.trading.MerchantOffer offer = offers.get(i);
            player.getInventory().clearContent();
            net.minecraft.world.item.ItemStack costA = offer.getCostA().copy();
            String costText = costA.getCount() + "x " + costA.getItem();
            net.minecraft.world.item.Item costItem = costA.getItem();
            player.getInventory().add(costA);
            int costHeld = player.getInventory().countItem(costItem);
            int usesBefore = offer.getUses();
            menu.setSelectionHint(i);
            menu.tryMoveItems(i);
            net.minecraft.world.item.ItemStack result = menu.getSlot(2).getItem().copy();
            if (result.isEmpty()) {
                log("TRADE offer " + i + " FAIL: no result for cost " + costText);
                continue;
            }
            menu.clicked(2, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            boolean received = player.getInventory().countItem(result.getItem()) >= result.getCount();
            boolean consumed = player.getInventory().countItem(costItem) < costHeld
                    || costItem == result.getItem();
            log("TRADE offer " + i + ": " + costText + " -> " + result.getCount() + "x " + result.getItem()
                    + " received=" + received + " costConsumed=" + consumed
                    + " uses " + usesBefore + "->" + offer.getUses() + " maxUses=" + offer.getMaxUses());
        }
        log("villager xp " + xpBefore + "->" + toad.getVillagerXp() + " level=" + toad.getVillagerData().getLevel());

        // Offers (with usage counts) must survive save + load.
        CompoundTag tag = new CompoundTag();
        toad.saveWithoutId(tag);
        ToadEntity copy = ModEntities.TOAD.create(toad.level());
        copy.load(tag);
        boolean same = copy.getOffers().size() == toad.getOffers().size();
        for (int i = 0; same && i < toad.getOffers().size(); i++) {
            same = copy.getOffers().get(i).getUses() == toad.getOffers().get(i).getUses();
        }
        log("offers + uses persisted through NBT: " + same + " profession=" + copy.getVillagerData().getProfession());
        player.closeContainer();

        // Exercise the hurt and death sound events.
        toad.hurt(toad.damageSources().generic(), 1.0F);
        toads.get(1).kill();
        log("hurt + kill issued for sound check");
    }


    private void spawnWalker(MinecraftServer server) {
        ServerLevel level = server.getPlayerList().getPlayers().get(0).serverLevel();
        for (ToadEntity t : toads) {
            t.discard(); // clear the line-up so only the walker is on stage
        }
        toads.clear();
        walker = ModEntities.TOAD.create(level);
        walker.setCapColor(ToadCapColor.BLUE);
        walker.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 4.5, 0.0F, 0.0F);
        walker.finalizeSpawn(level, level.getCurrentDifficultyAt(walker.blockPosition()), MobSpawnType.COMMAND, null);
        walker.setCapColor(ToadCapColor.BLUE);
        level.addFreshEntity(walker);
        toads.add(walker);
        log("walker spawned with AI");
    }

    private void animFrame(MinecraftServer server, String name, String label) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.teleportTo(player.serverLevel(), walker.getX(), origin.getY() + 1.6, walker.getZ() - 4.0,
                    java.util.Set.of(), 0.0F, 8.0F);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    walker.position().add(0, 0.7, 0));
            log(label + " frame " + name + " fleeing=" + walker.isFleeing() + " pos="
                    + String.format("(%.1f, %.1f)", walker.getX(), walker.getZ()));
        });
        stepWait = 3;
        pendingShot = name;
    }

    private void spawnTest(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        if (villagePos == null) {
            log("SPAWN TEST SKIPPED: no village found within range");
            return;
        }
        net.minecraft.world.level.ChunkPos centre = new net.minecraft.world.level.ChunkPos(villagePos);
        int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                villagePos.getX(), villagePos.getZ());
        log("village centre isVillage=" + level.isVillage(new BlockPos(villagePos.getX(), groundY, villagePos.getZ()))
                + " biome=" + level.getBiome(new BlockPos(villagePos.getX(), groundY, villagePos.getZ())).unwrapKey().map(k -> k.location().toString()).orElse("?"));
        // A clean spawn state (no existing mobs counted) so the global mob cap cannot mask the test.
        var spawnState = net.minecraft.world.level.NaturalSpawner.createState(289, java.util.List.<net.minecraft.world.entity.Entity>of(),
                (chunkPos, consumer) -> {
                    var c = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkPos),
                            net.minecraft.world.level.ChunkPos.getZ(chunkPos));
                    if (c != null) {
                        consumer.accept(c);
                    }
                },
                new net.minecraft.world.level.LocalMobCapCalculator(level.getChunkSource().chunkMap));
        // --- diagnostics ---
        var biome = level.getBiome(new BlockPos(villagePos.getX(), groundY, villagePos.getZ()));
        var pool = level.getChunkSource().getGenerator().getMobsAt(biome, level.structureManager(),
                net.minecraft.world.entity.MobCategory.CREATURE, new BlockPos(villagePos.getX(), groundY, villagePos.getZ()));
        StringBuilder names = new StringBuilder();
        pool.unwrap().forEach(d -> names.append(d.type.builtInRegistryHolder().key().location().getPath())
                .append("(w").append(d.getWeight()).append(") "));
        log("CREATURE spawn pool at village: " + names);
        int pass = 0;
        int tried = 0;
        for (int dx = -40; dx <= 40; dx += 8) {
            for (int dz = -40; dz <= 40; dz += 8) {
                int x = villagePos.getX() + dx;
                int z = villagePos.getZ() + dz;
                BlockPos g = new BlockPos(x, level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                tried++;
                if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(ModEntities.TOAD, level,
                        MobSpawnType.NATURAL, g, level.random)) {
                    pass++;
                }
            }
        }
        log("spawn rule NATURAL passes at " + pass + "/" + tried + " sampled positions around the village");
        var all = level.getAllEntities();
        int creaturesBefore = 0;
        for (var e : all) {
            if (e.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE) {
                creaturesBefore++;
            }
        }
        spawnT0 = level.getGameTime();
        toadsAtStart = level.getEntitiesOfClass(ToadEntity.class,
                new net.minecraft.world.phys.AABB(villagePos).inflate(128)).size();
        log("real-time natural spawning window started at tick " + spawnT0 + " (toads present: " + toadsAtStart + ")");
    }

    private int runSpawner(ServerLevel level, net.minecraft.world.level.ChunkPos centre, int radius,
                           net.minecraft.world.level.NaturalSpawner.SpawnState state, int attempts) {
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                centre.getMinBlockX() - radius * 16, level.getMinBuildHeight(), centre.getMinBlockZ() - radius * 16,
                centre.getMaxBlockX() + radius * 16, level.getMaxBuildHeight(), centre.getMaxBlockZ() + radius * 16);
        int before = level.getEntitiesOfClass(ToadEntity.class, area).size();
        for (int i = 0; i < attempts; i++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk =
                            level.getChunkSource().getChunkNow(centre.x + dx, centre.z + dz);
                    if (chunk != null) {
                        net.minecraft.world.level.NaturalSpawner.spawnForChunk(level, chunk, state, true, false, false);
                    }
                }
            }
        }
        return level.getEntitiesOfClass(ToadEntity.class, area).size() - before;
    }

    private void runStep(MinecraftServer server) {
        ToadEntity focus = toads.get(0);
        double fx = focus.getX();
        double fz = focus.getZ();
        stepWait = 12;
        switch (step++) {
            case 0 -> shot("01_group_front");
            case 1 -> {
                moveCamera(server, fx, fz - 2.8, 8.0F);
                stepWait = 25;
            }
            case 2 -> shot("02_closeup_front");
            case 3 -> facing(server, focus, 135.0F); // three-quarter
            case 4 -> shot("03_closeup_three_quarter");
            case 5 -> facing(server, focus, 90.0F); // side
            case 6 -> shot("04_closeup_side");
            case 7 -> facing(server, focus, 0.0F); // back
            case 8 -> shot("05_closeup_back");
            case 9 -> {
                facing(server, focus, 180.0F);
                // high three-quarter view of the cap, like the design reference
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                server.execute(() -> {
                    player.teleportTo(player.serverLevel(), focus.getX() + 0.9, origin.getY() + 1.7, focus.getZ() - 1.5,
                            java.util.Set.of(), 25.0F, 42.0F);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                });
                stepWait = 25;
            }
            case 10 -> shot("06_closeup_yellow");
            case 11 -> {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    focus.setTradingPlayer(player);
                    focus.openTradingScreen(player, focus.getDisplayName(), focus.getVillagerData().getLevel());
                });
                stepWait = 30;
            }
            case 12 -> shot("07_trading_screen");
            case 13 -> server.execute(() -> tradeTest(server, focus));
            // ---- animations with a real AI-driven toad ----
            case 14 -> {
                server.execute(() -> spawnWalker(server));
                stepWait = 30;
            }
            case 15 -> {
                server.execute(() -> {
                    walkerStart = walker.position();
                    BlockPos target = origin.offset(7, 0, 11);
                    walker.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                            new net.minecraft.world.entity.ai.memory.WalkTarget(target, 0.6F, 1));
                    log("walker start " + walkerStart);
                });
                stepWait = 6;
            }
            case 16 -> {
                if (frameIdx < 8) {
                    animFrame(server, "anim_walk_" + frameIdx, "walk");
                    frameIdx++;
                    step--;
                    stepWait = 5;
                } else {
                    frameIdx = 0;
                    server.execute(() -> log("walker moved " + String.format("%.2f", walker.position().distanceTo(walkerStart))
                            + " blocks while walking"));
                }
            }
            case 17 -> server.execute(() -> {
                server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
                net.minecraft.world.entity.monster.Zombie z =
                        net.minecraft.world.entity.EntityType.ZOMBIE.create(((ServerLevel) walker.level()));
                z.moveTo(walker.getX() + 3.0, walker.getY(), walker.getZ(), 90.0F, 0.0F);
                z.setNoAi(true);
                z.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 100000, 0, false, false));
                z.setPersistenceRequired();
                ((ServerLevel) walker.level()).addFreshEntity(z);
                this.zombie = z;
                walkerStart = walker.position();
                log("zombie spawned next to the walker");
            });
            case 18 -> {
                if (frameIdx < 10) {
                    animFrame(server, "anim_flee_" + frameIdx, "flee");
                    frameIdx++;
                    step--;
                    stepWait = 5;
                } else {
                    frameIdx = 0;
                    server.execute(() -> {
                        log("walker fled " + String.format("%.2f", walker.position().distanceTo(walkerStart))
                                + " blocks");
                        zombie.discard();
                        walker.hurt(walker.damageSources().generic(), 1.0F);
                    });
                    stepWait = 2;
                }
            }
            case 19 -> animFrame(server, "anim_hurt", "hurt");

            // ---- natural spawning in a real village ----
            case 20 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ServerLevel level = player.serverLevel();
                villagePos = level.findNearestMapStructure(net.minecraft.tags.StructureTags.VILLAGE,
                        player.blockPosition(), 100, false);
                log("nearest village: " + villagePos);
                if (villagePos != null) {
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                            villagePos.getX(), villagePos.getZ());
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, villagePos.getX() + 0.5, y + 3.0, villagePos.getZ() + 0.5,
                            java.util.Set.of(), 0.0F, 30.0F);
                }
            });
            case 21 -> {
                if (villagePos != null && !villageReady && villageWaits++ < 200) {
                    server.execute(() -> {
                        net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(villagePos);
                        ServerLevel level = server.getPlayerList().getPlayers().get(0).serverLevel();
                        boolean all = true;
                        for (int dx = -3; dx <= 3 && all; dx++) {
                            for (int dz = -3; dz <= 3 && all; dz++) {
                                all = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz) != null;
                            }
                        }
                        villageReady = all;
                    });
                    step--;
                    stepWait = 20;
                } else {
                    stepWait = 200; // let the village finish generating and settle
                }
            }
            case 22 -> server.execute(() -> spawnTest(server));
            // ---- job site: an unemployed toad must take the profession of a placed workstation ----
            case 30 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().get(0).serverLevel();
                for (ToadEntity t : toads) {
                    t.discard();
                }
                toads.clear();
                ToadEntity t = ModEntities.TOAD.create(level);
                t.setCapColor(ToadCapColor.GREEN);
                t.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 6.5, 0.0F, 0.0F);
                t.finalizeSpawn(level, level.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.COMMAND, null);
                t.setCapColor(ToadCapColor.GREEN);
                level.addFreshEntity(t);
                walker = t;
                toads.add(t);
                level.setBlockAndUpdate(origin.offset(4, 0, 6), net.minecraft.world.level.block.Blocks.COMPOSTER.defaultBlockState());
                net.minecraft.world.InteractionResult poke = t.mobInteract(
                        server.getPlayerList().getPlayers().get(0), net.minecraft.world.InteractionHand.MAIN_HAND);
                log("JOB: interacted with unemployed toad -> " + poke);
                log("JOB: spawned toad profession=" + t.getVillagerData().getProfession()
                        + " offers=" + t.getOffers().size() + ", composter placed 4 blocks away");
            });
            case 31 -> {
                server.execute(() -> {
                    var prof = walker.getVillagerData().getProfession();
                    boolean employed = prof != net.minecraft.world.entity.npc.VillagerProfession.NONE;
                    if (employed || jobPolls % 10 == 0) {
                        log("JOB: after " + (jobPolls * 100) + " client ticks profession=" + prof
                                + " level=" + walker.getVillagerData().getLevel()
                                + " offers=" + walker.getOffers().size());
                    }
                    jobDone = employed;
                });
                if (!jobDone && jobPolls++ < 120) {
                    step--;
                    stepWait = 100;
                } else {
                    stepWait = 20;
                }
            }
            case 32 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().get(0).serverLevel();
                log("JOB-LOSS: breaking the workstation; toad profession=" + walker.getVillagerData().getProfession()
                        + " xp=" + walker.getVillagerXp() + " level=" + walker.getVillagerData().getLevel()
                        + " jobSiteMemory=" + walker.getBrain().getMemory(
                                net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).isPresent());
                level.destroyBlock(origin.offset(4, 0, 6), false);
            });
            case 33 -> {
                server.execute(() -> {
                    var prof = walker.getVillagerData().getProfession();
                    boolean unemployed = prof == net.minecraft.world.entity.npc.VillagerProfession.NONE;
                    if (unemployed || lossPolls % 10 == 0) {
                        log("JOB-LOSS: after " + (lossPolls * 100) + " client ticks profession=" + prof
                                + " jobSiteMemory=" + walker.getBrain().getMemory(
                                        net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).isPresent());
                    }
                    lossDone = unemployed;
                });
                if (!lossDone && lossPolls++ < 60) {
                    step--;
                    stepWait = 100;
                } else {
                    stepWait = 20;
                }
            }
            case 34 -> {
                log("done, quitting");
                mc.stop();
            }
            case 23 -> {
                if (spawnT0 >= 0 && !spawnDone && spawnPolls++ < 5000) {
                    server.execute(() -> {
                        ServerLevel level = server.getPlayerList().getPlayers().get(0).serverLevel();
                        long elapsed = level.getGameTime() - spawnT0;
                        java.util.List<ToadEntity> found = level.getEntitiesOfClass(ToadEntity.class,
                                new net.minecraft.world.phys.AABB(villagePos).inflate(128));
                        if (elapsed / 100 > lastLoggedWindow || elapsed >= SPAWN_WINDOW_TICKS) {
                            lastLoggedWindow = elapsed / 100;
                            StringBuilder sb = new StringBuilder();
                            int villageHits = 0;
                            for (ToadEntity t : found) {
                                sb.append(t.getCapColor().getSerializedName()).append(' ');
                                if (level.isVillage(t.blockPosition())) {
                                    villageHits++;
                                }
                            }
                            log("natural spawn after " + elapsed + " ticks: toads=" + found.size()
                                    + " (started with " + toadsAtStart + "), colours: " + sb
                                    + "| standing in village: " + villageHits);
                        }
                        spawnDone = elapsed >= SPAWN_WINDOW_TICKS;
                    });
                    step--;
                    stepWait = 60;
                } else {
                    stepWait = 5;
                }
            }
            case 24 -> {
                log("done, quitting");
                mc.stop();
            }
            default -> {
            }
        }
    }
}
