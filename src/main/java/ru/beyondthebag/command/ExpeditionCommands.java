package ru.beyondthebag.command;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;

/**
 * Administrative/testing commands for the expedition state machine.
 * These commands do not yet create or place the actual pocket dimension.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class ExpeditionCommands {
    private ExpeditionCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("bagrun")
                .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("unstable")
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> instability(ctx.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("end").executes(ctx -> end(ctx.getSource()))));
    }

    private static int start(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long seed = ThreadLocalRandom.current().nextLong();
        long runId = ThreadLocalRandom.current().nextLong();
        PocketData.Expedition run = PocketData.get(source.getServer().overworld())
                .startExpedition(player.getUUID(), seed, runId);
        source.sendSuccess(() -> Component.translatable("command.beyondthebag.run_started",
                Long.toUnsignedString(run.runId())), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PocketData data = PocketData.get(source.getServer().overworld());
        var current = data.getExpedition(player.getUUID());
        if (current.isEmpty()) {
            source.sendFailure(Component.translatable("command.beyondthebag.no_active_run"));
            return 0;
        }
        PocketData.Expedition run = current.get();
        source.sendSuccess(() -> Component.translatable("command.beyondthebag.run_status",
                Long.toUnsignedString(run.runId()), run.instability()), false);
        return 1;
    }

    private static int instability(CommandSourceStack source, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var updated = PocketData.get(source.getServer().overworld()).setInstability(player.getUUID(), level);
        if (updated.isEmpty()) {
            source.sendFailure(Component.translatable("command.beyondthebag.no_active_run"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.beyondthebag.instability_set", level), false);
        return 1;
    }

    private static int end(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PocketData data = PocketData.get(source.getServer().overworld());
        if (data.getExpedition(player.getUUID()).isEmpty()) {
            source.sendFailure(Component.translatable("command.beyondthebag.no_active_run"));
            return 0;
        }
        data.clearExpedition(player.getUUID());
        source.sendSuccess(() -> Component.translatable("command.beyondthebag.run_ended"), false);
        return 1;
    }
}
