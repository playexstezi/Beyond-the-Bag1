package ru.beyondthebag.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.beyondthebag.entity.GuardianEntity;
import ru.beyondthebag.registry.ModEntities;
import ru.beyondthebag.data.PocketData;

/** Places the deterministic expedition graph as a simple, explorable block maze. */
public final class MazeWorldBuilder {
    private static final int FLOOR_Y = 72;
    private static final int SPACING = 24;
    private static final int ROOM_RADIUS = 6;
    private static final int WALL_HEIGHT = 6;
    private static final int TERRITORY_HEIGHT = 18;

    private MazeWorldBuilder() {}

    public static BlockPos buildIfNeeded(ServerLevel level, PocketData data,
            PocketData.Expedition expedition, int originX, int originZ) {
        if (!data.isMazeGenerated(expedition.runId())) {
            build(level, expedition, originX, originZ);
            data.markMazeGenerated(expedition.runId());
        }
        return new BlockPos(originX, FLOOR_Y + 1, originZ);
    }

    private static void build(ServerLevel level, PocketData.Expedition expedition,
            int originX, int originZ) {
        MazeLayout layout = MazeLayout.generate(expedition.seed(), expedition.instability());
        for (MazeLayout.Room room : layout.rooms()) {
            int cx = originX + room.x() * SPACING;
            int cz = originZ + room.z() * SPACING;
            buildRoom(level, room, cx, cz, expedition);
        }

        // Every graph edge receives a carved passage. Secret links become narrow side passages.
        for (MazeLayout.Room room : layout.rooms()) {
            for (int targetId : room.connections()) {
                if (targetId <= room.id()) continue;
                MazeLayout.Room target = layout.rooms().get(targetId);
                carvePassage(level, originX + room.x() * SPACING,
                        originZ + room.z() * SPACING,
                        originX + target.x() * SPACING,
                        originZ + target.z() * SPACING,
                        room.isSecretConnection(targetId));
            }
        }
        // Keep the entry clear and recognizable.
        BlockPos entry = new BlockPos(originX, FLOOR_Y, originZ);
        setBlockIfChanged(level, entry, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
        setBlockIfChanged(level, entry.above(), Blocks.AIR.defaultBlockState(), 3);
        setBlockIfChanged(level, entry.above(2), Blocks.AIR.defaultBlockState(), 3);
        sealTerritory(level, layout, originX, originZ);
    }

    /** Prevents leaving the expedition by climbing, digging, or flying above it. */
    private static void sealTerritory(ServerLevel level, MazeLayout layout, int originX, int originZ) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (MazeLayout.Room room : layout.rooms()) {
            int x = originX + room.x() * SPACING;
            int z = originZ + room.z() * SPACING;
            minX = Math.min(minX, x - ROOM_RADIUS - 2);
            maxX = Math.max(maxX, x + ROOM_RADIUS + 2);
            minZ = Math.min(minZ, z - ROOM_RADIUS - 2);
            maxZ = Math.max(maxZ, z + ROOM_RADIUS + 2);
        }
        BlockState boundary = Blocks.BEDROCK.defaultBlockState();
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + TERRITORY_HEIGHT; y++) {
            for (int x = minX; x <= maxX; x++) {
                setBlockIfChanged(level, new BlockPos(x, y, minZ), boundary, 3);
                setBlockIfChanged(level, new BlockPos(x, y, maxZ), boundary, 3);
            }
            for (int z = minZ; z <= maxZ; z++) {
                setBlockIfChanged(level, new BlockPos(minX, y, z), boundary, 3);
                setBlockIfChanged(level, new BlockPos(maxX, y, z), boundary, 3);
            }
        }
        // Rooms and corridors already have solid ceilings. Avoid filling the entire
        // rectangular bounding box with bedrock: for large mazes that can mean
        // hundreds of thousands of extra block updates during dimension entry.
    }

    /** Avoids expensive block updates when generation revisits an already-correct block. */
    private static void setBlockIfChanged(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        if (!level.getBlockState(pos).equals(state)) {
            level.setBlock(pos, state, flags);
        }
    }

    private static void buildRoom(ServerLevel level, MazeLayout.Room room,
            int cx, int cz, PocketData.Expedition expedition) {
        BlockState wall = switch (room.type()) {
            case ANOMALY, BOSS -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case SHRINE -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };
        BlockState floor = switch (room.type()) {
            case TREASURE, RARE -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case HAZARD -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            case ANOMALY, BOSS -> Blocks.BLACKSTONE.defaultBlockState();
            case REST -> Blocks.MOSS_BLOCK.defaultBlockState();
            case SHRINE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case PUZZLE -> Blocks.CUT_COPPER.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };

        for (int dx = -ROOM_RADIUS; dx <= ROOM_RADIUS; dx++) {
            for (int dz = -ROOM_RADIUS; dz <= ROOM_RADIUS; dz++) {
                boolean edge = Math.abs(dx) == ROOM_RADIUS || Math.abs(dz) == ROOM_RADIUS;
                BlockPos floorPos = new BlockPos(cx + dx, FLOOR_Y, cz + dz);
                setBlockIfChanged(level, floorPos, floor, 3);
                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    BlockPos pos = floorPos.above(dy);
                    setBlockIfChanged(level, pos, edge ? wall : Blocks.AIR.defaultBlockState(), 3);
                }
                setBlockIfChanged(level, floorPos.above(WALL_HEIGHT + 1),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            }
        }

        if (room.type() == MazeLayout.RoomType.TREASURE
                || room.type() == MazeLayout.RoomType.RARE) {
            BlockPos chestPos = new BlockPos(cx + 2, FLOOR_Y + 1, cz + 2);
            setBlockIfChanged(level, chestPos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                List<ExpeditionLoot.Stack> loot = ExpeditionLoot.generate(
                        expedition.seed(), room.id(), expedition.instability());
                int slot = 0;
                for (ExpeditionLoot.Stack stack : loot) {
                    var item = BuiltInRegistries.ITEM.get(new ResourceLocation(stack.itemId()));
                    if (item != net.minecraft.world.item.Items.AIR) {
                        chest.setItem(slot++, new ItemStack(item, stack.count()));
                    }
                }
                chest.setChanged();
            }
        }
        if (room.type() == MazeLayout.RoomType.HAZARD) {
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.ANOMALY || room.type() == MazeLayout.RoomType.BOSS) {
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz),
                    Blocks.SOUL_FIRE.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.REST) {
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz), Blocks.CAMPFIRE.defaultBlockState(), 3);
            setBlockIfChanged(level, new BlockPos(cx + 2, FLOOR_Y + 1, cz), Blocks.LANTERN.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.SHRINE) {
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz), Blocks.RESPAWN_ANCHOR.defaultBlockState(), 3);
            setBlockIfChanged(level, new BlockPos(cx + 2, FLOOR_Y + 1, cz), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.PUZZLE) {
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz), Blocks.LODESTONE.defaultBlockState(), 3);
            setBlockIfChanged(level, new BlockPos(cx - 2, FLOOR_Y + 1, cz), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.BOSS) {
            // The final chamber contains the expedition's physical return gate.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                        setBlockIfChanged(level, new BlockPos(cx + dx, FLOOR_Y + 1, cz + dz),
                                Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                    }
                }
            }
            setBlockIfChanged(level, new BlockPos(cx, FLOOR_Y + 1, cz),
                    Blocks.RESPAWN_ANCHOR.defaultBlockState(), 3);
            BlockPos rewardPos = new BlockPos(cx + 3, FLOOR_Y + 1, cz + 3);
            setBlockIfChanged(level, rewardPos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(rewardPos) instanceof ChestBlockEntity chest) {
                List<ExpeditionLoot.Stack> loot = ExpeditionLoot.generate(
                        expedition.seed() ^ 0xB055L, room.id(), Math.min(4, expedition.instability() + 1));
                int slot = 0;
                for (ExpeditionLoot.Stack stack : loot) {
                    var item = BuiltInRegistries.ITEM.get(new ResourceLocation(stack.itemId()));
                    if (item != net.minecraft.world.item.Items.AIR) {
                        chest.setItem(slot++, new ItemStack(item, stack.count()));
                    }
                }
                chest.setItem(26, new ItemStack(net.minecraft.world.item.Items.NETHER_STAR));
                chest.setChanged();
            }

            // Spawn the chamber guardian once, as part of the deterministic maze build.
            GuardianEntity guardian = ModEntities.GUARDIAN.get().create(level);
            if (guardian != null) {
                guardian.setExpeditionBoss(true);
                guardian.moveTo(cx + 4.5D, FLOOR_Y + 1, cz + 0.5D, 180.0F, 0.0F);
                guardian.setPersistenceRequired();
                level.addFreshEntity(guardian);
            }
        }
    }

    private static void carvePassage(ServerLevel level, int x1, int z1,
            int x2, int z2, boolean secret) {
        int y = FLOOR_Y;
        int x = x1;
        int z = z1;
        BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
        while (x != x2) {
            carveCrossSection(level, x, z, y, floor, secret);
            x += Integer.compare(x2, x);
        }
        while (z != z2) {
            carveCrossSection(level, x, z, y, floor, secret);
            z += Integer.compare(z2, z);
        }
        carveCrossSection(level, x, z, y, floor, secret);
    }

    private static void carveCrossSection(ServerLevel level, int x, int z, int y,
            BlockState floor, boolean secret) {
        int halfWidth = secret ? 0 : 1;
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                BlockPos base = new BlockPos(x + dx, y, z + dz);
                setBlockIfChanged(level, base, floor, 3);
                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    setBlockIfChanged(level, base.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
                setBlockIfChanged(level, base.above(WALL_HEIGHT + 1),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            }
        }
    }
}
