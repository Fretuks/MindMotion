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
                .then(Commands.argument("sanity", FloatArgumentType.floatArg(0, 80))
                    .then(Commands.argument("insanity", FloatArgumentType.floatArg(0, 80))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            float sanity = FloatArgumentType.getFloat(ctx, "sanity");
                            float insanity = FloatArgumentType.getFloat(ctx, "insanity");

                            player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(cap -> {
                                cap.setSanity(sanity);
                                cap.setInsanity(insanity);
                            });

                            ctx.getSource().sendSuccess(() ->
                                net.minecraft.network.chat.Component.literal(
                                        "Set sanity = " + sanity + ", insanity = " + insanity),
                                true
                            );

                            return 1;
                        })
                    )
                )
            )
        );
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SanityCommand.register(event.getDispatcher());
    }
}
