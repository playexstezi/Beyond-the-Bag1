package ru.beyondthebag.command;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/**
 * Server-authoritative access management for a player's personal space.
 * Only the owner can change their guest list; invited guests can visit by command.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class BagAccessCommands {
    private static final ResourceKey<Level> PERSONAL_WORLD = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(BeyondTheBag.MOD_ID, "personal_world"));

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
                .then(Commands.literal("visit")
                        .then(Commands.argument("owner", EntityArgument.player())
                                .executes(context -> visit(context.getSource(),
                                        EntityArgument.getPlayer(context, "owner")))))
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
            if (guest.level().dimension().equals(PERSONAL_WORLD)
                    && data.getRealmOwnerAt(guest.blockPosition().getX(), guest.blockPosition().getZ())
                            .filter(owner.getUUID()::equals).isPresent()) {
                ServerLevel overworld = source.getServer().overworld();
                guest.teleportTo(overworld, guest.getX(), overworld.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) guest.getX(), (int) guest.getZ()),
                        guest.getZ(), guest.getYRot(), guest.getXRot());
            }
        } else {
            source.sendFailure(Component.translatable("command.beyondthebag.not_invited"));
        }
        return 1;
    }

    private static int visit(CommandSourceStack source, ServerPlayer owner) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer visitor = source.getPlayerOrException();
        PocketData data = PocketData.get(source.getServer().overworld());
        if (!data.canAccessRealm(owner.getUUID(), visitor.getUUID())) {
            source.sendFailure(Component.translatable("command.beyondthebag.visit_denied"));
            return 0;
        }
        ServerLevel destination = source.getServer().getLevel(PERSONAL_WORLD);
        if (destination == null) {
            source.sendFailure(Component.translatable("message.beyondthebag.personal_world_unavailable"));
            return 0;
        }

        PocketData.PersonalRealm realm = data.getOrCreatePersonalRealm(owner.getUUID());
        if (!visitor.level().dimension().equals(PERSONAL_WORLD)) {
            data.setHome(visitor.getUUID(), new PocketData.Home(
                    visitor.level().dimension().location(), visitor.getX(), visitor.getY(),
                    visitor.getZ(), visitor.getYRot(), visitor.getXRot()));
        }
        int x = realm.centerX();
        int z = realm.centerZ();
        int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawn = new BlockPos(x, Math.max(destination.getMinBuildHeight() + 1, y), z);
        destination.getChunkAt(spawn);
        visitor.teleportTo(destination, x + 0.5, spawn.getY(), z + 0.5,
                visitor.getYRot(), visitor.getXRot());
        visitor.sendSystemMessage(Component.translatable("command.beyondthebag.visiting", owner.getGameProfile().getName()));
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
