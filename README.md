# Beyond the Bag

Minecraft Java Edition 1.20.1 · Forge 47.2.0 · Java 17

A Russian-language mod project about a persistent personal pocket dimension and procedurally regenerated unstable expeditions.

## Development branch

Current work is in `development/1.0.0`. The `main` branch is intentionally not updated with unfinished development builds.

### Implemented in source
- Registered craftable item “Мешок измерений”.
- Server-side persistent storage for player return points and guest permissions.
- Bag can save a return anchor with Shift + right-click and return to it with right-click.
- Outside the pocket dimensions, each normal bag use selects the personal realm or unstable expedition with an equal 50/50 chance.
- Added an unstable dimension datapack entry and travel/return flow; the current dimension terrain is still an Overworld-style placeholder until maze block placement is implemented.
- Server-side guest-list commands:
  - `/bagaccess invite <player>`
  - `/bagaccess revoke <player>`
  - `/bagaccess list`
- Persistent expedition state model: per-player seed, run identifier and instability level clamped to 0–4.
- Deterministic logical maze generator: connected room graph, room types, branches, dead ends and secret links; instability affects maze size and hazard/anomaly frequency.
- Seeded loot-table generator with instability-gated rare drops and repeatable chest rolls.
- Player mimics select a nearby explorer, copy their skin and name, imitate movement/building actions, and react to chat.
- Mimics now mirror the copied player's visible armour and held items without dropping copies of that equipment.
- Mimic armour set, elytra-compatible wings, form abilities, and the Battle Spoon with crafting recipes.
- Mirror expeditions, distortion events, anomaly hazards, guardian encounters, and an expedition stabilizer are present in source.

### Not implemented yet
- Runtime verification of dimension datapack loading on a clean Forge server.
- Full plot isolation for non-block interactions, fluids, explosions, pistons, mob griefing and other indirect world changes.
- Friend-plot selection and GUI-based invitation management.
- Bag GUI and invitation management through the GUI (not yet implemented).
- Placement of the generated maze graph into actual dimension blocks, run reset and chest population from the loot table.
- Full server-side dimension interaction protection and multiplayer travel-flow testing.

## Build

Requires JDK 17. Run:

```bash
./gradlew build
```

The mod JAR is produced in `build/libs/` after a successful build.

This is an active development branch, not a finished release. A successful CI build is not a substitute for testing the mod in a real Forge client and dedicated server.
