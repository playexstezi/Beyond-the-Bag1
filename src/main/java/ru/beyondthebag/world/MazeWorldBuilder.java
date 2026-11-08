package ru.beyondthebag.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.beyondthebag.data.PocketData;

/** Places a fully enclosed expedition maze and its physical return gate. */
public final class MazeWorldBuilder {
    private static final int FLOOR_Y = 72;
    private static final int SPACING = 16;
    private static final int ROOM_RADIUS = 4;
    private static final int WALL_HEIGHT = 5;
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

        for (MazeLayout.Room room : layout.rooms()) {
            for (int targetId : room.connections()) {
                if (targetId <= room.id()) continue;
                MazeLayout.Room target = layout.rooms().get(targetId);
                carvePassage(level, originX + room.x() * SPACING,
                        originZ + room.z() * SPACING,
                        originX + target.x() * SPACING,
                        originZ + target.z() * SPACING,
                        room.secret() || target.secret());
            }
        }

        BlockPos entry = new BlockPos(originX, FLOOR_Y, originZ);
        level.setBlock(entry, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(entry.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(entry.above(2), Blocks.AIR.defaultBlockState(), 3);

        buildExit(level, layout, originX, originZ);
        sealTerritory(level, layout, originX, originZ);
    }

    private static void buildExit(ServerLevel level, MazeLayout layout, int originX, int originZ) {
        MazeLayout.Room exitRoom = layout.rooms().stream()
                .max((a, b) -> Integer.compare(
                        Math.abs(a.x()) + Math.abs(a.z()),
                        Math.abs(b.x()) + Math.abs(b.z())))
                .orElse(layout.rooms().get(layout.rooms().size() - 1));
        int cx = originX + exitRoom.x() * SPACING;
        int cz = originZ + exitRoom.z() * SPACING;

        // A recognizable gate: the lodestone at its foot is the interaction point.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = new BlockPos(cx + dx, FLOOR_Y, cz + dz);
                level.setBlock(floor, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                for (int dy = 1; dy <= 4; dy++) {
                    level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        int gateZ = cz + 2;
        for (int dx : new int[]{-1, 1}) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(new BlockPos(cx + dx, FLOOR_Y + dy, gateZ),
                        Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(new BlockPos(cx + dx, FLOOR_Y + 4, gateZ),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(cx, FLOOR_Y + 1, cz),
                Blocks.LODESTONE.defaultBlockState(), 3);
    }

    private static void sealTerritory(ServerLevel level, MazeLayout layout, int originX, int originZ) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (MazeLayout.Room room : layout.rooms()) {
            int x = originX + room.x() * SPACING;
            int z = originZ + room.z() * SPACING;
            minX = Math.min(minX, x - 8);
            maxX = Math.max(maxX, x + 8);
            minZ = Math.min(minZ, z - 8);
            maxZ = Math.max(maxZ, z + 8);
        }

        // Bedrock shell prevents climbing, digging or jumping out of the maze.
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + TERRITORY_HEIGHT; y++) {
            for (int x = minX; x <= maxX; x++) {
                level.setBlock(new BlockPos(x, y, minZ), Blocks.BEDROCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, y, maxZ), Blocks.BEDROCK.defaultBlockState(), 3);
            }
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(minX, y, z), Blocks.BEDROCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(maxX, y, z), Blocks.BEDROCK.defaultBlockState(), 3);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(x, FLOOR_Y + TERRITORY_HEIGHT, z),
                        Blocks.BEDROCK.defaultBlockState(), 3);
            }
        }
    }

    private static void buildRoom(ServerLevel level, MazeLayout.Room room,
            int cx, int cz, PocketData.Expedition expedition) {
        BlockState wall = room.type() == MazeLayout.RoomType.ANOMALY
                ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floor = switch (room.type()) {
            case TREASURE, RARE -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case HAZARD -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            case ANOMALY -> Blocks.BLACKSTONE.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };

        for (int dx = -ROOM_RADIUS; dx <= ROOM_RADIUS; dx++) {
            for (int dz = -ROOM_RADIUS; dz <= ROOM_RADIUS; dz++) {
                boolean edge = Math.abs(dx) == ROOM_RADIUS || Math.abs(dz) == ROOM_RADIUS;
                BlockPos floorPos = new BlockPos(cx + dx, FLOOR_Y, cz + dz);
                level.setBlock(floorPos, floor, 3);
                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    level.setBlock(floorPos.above(dy), edge ? wall : Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(floorPos.above(WALL_HEIGHT + 1),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            }
        }

        if (room.type() == MazeLayout.RoomType.TREASURE || room.type() == MazeLayout.RoomType.RARE) {
            BlockPos chestPos = new BlockPos(cx + 2, FLOOR_Y + 1, cz + 2);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
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
            level.setBlock(new BlockPos(cx, FLOOR_Y + 1, cz), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.ANOMALY) {
            level.setBlock(new BlockPos(cx, FLOOR_Y + 1, cz), Blocks.SOUL_FIRE.defaultBlockState(), 3);
        }
    }

    private static void carvePassage(ServerLevel level, int x1, int z1,
            int x2, int z2, boolean secret) {
        int x = x1, z = z1;
        BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
        while (x != x2) {
            carveCrossSection(level, x, z, floor, secret);
            x += Integer.compare(x2, x);
        }
        while (z != z2) {
            carveCrossSection(level, x, z, floor, secret);
            z += Integer.compare(z2, z);
        }
        carveCrossSection(level, x, z, floor, secret);
    }

    private static void carveCrossSection(ServerLevel level, int x, int z,
            BlockState floor, boolean secret) {
        int halfWidth = secret ? 0 : 1;
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                BlockPos base = new BlockPos(x + dx, FLOOR_Y, z + dz);
                level.setBlock(base, floor, 3);
                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    level.setBlock(base.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(base.above(WALL_HEIGHT + 1),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            }
        }
    }
}
