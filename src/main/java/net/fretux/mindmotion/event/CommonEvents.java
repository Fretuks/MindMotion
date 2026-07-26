package net.fretux.mindmotion.event;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.compat.IronsSpellbooksCompat;
import net.fretux.mindmotion.damage.ModDamageTypes;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.fretux.mindmotion.player.SanityCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public class CommonEvents {
    private static final int MADNESS_DECAY_DELAY_TICKS = 2 * 20;

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (player.level().isClientSide) return;
        if (!ConfigMM.COMMON.ENABLE_SANITY.get()) return;

        float loss = Math.min(5f, 0.5f + event.getAmount() * 0.25f);
        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> sanity.reduceSanity(loss));
    }

    @SubscribeEvent
    public void onPlayerDamaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ConfigMM.COMMON.ENABLE_SANITY.get()) return;
        if (!event.getSource().is(ModDamageTypes.TERROR) || event.getAmount() <= 0f) return;

        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> {
            // One heart is two health points, so "2 x hearts taken" equals the
            // final post-mitigation health-point damage reported by this event.
            sanity.setMadness(sanity.getMadness() + event.getAmount());
            sanity.setMadnessDecayDelayTicks(MADNESS_DECAY_DELAY_TICKS);
            if (sanity.getMadness() < sanity.getMaxMadness()) return;

            sanity.setMadness(0f);
            // Defer the backlash until the next player tick. Calling hurt from
            // inside LivingDamageEvent can complete two nested lethal hits and
            // broadcast both of their death messages.
            sanity.setMadnessBacklashPending(true);
        });
    }

    public static void triggerPendingMadnessBacklash(ServerPlayer player) {
        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> {
            if (!sanity.isMadnessBacklashPending()) return;

            sanity.setMadnessBacklashPending(false);
            if (!player.isAlive()) return;

            sanity.setMadnessStunTicks(30);
            triggerMadnessBacklash(player);
        });
    }

    private static void triggerMadnessBacklash(ServerPlayer player) {
        if (!player.isAlive()) return;

        player.stopUsingItem();
        player.setSprinting(false);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false));

        float backlashDamage = player.getMaxHealth() * 0.30f;
        player.hurt(ModDamageTypes.madnessBacklash(player.level()), backlashDamage);

        int remainingFood = Math.max(0, (int) Math.floor(player.getFoodData().getFoodLevel() * 0.5f));
        player.getFoodData().setFoodLevel(remainingFood);
        player.getFoodData().setSaturation(Math.min(player.getFoodData().getSaturationLevel(), remainingFood));

        if (ModList.get().isLoaded("irons_spellbooks")) {
            try {
                IronsSpellbooksCompat.reduceMana(player, 0.30f);
            } catch (LinkageError | RuntimeException ignored) {
                // Optional integrations must never break the backlash itself.
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (player.level().isClientSide) return;
        if (!ConfigMM.COMMON.ENABLE_SANITY.get()) return;

        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> {
            if (event.getEntity() instanceof Monster) {
                sanity.addSanity(0.75f);
            } else if (event.getEntity() instanceof Villager) {
                sanity.reduceSanity(4f);
            }
        });
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getEntity().getCapability(PlayerCapabilityProvider.SANITY).ifPresent(newCap ->
            event.getOriginal().getCapability(PlayerCapabilityProvider.SANITY)
                    .ifPresent(oldCap -> {
                        if (newCap instanceof SanityCapability newImpl && oldCap instanceof SanityCapability oldImpl) {
                            newImpl.setBaseMaxSanity(oldImpl.getBaseMaxSanity());
                            newImpl.setBonusMaxSanity(oldImpl.getBonusMaxSanity());
                            newImpl.setBaseMaxMadness(oldImpl.getBaseMaxMadness());
                            newImpl.setBonusMaxMadness(oldImpl.getBonusMaxMadness());
                            newImpl.setBonusMadnessDecayPerTick(oldImpl.getBonusMadnessDecayPerTick());
                        }
                        if (event.isWasDeath()) {
                            newCap.setInsanity(0f);
                            newCap.setSanity(newCap.getMaxSanity());
                            newCap.setMadness(0f);
                            newCap.setMadnessDecayDelayTicks(0);
                            newCap.setMadnessStunTicks(0);
                            newCap.setMadnessBacklashPending(false);
                        } else {
                            newCap.setSanity(oldCap.getSanity());
                            newCap.setInsanity(oldCap.getInsanity());
                            newCap.setMadness(oldCap.getMadness());
                            newCap.setMadnessDecayDelayTicks(oldCap.getMadnessDecayDelayTicks());
                            newCap.setMadnessStunTicks(oldCap.getMadnessStunTicks());
                            newCap.setMadnessBacklashPending(oldCap.isMadnessBacklashPending());
                        }
                    })
        );
        event.getEntity().getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(newCap ->
            event.getOriginal().getCapability(PlayerCapabilityProvider.TEMPO)
                    .ifPresent(oldCap -> {
                        newCap.setTempo(oldCap.getTempo());
                        newCap.setVentCooldown(oldCap.getVentCooldown());
                    })
        );
        event.getOriginal().invalidateCaps();
    }
}
