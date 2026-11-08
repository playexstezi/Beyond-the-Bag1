package ru.beyondthebag.event;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/** First server-authoritative protection layer for private realm plots. */
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

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!mayUse(player, player.level(), event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
            deny(player);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!mayUse(player, player.level(), event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
            deny(player);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!mayUse(player, event.getLevel(), event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
            deny(player);
        }
    }
}
