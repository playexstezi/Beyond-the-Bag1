# Beyond the Bag

Minecraft Java Edition 1.20.1 · Forge 47.2.0 · Java 17

A Russian-language mod project about a persistent personal pocket dimension and procedurally regenerated unstable expeditions.

## Development branch

Current work is in `development/1.0.0`. The `main` branch is intentionally not updated with unfinished development builds.

### Implemented in source
- Registered craftable item “Мешок измерений”.
- Server-side persistent storage for player return points and guest permissions.
- Bag can save a return anchor with Shift + right-click and return to it with right-click.
- Server-side guest-list commands:
  - `/bagaccess invite <player>`
  - `/bagaccess revoke <player>`
  - `/bagaccess list`
- Persistent expedition state model: per-player seed, run identifier and instability level clamped to 0–4.

### Not implemented yet
- Separate persistent personal dimension and its terrain generation.
- Bag GUI and invitation management through the GUI.
- Procedural unstable maze generation, run reset and loot generation.
- Instability events, traps, unique monsters and boss.
- Server-side dimension interaction protection and full multiplayer travel flow.

## Build

Requires JDK 17. Run:

```bash
./gradlew build
```

The mod JAR is produced in `build/libs/` after a successful build.

This is an active development branch, not a finished release. No release JAR is provided until the agreed gameplay systems are implemented and the build is verified.
