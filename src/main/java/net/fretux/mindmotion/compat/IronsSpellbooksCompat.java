package net.fretux.mindmotion.compat;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.UpdateClient;
import net.minecraft.server.level.ServerPlayer;

public final class IronsSpellbooksCompat {
    private IronsSpellbooksCompat() {
    }

    public static void reduceMana(ServerPlayer player, float fraction) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(Math.max(0f, magicData.getMana() * (1f - fraction)));
        UpdateClient.SendManaUpdate(player, magicData);
    }
}
