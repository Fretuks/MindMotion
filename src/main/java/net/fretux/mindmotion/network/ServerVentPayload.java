package net.fretux.mindmotion.network;

import net.fretux.mindmotion.AscendMindMotion;
import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.event.VentHandler;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerVentPayload(String string) implements CustomPacketPayload {
    public static final Type<ServerVentPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AscendMindMotion.MODID, "server_vent_payload")
    );

    public static final StreamCodec<FriendlyByteBuf, ServerVentPayload> CODEC = StreamCodec.of(
            ServerVentPayload::write,
            ServerVentPayload::read
    );

    public static void write(FriendlyByteBuf buffer, ServerVentPayload payload) {buffer.writeUtf(payload.string);}

    public static ServerVentPayload read(FriendlyByteBuf buffer) {
        return new ServerVentPayload(buffer.readUtf());
    }

    public static void handleServer(ServerVentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            int ventCost = ConfigMM.COMMON.VENT_COST.get();
            var tempo = player.getData(PlayerCapabilityProvider.TEMPO);
                if (tempo.getTempo() < ventCost) return;
                if (tempo.getVentCooldown() > 0) return;
                tempo.setTempo(tempo.getTempo() - ConfigMM.COMMON.VENT_COST.get());
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
                PacketDistributor.sendToPlayer(player, new ClientVentPayload(""));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
