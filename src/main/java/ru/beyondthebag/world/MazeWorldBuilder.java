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
import ru.beyondthebag.data.PocketData;

/** Places the deterministic expedition graph as a simple, explorable block maze. */
public final class MazeWorldBuilder {
    private static final int FLOOR_Y = 72;
    private static final int SPACING = 16;
    private static final int ROOM_RADIUS = 4;
    private static final int WALL_HEIGHT = 5;

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
                        room.secret() || target.secret());
            }
        }
        // Keep the entry clear and recognizable.
        BlockPos entry = new BlockPos(originX, FLOOR_Y, originZ);
        level.setBlock(entry, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(entry.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(entry.above(2), Blocks.AIR.defaultBlockState(), 3);
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
                    BlockPos pos = floorPos.above(dy);
                    level.setBlock(pos, edge ? wall : Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(floorPos.above(WALL_HEIGHT + 1),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            }
        }

        if (room.type() == MazeLayout.RoomType.TREASURE
                || room.type() == MazeLayout.RoomType.RARE) {
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
            level.setBlock(new BlockPos(cx, FLOOR_Y + 1, cz),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        }
        if (room.type() == MazeLayout.RoomType.ANOMALY) {
            level.setBlock(new BlockPos(cx, FLOOR_Y + 1, cz),
                    Blocks.SOUL_FIRE.defaultBlockState(), 3);
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
