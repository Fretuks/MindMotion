package net.fretux.mindmotion.network;

import net.fretux.mindmotion.event.VentHandler;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class VentPacket {

    public VentPacket() {}
    public VentPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> {
                if (tempo.getTempo() < 40) return;
                if (tempo.getVentCooldown() > 0) return;
                tempo.setTempo(tempo.getTempo() - 40);
                tempo.setVentCooldown(20 * 15);
                VentHandler.performVent(player);
                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        1.2f,
                        0.8f + player.getRandom().nextFloat() * 0.4f
                );
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 5, 1));
                ModMessages.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new VentClientEffectPacket()
                );
            });
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}