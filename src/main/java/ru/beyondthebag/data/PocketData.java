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

    private final Map<UUID, Home> homes = new HashMap<>();
    private final Map<UUID, Set<UUID>> guests = new HashMap<>();
    private final Map<UUID, Expedition> expeditions = new HashMap<>();
    private final Map<UUID, PersonalRealm> personalRealms = new HashMap<>();
    private int nextRealmSlot = 0;
    // Stable private plots inside the shared pocket-world dimension. Each plot is allocated once.

    /** Persistent state of the current unstable-dimension run. */
    public record Expedition(long seed, int instability, long runId, long startedAt) {
        public Expedition {
            instability = Math.max(0, Math.min(4, instability));
        }
    }

    public record Home(ResourceLocation dimension, double x, double y, double z,
                       float yaw, float pitch) {}

    /** A permanent isolated plot in the shared personal-world dimension. */
    public record PersonalRealm(int slot, int originX, int originZ) {
        public static final int SIZE = 2048;
        public int centerX() { return originX + SIZE / 2; }
        public int centerZ() { return originZ + SIZE / 2; }
        public boolean contains(int x, int z) {
            return x >= originX && x < originX + SIZE && z >= originZ && z < originZ + SIZE;
        }
    }

    /** Returns the owner of a plot at the supplied block coordinates, if any. */
    public Optional<UUID> getRealmOwnerAt(int x, int z) {
        return personalRealms.entrySet().stream()
                .filter(entry -> entry.getValue().contains(x, z))
                .map(Map.Entry::getKey).findFirst();
    }

    /** Checks plot ownership and invitation permissions. */
    public boolean canAccessRealm(UUID owner, UUID visitor) {
        return hasAccess(owner, visitor);
    }

    public Optional<PersonalRealm> getPersonalRealm(UUID owner) {
        return Optional.ofNullable(personalRealms.get(owner));
    }

    /** Allocates a non-overlapping plot once; its coordinates never change after saving. */
    public PersonalRealm getOrCreatePersonalRealm(UUID owner) {
        PersonalRealm existing = personalRealms.get(Objects.requireNonNull(owner));
        if (existing != null) return existing;
        int slot = nextRealmSlot++;
        // A fixed-width grid is bijective: no two slot numbers map to the same plot.
        int plotsPerRow = 64;
        int xIndex = slot % plotsPerRow;
        int zIndex = slot / plotsPerRow;
        PersonalRealm created = new PersonalRealm(slot, xIndex * 2048, zIndex * 2048);
        personalRealms.put(owner, created);
        setDirty();
        return created;
    }

    public static PocketData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PocketData::load, PocketData::new, DATA_NAME);
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

    public Optional<Expedition> getExpedition(UUID player) {
        return Optional.ofNullable(expeditions.get(player));
    }

    /**
     * Joins the most recently opened expedition while its rendezvous window is
     * active. Players who arrive together share seed/runId and therefore the
     * same generated maze. Otherwise a new solo expedition is created.
     */
    public Expedition joinOrStartExpedition(UUID player, long seed, long runId,
                                            long now, long joinWindowTicks) {
        Objects.requireNonNull(player);
        Expedition existingForPlayer = expeditions.get(player);
        if (existingForPlayer != null) return existingForPlayer;

        Expedition rendezvous = expeditions.values().stream()
                .filter(candidate -> now >= candidate.startedAt()
                        && now - candidate.startedAt() <= joinWindowTicks)
                .max(Comparator.comparingLong(Expedition::startedAt))
                .orElse(null);
        Expedition expedition = rendezvous != null
                ? new Expedition(rendezvous.seed(), rendezvous.instability(),
                        rendezvous.runId(), rendezvous.startedAt())
                : new Expedition(seed, 0, runId, now);
        expeditions.put(player, expedition);
        setDirty();
        return expedition;
    }

    /** Starts a fresh solo run; retained for callers that explicitly need one. */
    public Expedition startExpedition(UUID player, long seed, long runId) {
        Expedition expedition = new Expedition(seed, 0, runId, 0L);
        expeditions.put(Objects.requireNonNull(player), expedition);
        setDirty();
        return expedition;
    }

    /**
     * Instability belongs to the shared run, not to an individual participant.
     * Updating it for one member updates every player carrying the same run id.
     */
    public Optional<Expedition> setInstability(UUID player, int level) {
        Expedition current = expeditions.get(player);
        if (current == null) return Optional.empty();
        Expedition updated = new Expedition(current.seed(), level, current.runId(), current.startedAt());
        expeditions.replaceAll((member, expedition) ->
                expedition.runId() == current.runId() ? updated : expedition);
        setDirty();
        return Optional.of(updated);
    }

    /** Returns a stable snapshot of all players assigned to the same run. */
    public Set<UUID> getExpeditionMembers(UUID player) {
        Expedition current = expeditions.get(player);
        if (current == null) return Collections.emptySet();
        Set<UUID> members = new HashSet<>();
        expeditions.forEach((member, expedition) -> {
            if (expedition.runId() == current.runId()) members.add(member);
        });
        return Collections.unmodifiableSet(members);
    }

    public void clearExpedition(UUID player) {
        if (expeditions.remove(player) != null) setDirty();
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

        ListTag expeditionList = new ListTag();
        expeditions.forEach((player, expedition) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putLong("seed", expedition.seed());
            entry.putInt("instability", expedition.instability());
            entry.putLong("runId", expedition.runId());
            entry.putLong("startedAt", expedition.startedAt());
            expeditionList.add(entry);
        });
        tag.put("expeditions", expeditionList);

        ListTag realmList = new ListTag();
        personalRealms.forEach((owner, realm) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putInt("slot", realm.slot());
            entry.putInt("originX", realm.originX());
            entry.putInt("originZ", realm.originZ());
            realmList.add(entry);
        });
        tag.put("personalRealms", realmList);
        tag.putInt("nextRealmSlot", nextRealmSlot);
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
        ListTag expeditionList = tag.getList("expeditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < expeditionList.size(); i++) {
            CompoundTag entry = expeditionList.getCompound(i);
            try {
                data.expeditions.put(entry.getUUID("player"), new Expedition(
                        entry.getLong("seed"), entry.getInt("instability"), entry.getLong("runId"), entry.getLong("startedAt")));
            } catch (RuntimeException ignored) {
                // Ignore malformed expedition state.
            }
        }
        ListTag realmList = tag.getList("personalRealms", Tag.TAG_COMPOUND);
        int maxSlot = -1;
        for (int i = 0; i < realmList.size(); i++) {
            CompoundTag entry = realmList.getCompound(i);
            try {
                UUID owner = entry.getUUID("owner");
                PersonalRealm realm = new PersonalRealm(entry.getInt("slot"),
                        entry.getInt("originX"), entry.getInt("originZ"));
                data.personalRealms.put(owner, realm);
                maxSlot = Math.max(maxSlot, realm.slot());
            } catch (RuntimeException ignored) {
                // Ignore malformed plot records.
            }
        }
        data.nextRealmSlot = Math.max(tag.getInt("nextRealmSlot"), maxSlot + 1);
        return data;
    }
}
