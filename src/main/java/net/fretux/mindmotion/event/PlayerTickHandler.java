package net.fretux.mindmotion.event;

import net.fretux.mindmotion.compat.AscendCompat;
import net.fretux.mindmotion.network.ModMessages;
import net.fretux.mindmotion.network.SyncStatsS2CPacket;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.fretux.mindmotion.client.effects.IronsSpellbooksCompatMM.applyTempoToManaRegen;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class PlayerTickHandler {

    private static final int SANITY_TICK_INTERVAL = 20;
    private static final int TEMPO_TICK_INTERVAL = 15;
    private static final int COMBAT_DECAY_DELAY = 100;
    private static final Map<UUID, Integer> lastCombatTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> insanityDamageTicks = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lowSanitySoundCooldown = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        AscendCompat.applyWillpowerBonuses(player);
        int tick = player.tickCount;
        boolean sanityTick = tick % SANITY_TICK_INTERVAL == 0;
        boolean tempoSyncTick = tick % TEMPO_TICK_INTERVAL == 0;

        if (sanityTick) handleSanityAndInsanity(player);
        handleTempoTick(player, tick);

        if (player instanceof ServerPlayer serverPlayer && (sanityTick || tempoSyncTick)) {
            player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity ->
                    player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo ->
                            ModMessages.CHANNEL.sendTo(
                                    new SyncStatsS2CPacket(
                                            sanity.getSanity(),
                                            sanity.getInsanity(),
                                            tempo.getTempo(),
                                            tempo.getVentCooldown(),
                                            sanity.getMaxSanity(),
                                            tempo.getMaxTempo()
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
            boolean inWater = player.isInWater() && !player.isUnderWater();
            boolean underWater = player.isUnderWater();
            boolean sleeping = player.isSleeping();
            boolean inLight = player.level().getMaxLocalRawBrightness(player.blockPosition()) > 12;
            boolean nearTorch = player.level().getBlockState(player.blockPosition().above()).is(Blocks.TORCH);
            if (inDark) delta -= 0.02f;
            if (inRain) delta -= 0.04f;
            if (isHungry) delta -= 0.06f;
            if (nearLava) delta -= 0.05f;
            if (underWater) delta -= 0.04f;
            if (inWater && !underWater) delta -= 0.02f;
            int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
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
            if (inLight) delta += 0.03f;
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
            sanity.setSanity(value + delta);
            if (sanity.getSanity() > 0) {
                sanity.setInsanity(Math.max(0, insanity - 0.15f));
                insanityDamageTicks.remove(player.getUUID());
            } else {
                sanity.setInsanity(Math.min(sanity.getMaxSanity(), insanity + 0.3f));
                if (sanity.getInsanity() >= sanity.getMaxSanity()) {
                    UUID id = player.getUUID();
                    int ticks = insanityDamageTicks.getOrDefault(id, 0) + SANITY_TICK_INTERVAL;
                    insanityDamageTicks.put(id, ticks);

                    float damage = 1.0f + (ticks / 100f);
                    player.hurt(player.damageSources().magic(), damage);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    return;
                }
            }
            float percent = (value / sanity.getMaxSanity()) * 100f;
            if (percent <= 70 && Math.random() < 0.03) {
                applyShiver(player);
            }
            if (percent <= 50 && Math.random() < 0.015) {
                applyPanic(player);
            }
            if (percent <= 10 && Math.random() < 0.02) {
                applyScratching(player);
            }
            float insanityPercent = (insanity / sanity.getMaxSanity()) * 100f;
            if (insanityPercent > 0) {
                if (insanityPercent > 60 && Math.random() < 0.02) {
                    applyPanic(player);
                }
                if (insanityPercent > 90 && Math.random() < 0.05) {
                    applyScratching(player);
                }
            }
            if (percent <= 30f) {
                UUID id = player.getUUID();
                int cd = lowSanitySoundCooldown.getOrDefault(id, 0) - SANITY_TICK_INTERVAL;
                if (cd <= 0) {
                    playSanitySound(player);
                    cd = 20 * (6 + player.getRandom().nextInt(15));
                }
                lowSanitySoundCooldown.put(id, cd);
            } else {
                lowSanitySoundCooldown.remove(player.getUUID());
            }
        });
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
                if (ticksSinceCombat > COMBAT_DECAY_DELAY && value > 0) {
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
    }

    private static void applyScratching(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        player.hurt(player.damageSources().magic(), 2f);
        applyShiver(player);
    }

    private static void playSanitySound(Player player) {
        var level = player.level();
        if (level.isClientSide) return;
        var sounds = new net.minecraft.sounds.SoundEvent[]{
                net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT,
                net.minecraft.sounds.SoundEvents.ENDERMAN_STARE,
                net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT,
                net.minecraft.sounds.SoundEvents.CREEPER_PRIMED,
                net.minecraft.sounds.SoundEvents.HUSK_AMBIENT,
                net.minecraft.sounds.SoundEvents.WARDEN_ANGRY
        };
        net.minecraft.sounds.SoundEvent chosen =
                sounds[player.getRandom().nextInt(sounds.length)];
        var holder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(chosen);
        float yaw = player.getYRot();
        double angleRad = Math.toRadians(yaw + 180 + player.getRandom().nextInt(40) - 20);
        double dist = 4.0 + player.getRandom().nextDouble() * 3.0;
        double dx = player.getX() + Math.cos(angleRad) * dist;
        double dz = player.getZ() + Math.sin(angleRad) * dist;
        double dy = player.getY();
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            holder,
                            net.minecraft.sounds.SoundSource.AMBIENT,
                            dx, dy, dz,
                            1.0f,
                            0.9f + player.getRandom().nextFloat() * 0.2f,
                            player.getRandom().nextLong()
                    )
            );
        }
    }
}