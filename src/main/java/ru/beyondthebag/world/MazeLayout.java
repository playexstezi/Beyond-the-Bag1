package ru.beyondthebag.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic logical layout for one unstable expedition.
 *
 * The same seed and instability produce the same room graph. World placement,
 * block palettes and server-side reset are deliberately handled by the dimension
 * layer, not by this data-only generator.
 */
public final class MazeLayout {
    public enum RoomType {
        ENTRY, CORRIDOR, CHAMBER, DEAD_END, TREASURE, HAZARD, ANOMALY, RARE, REST, SHRINE, PUZZLE, BOSS
    }

    public record Room(int id, int x, int z, RoomType type, int danger,
                       boolean secret, List<Integer> connections) {
        public Room {
            connections = List.copyOf(connections);
        }
    }

    private final long seed;
    private final int instability;
    private final List<Room> rooms;

    private MazeLayout(long seed, int instability, List<Room> rooms) {
        this.seed = seed;
        this.instability = instability;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
    }

    public long seed() { return seed; }
    public int instability() { return instability; }
    public List<Room> rooms() { return rooms; }

    public static MazeLayout generate(long seed, int requestedInstability) {
        int instability = Math.max(0, Math.min(4, requestedInstability));
        Random random = new Random(seed);
        int target = 42 + random.nextInt(13) + instability * 7;
        List<MutableRoom> rooms = new ArrayList<>();
        rooms.add(new MutableRoom(0, 0, 0, RoomType.ENTRY));

        // Grow a connected maze by extending randomly selected frontier rooms.
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
        while (rooms.size() < target) {
            MutableRoom parent = rooms.get(random.nextInt(rooms.size()));
            List<int[]> candidates = new ArrayList<>();
            Collections.addAll(candidates, directions);
            Collections.shuffle(candidates, random);
            boolean placed = false;
            for (int[] direction : candidates) {
                int nx = parent.x + direction[0];
                int nz = parent.z + direction[1];
                if (findAt(rooms, nx, nz) != null) continue;
                MutableRoom child = new MutableRoom(rooms.size(), nx, nz,
                        chooseType(random, rooms.size(), target, instability));
                parent.links.add(child.id);
                child.links.add(parent.id);
                rooms.add(child);
                placed = true;
                break;
            }
            if (!placed) {
                // A bounded fallback prevents a pathological seed from looping forever.
                int radius = 2 + rooms.size();
                int nx = radius;
                int nz = (rooms.size() % 2 == 0 ? radius : -radius);
                while (findAt(rooms, nx, nz) != null) { radius++; nx = radius; nz = (rooms.size() % 2 == 0 ? radius : -radius); }
                MutableRoom child = new MutableRoom(rooms.size(), nx, nz,
                        chooseType(random, rooms.size(), target, instability));
                MutableRoom nearest = rooms.stream().min((a, b) -> Integer.compare(manhattan(a, child), manhattan(b, child))).orElse(rooms.get(0));
                child.links.add(nearest.id);
                nearest.links.add(child.id);
                rooms.add(child);
            }
        }

        // Optional secret passages connect non-adjacent rooms.
        int secretAttempts = 1 + instability;
        for (int i = 0; i < secretAttempts; i++) {
            MutableRoom a = rooms.get(random.nextInt(rooms.size()));
            MutableRoom b = rooms.get(random.nextInt(rooms.size()));
            if (a != b && !a.links.contains(b.id) && manhattan(a, b) > 2) {
                a.links.add(b.id);
                b.links.add(a.id);
                a.secretLinks.add(b.id);
                b.secretLinks.add(a.id);
            }
        }

        List<Room> result = new ArrayList<>();
        for (MutableRoom room : rooms) {
            int danger = Math.min(4, instability + (room.type == RoomType.HAZARD ? 1 : 0));
            result.add(new Room(room.id, room.x, room.z, room.type, danger,
                    !room.secretLinks.isEmpty(), room.links));
        }
        return new MazeLayout(seed, instability, result);
    }

    private static RoomType chooseType(Random random, int index, int total, int instability) {
        if (index == total - 1) return RoomType.BOSS;
        float roll = random.nextFloat();
        if (roll < 0.07f) return RoomType.DEAD_END;
        if (roll < 0.17f + instability * 0.025f) return RoomType.HAZARD;
        if (roll < 0.27f + instability * 0.04f) return RoomType.ANOMALY;
        if (roll < 0.36f) return RoomType.TREASURE;
        if (roll < 0.41f) return RoomType.RARE;
        if (roll < 0.47f) return RoomType.REST;
        if (roll < 0.53f) return RoomType.SHRINE;
        if (roll < 0.59f) return RoomType.PUZZLE;
        if (roll < 0.76f) return RoomType.CORRIDOR;
        return RoomType.CHAMBER;
    }

    private static MutableRoom findAt(List<MutableRoom> rooms, int x, int z) {
        for (MutableRoom room : rooms) if (room.x == x && room.z == z) return room;
        return null;
    }

    private static int manhattan(MutableRoom a, MutableRoom b) {
        return Math.abs(a.x - b.x) + Math.abs(a.z - b.z);
    }

    private static final class MutableRoom {
        final int id, x, z;
        final RoomType type;
        final List<Integer> links = new ArrayList<>();
        final List<Integer> secretLinks = new ArrayList<>();
        MutableRoom(int id, int x, int z, RoomType type) {
            this.id = id; this.x = x; this.z = z; this.type = type;
        }
    }
}
