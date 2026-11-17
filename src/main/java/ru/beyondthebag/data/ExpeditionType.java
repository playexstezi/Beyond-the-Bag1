package ru.beyondthebag.data;

/**
 * Stable identity of an unstable expedition route.
 * New route types can be added without changing the saved-data schema.
 */
public enum ExpeditionType {
    MAZE,
    RIDDLE,
    DUNGEON,
    MIRROR,
    DISTORTION;

    public static ExpeditionType fromSerialized(String value) {
        if (value == null || value.isBlank()) return MAZE;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return MAZE;
        }
    }
}
