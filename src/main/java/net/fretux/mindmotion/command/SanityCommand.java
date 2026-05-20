package net.fretux.mindmotion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SanityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("sanity", FloatArgumentType.floatArg(0))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        float requestedSanity = FloatArgumentType.getFloat(ctx, "sanity");
                        final float[] appliedSanity = new float[] {0f};
                        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(cap -> {
                            float clampedSanity = Math.min(cap.getMaxSanity(), requestedSanity);
                            if (clampedSanity > 0f && cap.getInsanity() > 0f) {
                                cap.setInsanity(0f);
                            }
                            cap.setSanity(clampedSanity);
                            appliedSanity[0] = cap.getSanity();
                        });

                        ctx.getSource().sendSuccess(() ->
                            net.minecraft.network.chat.Component.literal("Set sanity = " + appliedSanity[0]),
                            true
                        );

                        return 1;
                    })
                )
            )
        );
        dispatcher.register(Commands.literal("insanity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("insanity", FloatArgumentType.floatArg(0))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        float requestedInsanity = FloatArgumentType.getFloat(ctx, "insanity");
                        final float[] appliedInsanity = new float[] {0f};
                        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(cap -> {
                            float clampedInsanity = Math.min(cap.getMaxSanity(), requestedInsanity);
                            if (clampedInsanity > 0f && cap.getSanity() > 0f) {
                                cap.setSanity(0f);
                            }
                            cap.setInsanity(clampedInsanity);
                            appliedInsanity[0] = cap.getInsanity();
                        });

                        ctx.getSource().sendSuccess(() ->
                            net.minecraft.network.chat.Component.literal("Set insanity = " + appliedInsanity[0]),
                            true
                        );

                        return 1;
                    })
                )
            )
        );
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SanityCommand.register(event.getDispatcher());
    }
}
