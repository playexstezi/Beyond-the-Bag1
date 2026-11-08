package ru.beyondthebag.event;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/** Server-authoritative protection for private plots in the shared personal dimension. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class PersonalRealmProtection {
    private PersonalRealmProtection() {}

    private static boolean mayUse(Player player, Level level, int x, int z) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.dimension().equals(DimensionBagItem.PERSONAL_WORLD)) {
            return true;
        }
        PocketData data = PocketData.get(serverLevel);
        Optional<UUID> owner = data.getRealmOwnerAt(x, z);
        return owner.isPresent() && data.canAccessRealm(owner.get(), player.getUUID());
    }

    private static void deny(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.beyondthebag.realm_access_denied"), true);
    }

    private static boolean denyIfForbidden(Player player, Level level, int x, int z) {
        if (mayUse(player, level, x, z)) return false;
        deny(player);
        return true;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (denyIfForbidden(event.getPlayer(), event.getPlayer().level(),
                event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (denyIfForbidden(player, player.level(), event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (denyIfForbidden(player, event.getLevel(), event.getPos().getX(),
                event.getPos().getZ())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (denyIfForbidden(player, event.getLevel(), event.getPos().getX(),
                event.getPos().getZ())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (denyIfForbidden(player, target.level(), target.blockPosition().getX(),
                target.blockPosition().getZ())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (denyIfForbidden(player, target.level(), target.blockPosition().getX(),
                target.blockPosition().getZ())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (denyIfForbidden(player, target.level(), target.blockPosition().getX(),
                target.blockPosition().getZ())) {
            event.setCanceled(true);
        }
    }
}
