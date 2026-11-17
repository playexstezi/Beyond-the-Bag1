package ru.beyondthebag.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.beyondthebag.data.PocketData;

/** A compact mirror route: a false home, three reflection trials and an exit chamber. */
public final class MirrorWorldBuilder {
    private static final int Y = 72;
    private static final int STEP = 24;
    private static final BlockState[] SHARDS = {
            Blocks.CYAN_CONCRETE.defaultBlockState(),
            Blocks.MAGENTA_CONCRETE.defaultBlockState(),
            Blocks.BLUE_CONCRETE.defaultBlockState()
    };
    private static final BlockState[] REFLECTIONS = {
            Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
            Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(),
            Blocks.BLUE_STAINED_GLASS.defaultBlockState()
    };

    private MirrorWorldBuilder() {}

    public static BlockPos buildIfNeeded(ServerLevel level, PocketData data,
            PocketData.Expedition expedition, int originX, int originZ) {
        if (!data.isMazeGenerated(expedition.runId())) {
            for (int room = 0; room < 5; room++) {
                int centerX = originX + room * STEP;
                buildRoom(level, centerX, originZ, room);
                if (room >= 1 && room <= 3) {
                    int stage = room - 1;
                    int answer = answer(expedition.seed(), stage);
                    BlockPos clue = new BlockPos(centerX, Y + 2, originZ - 5);
                    set(level, clue, REFLECTIONS[answer]);
                    set(level, clue.above(), REFLECTIONS[answer]);
                    for (int option = 0; option < 3; option++) {
                        BlockPos shard = shardPosition(originX, originZ, stage, option);
                        set(level, shard.below(), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                        set(level, shard, SHARDS[option]);
                    }
                }
            }
            buildFalsePassage(level, originX + STEP, originZ);
            BlockPos lootPos = new BlockPos(originX + STEP * 2 + 5, Y + 1, originZ + 1);
            set(level, lootPos, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(lootPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                chest.clearContent();
                chest.setItem(0, new net.minecraft.world.item.ItemStack(ru.beyondthebag.registry.ModItems.MIRROR_FRAGMENT.get(), 3));
                chest.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, 6));
                chest.setChanged();
            }
            set(level, gatePosition(originX, originZ).below(), Blocks.PURPUR_BLOCK.defaultBlockState());
            data.markMazeGenerated(expedition.runId());
        }
        return new BlockPos(originX, Y + 1, originZ);
    }

    public static int answer(long seed, int stage) {
        long value = seed + stage * 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return Math.floorMod((int) (value >>> 32), 3);
    }

    public static BlockPos shardPosition(int originX, int originZ, int stage, int option) {
        return new BlockPos(originX + (stage + 1) * STEP + (option - 1) * 4,
                Y + 1, originZ + 3);
    }

    public static BlockPos gatePosition(int originX, int originZ) {
        return new BlockPos(originX + STEP * 4, Y + 1, originZ);
    }

    private static void buildRoom(ServerLevel level, int centerX, int centerZ, int index) {
        BlockState floor = index == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                : index == 4 ? Blocks.PURPUR_BLOCK.defaultBlockState()
                : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState wall = index == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                : index == 4 ? Blocks.PURPUR_PILLAR.defaultBlockState()
                : Blocks.TINTED_GLASS.defaultBlockState();

        for (int dx = -8; dx <= 8; dx++) for (int dz = -7; dz <= 7; dz++) {
            set(level, new BlockPos(centerX + dx, Y, centerZ + dz), floor);
            boolean edge = Math.abs(dx) == 8 || Math.abs(dz) == 7;
            for (int dy = 1; dy <= 5; dy++) {
                BlockState state = edge ? wall : Blocks.AIR.defaultBlockState();
                if (index == 0 && Math.abs(dz) == 7 && Math.abs(dx) <= 2 && dy >= 2 && dy <= 4)
                    state = Blocks.GLASS.defaultBlockState();
                set(level, new BlockPos(centerX + dx, Y + dy, centerZ + dz), state);
            }
            set(level, new BlockPos(centerX + dx, Y + 6, centerZ + dz), wall);
        }

        if (index > 0) openDoor(level, centerX - 8, centerZ, true);
        if (index < 4) openDoor(level, centerX + 8, centerZ, false);
        if (index == 0) {
            set(level, new BlockPos(centerX - 4, Y + 1, centerZ - 2), Blocks.CRAFTING_TABLE.defaultBlockState());
            set(level, new BlockPos(centerX + 4, Y + 1, centerZ - 2), Blocks.BARREL.defaultBlockState());
            set(level, new BlockPos(centerX, Y + 1, centerZ + 3), Blocks.WHITE_CARPET.defaultBlockState());
            set(level, new BlockPos(centerX, Y + 1, centerZ - 5), Blocks.TINTED_GLASS.defaultBlockState());
        } else if (index >= 1 && index <= 3) {
            set(level, new BlockPos(centerX, Y + 1, centerZ - 5), Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(level, new BlockPos(centerX - 6, Y + 1, centerZ), Blocks.SOUL_LANTERN.defaultBlockState());
            set(level, new BlockPos(centerX + 6, Y + 1, centerZ), Blocks.SOUL_LANTERN.defaultBlockState());
        } else {
            set(level, new BlockPos(centerX - 3, Y + 1, centerZ), Blocks.END_ROD.defaultBlockState());
            set(level, new BlockPos(centerX + 3, Y + 1, centerZ), Blocks.END_ROD.defaultBlockState());
        }
    }

    private static void openDoor(ServerLevel level, int x, int z, boolean west) {
        for (int dz = -1; dz <= 1; dz++) for (int dy = 1; dy <= 3; dy++)
            set(level, new BlockPos(x, Y + dy, z + dz), Blocks.AIR.defaultBlockState());
        int direction = west ? -1 : 1;
        for (int step = 1; step < STEP - 16; step++) for (int dz = -2; dz <= 2; dz++) {
            int px = x + direction * step;
            set(level, new BlockPos(px, Y, z + dz), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            for (int dy = 1; dy <= 5; dy++) {
                BlockState state = Math.abs(dz) == 2
                        ? Blocks.TINTED_GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                set(level, new BlockPos(px, Y + dy, z + dz), state);
            }
            set(level, new BlockPos(px, Y + 6, z + dz), Blocks.TINTED_GLASS.defaultBlockState());
        }
    }

    private static void buildFalsePassage(ServerLevel level, int centerX, int centerZ) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++)
            set(level, new BlockPos(centerX + dx, Y + dy, centerZ - 7), Blocks.AIR.defaultBlockState());
        for (int z = centerZ - 8; z >= centerZ - 14; z--) for (int dx = -2; dx <= 2; dx++) {
            set(level, new BlockPos(centerX + dx, Y, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            for (int dy = 1; dy <= 4; dy++) {
                BlockState state = Math.abs(dx) == 2
                        ? Blocks.TINTED_GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                set(level, new BlockPos(centerX + dx, Y + dy, z), state);
            }
            set(level, new BlockPos(centerX + dx, Y + 5, z), Blocks.TINTED_GLASS.defaultBlockState());
        }
        for (int dx = -2; dx <= 2; dx++) for (int dy = 1; dy <= 4; dy++)
            set(level, new BlockPos(centerX + dx, Y + dy, centerZ - 14), Blocks.TINTED_GLASS.defaultBlockState());
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, 3);
    }
}