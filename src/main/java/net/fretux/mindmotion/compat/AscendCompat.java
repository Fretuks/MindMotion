package net.fretux.mindmotion.compat;


import net.fretux.ascend.player.PlayerStatsProvider;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.fretux.mindmotion.player.SanityCapability;
import net.fretux.mindmotion.player.TempoCapability;
import net.minecraft.world.entity.player.Player;

public final class AscendCompat {

    private AscendCompat() {}
    
    public static void applyWillpowerBonuses(Player player) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int willpower = stats.getAttributeLevel("willpower");
            float sanityBonus = willpower * 3.0f;
            float tempoBonus = willpower * 0.5f;
            player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(cap -> {
                if (cap instanceof SanityCapability impl) {
                    impl.setBonusMaxSanity(sanityBonus);
                }
            });
            player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(cap -> {
                if (cap instanceof TempoCapability impl) {
                    impl.setBonusMaxTempo(tempoBonus);
                }
            });
        });
    }
}
