package ru.beyondthebag.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.util.RandomSource;

/**
 * Weighted anomaly selection for expedition gameplay.
 * This class only chooses an event; world effects are dispatched by the
 * dimension-side event controller when that dimension is wired in.
 */
public final class AnomalyDirector {
    private AnomalyDirector() {}

    public enum Event {
        DISTANT_WHISPER(0, 8),
        LIGHT_FLICKER(1, 10),
        DISTANT_FOOTSTEPS(1, 8),
        FALSE_DOOR(2, 7),
        CORRIDOR_SHIFT(2, 5),
        HIDDEN_CACHE(2, 3),
        FLOOR_TRAP(3, 8),
        ENTITY_SIGHTING(3, 5),
        RIFT_SURGE(4, 3);

        private final int minimumLevel;
        private final int weight;

        Event(int minimumLevel, int weight) {
            this.minimumLevel = minimumLevel;
            this.weight = weight;
        }

        public int minimumLevel() { return minimumLevel; }
        public int weight() { return weight; }
    }

    /**
     * Rolls one possible event. Higher instability unlocks more severe events.
     * The chance argument is a percentage from 0 to 100, independent of
     * event selection; this lets the caller control the pacing/cooldown.
     */
    public static Optional<Event> roll(int instability, int chancePercent, RandomSource random) {
        int level = Math.max(0, Math.min(4, instability));
        int chance = Math.max(0, Math.min(100, chancePercent));
        if (chance == 0 || random.nextInt(100) >= chance) return Optional.empty();

        List<Event> available = new ArrayList<>();
        int totalWeight = 0;
        for (Event event : Event.values()) {
            if (event.minimumLevel() <= level) {
                available.add(event);
                totalWeight += event.weight();
            }
        }
        if (totalWeight == 0) return Optional.empty();

        int choice = random.nextInt(totalWeight);
        for (Event event : available) {
            choice -= event.weight();
            if (choice < 0) return Optional.of(event);
        }
        return Optional.empty();
    }
}
