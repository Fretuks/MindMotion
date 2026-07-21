package net.fretux.mindmotion.event;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.fretux.mindmotion.player.SanityCapability;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonEvents {

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (player.level().isClientSide) return;
        if (!ConfigMM.COMMON.ENABLE_SANITY.get()) return;

        float loss = Math.min(5f, 0.5f + event.getAmount() * 0.25f);
        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> sanity.reduceSanity(loss));
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
                        }
                        if (event.isWasDeath()) {
                            newCap.setInsanity(0f);
                            newCap.setSanity(newCap.getMaxSanity());
                        } else {
                            newCap.setSanity(oldCap.getSanity());
                            newCap.setInsanity(oldCap.getInsanity());
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
