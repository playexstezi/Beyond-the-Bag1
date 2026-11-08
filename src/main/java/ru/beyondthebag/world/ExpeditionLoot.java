package ru.beyondthebag.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Seeded loot rolls for generated expedition containers. */
public final class ExpeditionLoot {
    public record Loot(String itemId, int minCount, int maxCount, int weight, int minInstability) {}
    public record Stack(String itemId, int count) {}

    private static final List<Loot> TABLE = List.of(
            new Loot("minecraft:bread", 2, 5, 18, 0),
            new Loot("minecraft:torch", 4, 12, 16, 0),
            new Loot("minecraft:iron_ingot", 1, 4, 14, 0),
            new Loot("minecraft:gold_ingot", 1, 3, 9, 1),
            new Loot("minecraft:ender_pearl", 1, 2, 5, 2),
            new Loot("minecraft:diamond", 1, 2, 3, 2),
            new Loot("minecraft:golden_apple", 1, 1, 2, 3),
            new Loot("minecraft:amethyst_shard", 2, 6, 8, 1),
            new Loot("minecraft:echo_shard", 1, 2, 3, 3),
            new Loot("minecraft:enchanted_book", 1, 1, 2, 4)
    );

    private ExpeditionLoot() {}

    /**
     * Generates a stable chest roll from expedition seed and room id.
     * Re-running the same room generation gives the same result.
     */
    public static List<Stack> generate(long expeditionSeed, int roomId, int instability) {
        int level = Math.max(0, Math.min(4, instability));
        Random random = new Random(expeditionSeed ^ (0x9E3779B97F4A7C15L * (roomId + 1L)));
        int rolls = 2 + random.nextInt(2) + (level >= 3 ? 1 : 0);
        List<Loot> available = TABLE.stream().filter(entry -> entry.minInstability() <= level).toList();
        List<Stack> result = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            int totalWeight = available.stream().mapToInt(Loot::weight).sum();
            int pick = random.nextInt(totalWeight);
            Loot selected = available.get(0);
            for (Loot entry : available) {
                pick -= entry.weight();
                if (pick < 0) { selected = entry; break; }
            }
            int count = selected.minCount() + random.nextInt(selected.maxCount() - selected.minCount() + 1);
            result.add(new Stack(selected.itemId(), count));
        }
        return List.copyOf(result);
    }
}
