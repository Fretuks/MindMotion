package net.fretux.mindmotion.event;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.network.ModMessages;
import net.fretux.mindmotion.network.SyncStatsS2CPacket;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.fretux.mindmotion.client.effects.IronsSpellbooksCompatMM.applyTempoToManaRegen;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class PlayerTickHandler {

    private static final int SANITY_TICK_INTERVAL = 20;
    private static final int TEMPO_TICK_INTERVAL = 15;
    private static final SoundEvent[] FAKE_MOB_SOUNDS = {
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.SPIDER_AMBIENT,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.HUSK_AMBIENT,
            SoundEvents.WARDEN_ANGRY
    };
    private static final SoundEvent[] FAKE_BLOCK_BREAK_SOUNDS = {
            SoundEvents.STONE_BREAK,
            SoundEvents.DEEPSLATE_BREAK,
            SoundEvents.GRASS_BREAK,
            SoundEvents.GRAVEL_BREAK,
            SoundEvents.WOOD_BREAK,
            SoundEvents.GLASS_BREAK
    };
    private static final SoundEvent[] FAKE_FOOTSTEP_SOUNDS = {
            SoundEvents.STONE_STEP,
            SoundEvents.DEEPSLATE_STEP,
            SoundEvents.GRASS_STEP,
            SoundEvents.GRAVEL_STEP,
            SoundEvents.WOOD_STEP,
            SoundEvents.SAND_STEP
    };
    private static final Map<UUID, Integer> lastCombatTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> insanityDamageTicks = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lowSanitySoundCooldown = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(PlayerTickHandler.class);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (ModList.get().isLoaded("ascend")) {
            try {
                net.fretux.mindmotion.compat.AscendCompat.applyWillpowerBonuses(player);
            } catch (NoClassDefFoundError | Exception e) {
                log.error(String.valueOf(e));
            }
        }
        int tick = player.tickCount;
        boolean sanityTick = tick % SANITY_TICK_INTERVAL == 0;
        boolean tempoSyncTick = tick % TEMPO_TICK_INTERVAL == 0;
        boolean sanityEnabled = ConfigMM.COMMON.ENABLE_SANITY.get();
        boolean tempoEnabled = ConfigMM.COMMON.ENABLE_TEMPO.get();

        if (sanityEnabled && sanityTick) handleSanityAndInsanity(player);
        if (tempoEnabled) {
            handleTempoTick(player, tick);
        } else if (tick % TEMPO_TICK_INTERVAL == 0) {
            applyTempoToManaRegen(player, 0);
        }

        if (player instanceof ServerPlayer serverPlayer && (sanityTick || tempoSyncTick)) {
            player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity ->
                    player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo ->
                            ModMessages.CHANNEL.sendTo(
                                    new SyncStatsS2CPacket(
                                            sanityEnabled ? sanity.getSanity() : sanity.getMaxSanity(),
                                            sanityEnabled ? sanity.getInsanity() : 0f,
                                            tempoEnabled ? tempo.getTempo() : 0,
                                            tempoEnabled ? tempo.getVentCooldown() : 0,
                                            sanity.getMaxSanity(),
                                            tempo.getMaxTempo(),
                                            sanityEnabled,
                                            tempoEnabled
                                    ),
                                    serverPlayer.connection.connection,
                                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                            )
                    )
            );
        }
    }

    private static void handleSanityAndInsanity(Player player) {
        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> {
            float value = sanity.getSanity();
            float insanity = sanity.getInsanity();
            float delta = 0f;
            boolean inDark = player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
            boolean inRain = player.level().isRainingAt(player.blockPosition());
            boolean isHungry = player.getFoodData().getFoodLevel() < 6;
            boolean nearLava = player.level().getBlockState(player.blockPosition().below()).is(Blocks.LAVA);
            // Prevent false "exposure" sanity drain while riding seats like boats, minecarts, or modded chairs.
            boolean isMounted = player.isPassenger();
            boolean inWater = player.isInWater() && !player.isUnderWater();
            boolean underWater = player.isUnderWater();
            boolean sleeping = player.isSleeping();
            boolean inLight = player.level().getMaxLocalRawBrightness(player.blockPosition()) > 12;
            boolean nearTorch = player.level().getBlockState(player.blockPosition().above()).is(Blocks.TORCH);
            int light = player.level().getMaxLocalRawBrightness(player.blockPosition());

            if (inDark) delta -= ConfigMM.COMMON.BASE_SANITY_DECAY_DARK.get().floatValue();
            if (inRain) delta -= 0.04f;
            if (isHungry) delta -= 0.06f;
            if (nearLava) delta -= 0.05f;
            if (!isMounted && underWater) delta -= 0.04f;
            if (!isMounted && inWater && !underWater) delta -= 0.02f;
            if (light <= 2) delta -= 0.10f;
            else if (light <= 5) delta -= 0.05f;
            if (player.level().isThundering()) delta -= 0.06f;
            if (player.level().getBiome(player.blockPosition())
                    .is(BiomeTags.IS_END)) {
                delta -= 0.02f;
            }
            if (player.level().getBiome(player.blockPosition())
                    .is(BiomeTags.IS_NETHER)) {
                delta -= 0.015f;
            }
            if (player.level().dimension() == net.minecraft.world.level.Level.NETHER)
                delta -= 0.12f;
            if (player.level().dimension() == net.minecraft.world.level.Level.END)
                delta -= 0.08f;
            if (!player.level().getEntities(player, player.getBoundingBox().inflate(30),
                    e -> e.getType().toString().contains("warden")).isEmpty()) {
                delta -= 0.15f;
            }
            long hostileCount = player.level().getEntities(player,
                    player.getBoundingBox().inflate(10),
                    e -> e instanceof net.minecraft.world.entity.Mob && !(e instanceof net.minecraft.world.entity.animal.Animal)
            ).size();
            delta -= hostileCount * 0.005f;
            if (inLight) delta += ConfigMM.COMMON.BASE_SANITY_REGEN_LIGHT.get().floatValue();
            if (nearTorch) delta += 0.05f;
            if (sleeping) delta += 0.25f;
            if (player.getUseItem() != null && player.getUseItem().isEdible()) delta += 0.05f;
            if (player.level().getBlockState(player.blockPosition().below())
                    .is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) {
                delta += 0.08f;
            }
            if (player.level().getBlockState(player.blockPosition().below())
                    .is(Blocks.FURNACE) || (player.level().getBlockState(player.blockPosition().below())
                    .is(Blocks.BLAST_FURNACE)) || (player.level().getBlockState(player.blockPosition().below())
                    .is(Blocks.SMOKER))) {
                delta += 0.04f;
            }
            long animalCount = player.level().getEntities(player,
                    player.getBoundingBox().inflate(6),
                    e -> e instanceof net.minecraft.world.entity.animal.Animal
            ).size();
            delta += animalCount * 0.01f;
            long villagerCount = player.level().getEntities(player,
                    player.getBoundingBox().inflate(8),
                    e -> e instanceof net.minecraft.world.entity.npc.Villager
            ).size();
            delta += villagerCount * 0.02f;
            if (player.level().getBlockState(player.blockPosition().below()).is(Blocks.BEACON))
                delta += 0.15f;
            if (player.getMainHandItem().is(Blocks.POPPY.asItem())
                    || player.getMainHandItem().is(Blocks.DANDELION.asItem())) {
                delta += 0.04f;
            }
            if (player.isSleepingLongEnough())
                delta += 0.10f;

            delta += getSurvivalSanityDelta(player, light);
            delta += getStatusSanityDelta(player);
            delta += getNearbyBlockSanityDelta(player);
            delta += getWorldSanityDelta(player, light);

            boolean isInsane = insanity > 0;
            if (isInsane) {
                if (delta > 0) {
                    sanity.setInsanity(Math.max(0, insanity - delta));
                    return;
                }
                float newValue = value + delta;
                if (newValue <= 0 && delta < 0) {
                    float overflow = -newValue;
                    sanity.setSanity(0);
                    sanity.setInsanity(Math.min(sanity.getMaxSanity(), insanity + overflow));
                } else {
                    sanity.setSanity(newValue);
                }
                insanity = sanity.getInsanity();
                if (sanity.getSanity() <= 0 && insanity >= sanity.getMaxSanity()) {
                    UUID id = player.getUUID();
                    int ticks = insanityDamageTicks.getOrDefault(id, 0) + SANITY_TICK_INTERVAL;
                    insanityDamageTicks.put(id, ticks);
                    float damage = 1.0f + (ticks / 100f);
                    player.hurt(player.damageSources().magic(), damage);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    return;
                }
        } else {
                float newValueElse = value + delta;
                if (newValueElse <= 0 && delta < 0) {
                    float overflow = -newValueElse;
                    sanity.setSanity(0);
                    sanity.setInsanity(Math.min(sanity.getMaxSanity(), insanity + overflow));
                    insanity = sanity.getInsanity();
                } else if (newValueElse > 0) {
                    sanity.setSanity(newValueElse);
                    sanity.setInsanity(Math.max(0, insanity - 0.15f));
                    insanityDamageTicks.remove(player.getUUID());
                    insanity = sanity.getInsanity();
                }
            }
            float percent = (sanity.getSanity() / sanity.getMaxSanity()) * 100f;
            float insanityPercent = (insanity / sanity.getMaxSanity()) * 100f;
            if (ConfigMM.COMMON.ENABLE_SANITY_GAMEPLAY_EFFECTS.get()) {
                applySanityGameplayPressure(player, percent, insanityPercent);
            }
            if (percent <= 70 && Math.random() < 0.03) {
                applyShiver(player);
            }
            if (percent <= 10 && Math.random() < ConfigMM.COMMON.SCRATCHING_CHANCE.get()) {
                applyScratching(player);
            }
            if (percent <= 40 && Math.random() < 0.02) {
                applyVertigo(player);
            }
            if (percent <= 20 && Math.random() < 0.015) {
                applyDread(player);
            }
            if (percent <= 5 && Math.random() < 0.01) {
                applyBlackout(player);
            }
            if (percent <= 50 && Math.random() < ConfigMM.COMMON.PANIC_CHANCE.get()) {
                applyPanic(player);
            }
            if (insanityPercent > 0) {
                if (insanityPercent > 60 && Math.random() < ConfigMM.COMMON.PANIC_CHANCE.get()) {
                    applyPanic(player);
                }
                if (insanityPercent > 90 && Math.random() < ConfigMM.COMMON.SCRATCHING_CHANCE.get()) {
                    applyScratching(player);
                }
                if (insanityPercent > 50 && Math.random() < 0.03) {
                    applyVertigo(player);
                }
                if (insanityPercent > 75 && Math.random() < 0.02) {
                    applyDread(player);
                }
                if (insanityPercent > 90 && Math.random() < 0.015) {
                    applyBlackout(player);
                }
            }
            if (ConfigMM.COMMON.ENABLE_LOW_SANITY_SOUNDS.get() && (percent <= 30f || insanityPercent >= 35f)) {
                UUID id = player.getUUID();
                int cd = lowSanitySoundCooldown.getOrDefault(id, 0) - SANITY_TICK_INTERVAL;
                if (cd <= 0) {
                    playSanitySound(player, percent, insanityPercent);
                    int baseSeconds = insanityPercent >= 60f || percent <= 10f ? 5 : 6;
                    cd = 20 * (baseSeconds + player.getRandom().nextInt(15));
                }
                lowSanitySoundCooldown.put(id, cd);
            } else {
                lowSanitySoundCooldown.remove(player.getUUID());
            }
        });
    }

    private static float getSurvivalSanityDelta(Player player, int light) {
        float delta = 0f;
        float healthPercent = player.getHealth() / player.getMaxHealth();
        int food = player.getFoodData().getFoodLevel();

        if (healthPercent <= 0.25f) delta -= 0.12f;
        else if (healthPercent <= 0.50f) delta -= 0.06f;

        if (player.isOnFire()) delta -= 0.12f;
        if (player.isFreezing()) delta -= 0.08f;
        if (player.getAirSupply() < player.getMaxAirSupply() / 2) delta -= 0.08f;
        if (player.fallDistance > 6f) delta -= 0.06f;
        if (player.isSprinting() && food <= 8) delta -= 0.03f;

        if (healthPercent >= 0.95f && food >= 18 && light >= 10) delta += 0.04f;
        if (player.isShiftKeyDown() && !player.isSprinting() && !player.isInWater() && light >= 8) delta += 0.015f;
        if (player.getAbsorptionAmount() > 0f) delta += 0.03f;

        return delta;
    }

    private static float getStatusSanityDelta(Player player) {
        float delta = 0f;

        if (player.hasEffect(MobEffects.POISON)) delta -= 0.08f;
        if (player.hasEffect(MobEffects.WITHER)) delta -= 0.12f;
        if (player.hasEffect(MobEffects.HUNGER)) delta -= 0.06f;
        if (player.hasEffect(MobEffects.WEAKNESS)) delta -= 0.04f;
        if (player.hasEffect(MobEffects.BLINDNESS)) delta -= 0.08f;
        if (player.hasEffect(MobEffects.DARKNESS)) delta -= 0.12f;
        if (player.hasEffect(MobEffects.CONFUSION)) delta -= 0.05f;
        if (player.hasEffect(MobEffects.BAD_OMEN)) delta -= 0.04f;

        if (player.hasEffect(MobEffects.REGENERATION)) delta += 0.06f;
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) delta += 0.04f;
        if (player.hasEffect(MobEffects.ABSORPTION)) delta += 0.04f;
        if (player.hasEffect(MobEffects.SATURATION)) delta += 0.05f;
        if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) delta += 0.08f;

        return delta;
    }

    private static float getNearbyBlockSanityDelta(Player player) {
        int comfortBlocks = 0;
        int unsettlingBlocks = 0;

        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 3, 4))) {
            var state = player.level().getBlockState(pos);

            if (state.is(BlockTags.BEDS)
                    || state.is(BlockTags.CANDLES)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(Blocks.BOOKSHELF)
                    || state.is(Blocks.CHISELED_BOOKSHELF)
                    || state.is(Blocks.CAMPFIRE)
                    || state.is(Blocks.LANTERN)
                    || state.is(Blocks.SEA_LANTERN)) {
                comfortBlocks++;
            }

            if (state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.SOUL_LANTERN)
                    || state.is(Blocks.SCULK)
                    || state.is(Blocks.SCULK_SENSOR)
                    || state.is(Blocks.SCULK_SHRIEKER)
                    || state.is(Blocks.SCULK_CATALYST)
                    || state.is(Blocks.COBWEB)
                    || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.NETHER_PORTAL)) {
                unsettlingBlocks++;
            }
        }

        float comfort = Math.min(0.10f, comfortBlocks * 0.008f);
        float unsettling = Math.min(0.16f, unsettlingBlocks * 0.012f);
        return comfort - unsettling;
    }

    private static float getWorldSanityDelta(Player player, int light) {
        float delta = 0f;

        if (player.level().canSeeSky(player.blockPosition()) && player.level().isDay() && light >= 12 && !player.level().isRaining()) {
            delta += 0.05f;
        }
        if (!player.level().canSeeSky(player.blockPosition()) && player.getY() < player.level().getSeaLevel() - 24) {
            delta -= 0.06f;
        }
        if (player.level().getMoonBrightness() > 0.85f && !player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
            delta -= 0.03f;
        }
        if (player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_OCEAN) && !player.onGround()) {
            delta -= 0.03f;
        }

        int lastCombat = lastCombatTick.getOrDefault(player.getUUID(), -10000);
        int ticksSinceCombat = player.tickCount - lastCombat;
        if (ticksSinceCombat <= 80) delta -= 0.04f;
        else if (ticksSinceCombat >= 20 * 60 && player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) delta += 0.025f;

        return delta;
    }

    private static void applySanityGameplayPressure(Player player, float sanityPercent, float insanityPercent) {
        float lowSanity = clamp01((55f - sanityPercent) / 55f);
        float highInsanity = clamp01((insanityPercent - 35f) / 65f);
        float pressure = Math.max(lowSanity, highInsanity);
        if (pressure <= 0f) return;

        if (!player.getAbilities().instabuild) {
            player.causeFoodExhaustion(0.08f + pressure * 0.18f);
        }

        if (sanityPercent <= 35f) {
            int amplifier = sanityPercent <= 15f ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, amplifier, true, true));
        }

        if (player.isUsingItem() && (sanityPercent <= 25f || insanityPercent >= 70f)) {
            float interruptChance = 0.06f + pressure * 0.12f;
            if (player.getRandom().nextFloat() < interruptChance) {
                player.stopUsingItem();
            }
        }

        if ((sanityPercent <= 15f || insanityPercent >= 80f) && player.getRandom().nextFloat() < 0.08f + pressure * 0.10f) {
            applyStumble(player, pressure);
        }

        if (insanityPercent >= 45f) {
            int tempoLoss = Math.max(1, Math.round(highInsanity * 4f));
            player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.reduceTempo(tempoLoss));
        }

        if (insanityPercent >= 60f && player.getRandom().nextFloat() < 0.18f + highInsanity * 0.20f) {
            alertNearbyMonsters(player, 10.0 + highInsanity * 10.0);
        }
    }

    private static void applyStumble(Player player, float pressure) {
        double strength = 0.08 + pressure * 0.16;
        double x = (player.getRandom().nextDouble() - 0.5) * strength;
        double z = (player.getRandom().nextDouble() - 0.5) * strength;
        player.setDeltaMovement(player.getDeltaMovement().add(x, 0.0, z));
        player.hurtMarked = true;
        if (player.isSprinting() && player.getRandom().nextFloat() < 0.5f) {
            player.setSprinting(false);
        }
    }

    private static void alertNearbyMonsters(Player player, double radius) {
        player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius),
                monster -> monster.isAlive() && monster.hasLineOfSight(player)
        ).forEach(monster -> {
            if (monster.getTarget() == null || monster.getRandom().nextFloat() < 0.35f) {
                monster.setTarget(player);
            }
            monster.getNavigation().moveTo(player, 1.1);
        });
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void handleTempoTick(Player player, int currentTick) {
        player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> {
            int cd = tempo.getVentCooldown();
            if (cd > 0) {
                tempo.setVentCooldown(cd - 1);
            }
            if (currentTick % TEMPO_TICK_INTERVAL == 0) {
                int value = tempo.getTempo();
                int lastCombat = lastCombatTick.getOrDefault(player.getUUID(), -10000);
                int ticksSinceCombat = currentTick - lastCombat;
                if (ticksSinceCombat > ConfigMM.COMMON.TEMPO_DECAY_DELAY.get()) {
                    tempo.setTempo(value - 1);
                }
                applyTempoToManaRegen(player, tempo.getTempo());
            }
        });
    }

    public static void markCombat(Player player) {
        lastCombatTick.put(player.getUUID(), player.tickCount);
    }

    private static void applyShiver(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
    }

    private static void applyPanic(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        player.hurt(player.damageSources().magic(), 1f);
        applyShiver(player);
        applyVertigo(player);
    }

    private static void applyScratching(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        player.hurt(player.damageSources().magic(), 2f);
        applyShiver(player);
        applyDread(player);
    }

    private static void applyVertigo(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
    }

    private static void applyDread(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
    }

    private static void applyBlackout(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
    }

    private static void playSanitySound(Player player, float sanityPercent, float insanityPercent) {
        var level = player.level();
        if (level.isClientSide) return;

        float pressure = Math.max(clamp01((30f - sanityPercent) / 30f), clamp01(insanityPercent / 100f));
        int roll = player.getRandom().nextInt(100);
        if (roll < 45) {
            SoundEvent chosen = pickSound(player, FAKE_MOB_SOUNDS);
            playFakeSound(player, chosen, 4.0, 7.0, 0.0, 1.0f, 0.9f, 0.2f);
        } else if (roll < 72) {
            SoundEvent chosen = pickSound(player, FAKE_BLOCK_BREAK_SOUNDS);
            playFakeSound(player, chosen, 2.5, 6.0, -0.5, 0.85f + pressure * 0.25f, 0.85f, 0.25f);
        } else {
            SoundEvent chosen = pickSound(player, FAKE_FOOTSTEP_SOUNDS);
            int steps = player.getRandom().nextFloat() < 0.35f + pressure * 0.35f ? 2 : 1;
            for (int i = 0; i < steps; i++) {
                playFakeSound(player, chosen, 1.8 + i * 0.8, 4.2 + i * 0.8, -0.9, 0.65f, 0.8f, 0.35f);
            }
        }
    }

    private static SoundEvent pickSound(Player player, SoundEvent[] sounds) {
        return sounds[player.getRandom().nextInt(sounds.length)];
    }

    private static void playFakeSound(Player player, SoundEvent sound, double minDistance, double maxDistance,
                                      double yOffset, float volume, float minPitch, float pitchRange) {
        if (!(player instanceof ServerPlayer sp)) return;

        var holder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        float yaw = player.getYRot();
        double angleRad = Math.toRadians(yaw + 180 + player.getRandom().nextInt(90) - 45);
        double dist = minDistance + player.getRandom().nextDouble() * (maxDistance - minDistance);
        double dx = player.getX() + Math.cos(angleRad) * dist;
        double dz = player.getZ() + Math.sin(angleRad) * dist;
        double dy = player.getY() + yOffset;
        sp.connection.send(
                new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        holder,
                        SoundSource.AMBIENT,
                        dx, dy, dz,
                        volume,
                        minPitch + player.getRandom().nextFloat() * pitchRange,
                        player.getRandom().nextLong()
                )
        );
    }
}
