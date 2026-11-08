package ru.beyondthebag.command;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.arguments.EntityArgument;
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
 * Server-authoritative access management for a player's personal space.
 * Only the owner can change their guest list.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class BagAccessCommands {
    private BagAccessCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("bagaccess")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> invite(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> revoke(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource()))));
    }

    private static int invite(CommandSourceStack source, ServerPlayer guest) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        PocketData data = PocketData.get(source.getServer().overworld());
        if (data.grantAccess(owner.getUUID(), guest.getUUID())) {
            source.sendSuccess(() -> Component.translatable("command.beyondthebag.invited", guest.getGameProfile().getName()), false);
            guest.sendSystemMessage(Component.translatable("command.beyondthebag.invitation_received", owner.getGameProfile().getName()));
        } else {
            source.sendFailure(Component.translatable("command.beyondthebag.already_invited"));
        }
        return 1;
    }

    private static int revoke(CommandSourceStack source, ServerPlayer guest) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        PocketData data = PocketData.get(source.getServer().overworld());
        if (data.revokeAccess(owner.getUUID(), guest.getUUID())) {
            source.sendSuccess(() -> Component.translatable("command.beyondthebag.access_revoked", guest.getGameProfile().getName()), false);
            guest.sendSystemMessage(Component.translatable("command.beyondthebag.access_removed", owner.getGameProfile().getName()));
        } else {
            source.sendFailure(Component.translatable("command.beyondthebag.not_invited"));
        }
        return 1;
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        Set<UUID> guests = PocketData.get(source.getServer().overworld()).getGuests(owner.getUUID());
        if (guests.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.beyondthebag.guest_list_empty"), false);
        } else {
            String names = guests.stream().map(UUID::toString).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.translatable("command.beyondthebag.guest_list", names), false);
        }
        return guests.size();
    }
}
