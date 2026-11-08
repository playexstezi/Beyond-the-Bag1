package ru.beyondthebag.data;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent server-side data for personal-space return points and guest access.
 * Stored in the Overworld data storage, independent of dimension unloads.
 */
public final class PocketData extends SavedData {
    private static final String DATA_NAME = "beyondthebag_pocket_data";
    private static final Factory<PocketData> FACTORY =
            new Factory<>(PocketData::new, PocketData::load);

    private final Map<UUID, Home> homes = new HashMap<>();
    private final Map<UUID, Set<UUID>> guests = new HashMap<>();

    public record Home(ResourceLocation dimension, double x, double y, double z,
                       float yaw, float pitch) {}

    public static PocketData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<Home> getHome(UUID owner) {
        return Optional.ofNullable(homes.get(owner));
    }

    public void setHome(UUID owner, Home home) {
        homes.put(Objects.requireNonNull(owner), Objects.requireNonNull(home));
        setDirty();
    }

    public boolean removeHome(UUID owner) {
        boolean changed = homes.remove(owner) != null;
        if (changed) setDirty();
        return changed;
    }

    public boolean grantAccess(UUID owner, UUID guest) {
        if (owner.equals(guest)) return false;
        boolean changed = guests.computeIfAbsent(owner, ignored -> new HashSet<>()).add(guest);
        if (changed) setDirty();
        return changed;
    }

    public boolean revokeAccess(UUID owner, UUID guest) {
        Set<UUID> set = guests.get(owner);
        if (set == null) return false;
        boolean changed = set.remove(guest);
        if (set.isEmpty()) guests.remove(owner);
        if (changed) setDirty();
        return changed;
    }

    public boolean hasAccess(UUID owner, UUID visitor) {
        return owner.equals(visitor) || guests.getOrDefault(owner, Collections.emptySet()).contains(visitor);
    }

    public Set<UUID> getGuests(UUID owner) {
        return Collections.unmodifiableSet(new HashSet<>(guests.getOrDefault(owner, Collections.emptySet())));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag homeList = new ListTag();
        homes.forEach((owner, home) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putString("dimension", home.dimension().toString());
            entry.putDouble("x", home.x());
            entry.putDouble("y", home.y());
            entry.putDouble("z", home.z());
            entry.putFloat("yaw", home.yaw());
            entry.putFloat("pitch", home.pitch());
            homeList.add(entry);
        });
        tag.put("homes", homeList);

        ListTag accessList = new ListTag();
        guests.forEach((owner, visitors) -> visitors.forEach(visitor -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putUUID("guest", visitor);
            accessList.add(entry);
        }));
        tag.put("access", accessList);
        return tag;
    }

    private static PocketData load(CompoundTag tag) {
        PocketData data = new PocketData();
        ListTag homeList = tag.getList("homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < homeList.size(); i++) {
            CompoundTag entry = homeList.getCompound(i);
            try {
                ResourceLocation dimension = new ResourceLocation(entry.getString("dimension"));
                data.homes.put(entry.getUUID("owner"), new Home(dimension,
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                        entry.getFloat("yaw"), entry.getFloat("pitch")));
            } catch (RuntimeException ignored) {
                // Ignore one malformed record rather than losing all saved data.
            }
        }
        ListTag accessList = tag.getList("access", Tag.TAG_COMPOUND);
        for (int i = 0; i < accessList.size(); i++) {
            CompoundTag entry = accessList.getCompound(i);
            try {
                UUID owner = entry.getUUID("owner");
                UUID guest = entry.getUUID("guest");
                if (!owner.equals(guest)) {
                    data.guests.computeIfAbsent(owner, ignored -> new HashSet<>()).add(guest);
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed permission records.
            }
        }
        return data;
    }
}
