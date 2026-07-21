package net.fretux.mindmotion.event;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class CombatEvents {

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            if (ConfigMM.COMMON.ENABLE_TEMPO.get()) {
                player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.addTempo(3));
            }
            if (ConfigMM.COMMON.ENABLE_SANITY.get() || ConfigMM.COMMON.ENABLE_TEMPO.get()) {
                PlayerTickHandler.markCombat(player);
            }
        }

        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attacker) {
            if (ConfigMM.COMMON.ENABLE_TEMPO.get()) {
                attacker.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.addTempo(4));
            }
            if (ConfigMM.COMMON.ENABLE_SANITY.get() || ConfigMM.COMMON.ENABLE_TEMPO.get()) {
                PlayerTickHandler.markCombat(attacker);
            }
        }
    }
}
