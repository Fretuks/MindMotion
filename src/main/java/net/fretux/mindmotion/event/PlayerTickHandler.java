package net.fretux.mindmotion.event;

import net.fretux.mindmotion.network.ModMessages;
import net.fretux.mindmotion.network.SyncIdentityBlindS2CPacket;
import net.fretux.mindmotion.network.SyncStatsS2CPacket;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class PlayerTickHandler {

    private static final int SANITY_TICK_INTERVAL = 20;
    private static final int TEMPO_TICK_INTERVAL = 15;
    private static final int COMBAT_DECAY_DELAY = 100;
    private static final Map<UUID, Integer> lastCombatTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> identityBlindUntil = new ConcurrentHashMap<>();
    private static final int IDENTITY_BLIND_DURATION = 30 * 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        boolean blindActive = identityBlindUntil.getOrDefault(player.getUUID(), 0) > player.tickCount;
        int tick = player.tickCount;
        boolean sanityTick = tick % SANITY_TICK_INTERVAL == 0;
        boolean tempoTick = tick % TEMPO_TICK_INTERVAL == 0;

        if (sanityTick) handleSanityAndInsanity(player);
        if (tempoTick) handleTempoTick(player, tick);

        if (player instanceof ServerPlayer serverPlayer && (sanityTick || tempoTick)) {
            player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity ->
                    player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo ->
                            ModMessages.CHANNEL.sendTo(
                                    new SyncStatsS2CPacket(
                                            sanity.getSanity(),
                                            sanity.getInsanity(),
                                            tempo.getTempo()
                                    ),
                                    serverPlayer.connection.connection,
                                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                            )
                    )
            );
        }
        if (player instanceof ServerPlayer serverPlayer && (sanityTick || tempoTick)) {
            ModMessages.CHANNEL.sendTo(
                    new SyncIdentityBlindS2CPacket(blindActive),
                    serverPlayer.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    private static void handleSanityAndInsanity(Player player) {
        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> {
            float value = sanity.getSanity();
            float insanity = sanity.getInsanity();
            float delta = 0f;
            boolean inDark = player.level().getMaxLocalRawBrightness(player.blockPosition()) < 5;
            boolean inRain = player.level().isRainingAt(player.blockPosition());
            boolean isHungry = player.getFoodData().getFoodLevel() < 6;
            boolean nearLava = player.level().getBlockState(player.blockPosition().below()).is(Blocks.LAVA);
            boolean inWater = player.isInWater() && !player.isUnderWater();
            boolean underWater = player.isUnderWater();
            boolean sleeping = player.isSleeping();
            boolean inLight = player.level().getMaxLocalRawBrightness(player.blockPosition()) > 12;
            boolean nearTorch = player.level().getBlockState(player.blockPosition().above()).is(Blocks.TORCH);
            if (inDark) delta -= 2f;
            if (inRain) delta -= 0.04f;
            if (isHungry) delta -= 0.06f;
            if (nearLava) delta -= 0.05f;
            if (underWater) delta -= 0.04f;
            if (inWater && !underWater) delta -= 0.02f;
            if (inLight) delta += 0.03f;
            if (nearTorch) delta += 0.05f;
            if (sleeping) delta += 0.25f;
            if (player.getUseItem() != null && player.getUseItem().isEdible()) delta += 0.05f;
            sanity.setSanity(value + delta);
            if (sanity.getSanity() <= 0) {
                sanity.setSanity(0);
                sanity.setInsanity(Math.min(sanity.getMaxSanity(), insanity + 0.3f));
                if (sanity.getInsanity() >= sanity.getMaxSanity()) {
                    player.kill();
                    return;
                }
            } else if (sanity.getInsanity() > 0) {
                sanity.setInsanity(Math.max(0, insanity - 0.15f));
            }
            float percent = (value / sanity.getMaxSanity()) * 100f;
            if (percent <= 70 && percent > 50 && Math.random() < 0.03) {
                applyShiver(player);
            }
            if (percent <= 30 && percent > 10 && Math.random() < 0.015) {
                applyPanic(player);
            }
            if (percent <= 10 && percent > 0 && Math.random() < 0.02) {
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
        });
    }

    private static void handleTempoTick(Player player, int currentTick) {
        player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> {
            int value = tempo.getTempo();
            int lastCombat = lastCombatTick.getOrDefault(player.getUUID(), -10000);
            int ticksSinceCombat = currentTick - lastCombat;

            if (ticksSinceCombat > COMBAT_DECAY_DELAY && value > 0) {
                tempo.setTempo(value - 1);
            }
        });
    }

    public static void markCombat(Player player) {
        lastCombatTick.put(player.getUUID(), player.tickCount);
    }

    public static void triggerIdentityBlindness(Player player) {
        identityBlindUntil.put(player.getUUID(), player.tickCount + IDENTITY_BLIND_DURATION);
    }

    private static void applyShiver(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
    }

    private static void applyPanic(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        player.hurt(player.damageSources().magic(), 1f);
        applyShiver(player);
        triggerIdentityBlindness(player);
    }

    private static void applyScratching(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        player.hurt(player.damageSources().magic(), 2f);
        applyShiver(player);
        triggerIdentityBlindness(player);
    }
}