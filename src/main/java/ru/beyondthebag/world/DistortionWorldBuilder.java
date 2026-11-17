package ru.beyondthebag.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.entity.GuardianEntity;
import ru.beyondthebag.registry.ModEntities;

/** Warped corridor expedition with false passages, fractured rooms and a final guardian. */
public final class DistortionWorldBuilder {
    private static final int Y = 72;
    private static final int STEP = 24;
    private DistortionWorldBuilder() {}

    public static BlockPos buildIfNeeded(ServerLevel level, PocketData data,
            PocketData.Expedition expedition, int originX, int originZ) {
        if (!data.isMazeGenerated(expedition.runId())) {
            for (int room = 0; room < 5; room++) {
                int centerX = originX + room * STEP;
                buildRoom(level, centerX, originZ, room);
                if (room == 1) buildFalsePassage(level, centerX, originZ, true);
                if (room == 2) buildFalsePassage(level, centerX, originZ, false);
            }

            BlockPos chestPos = new BlockPos(originX + STEP * 2, Y + 1, originZ + 3);
            set(level, chestPos, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.clearContent();
                chest.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 12));
                chest.setItem(1, new ItemStack(Items.ECHO_SHARD, 2));
                chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 8));
                chest.setChanged();
            }

            boolean bossExists = !level.getEntitiesOfClass(GuardianEntity.class,
                    new AABB(originX + STEP * 3 - 8, Y - 2, originZ - 8,
                            originX + STEP * 3 + 8, Y + 12, originZ + 8),
                    GuardianEntity::isExpeditionBoss).isEmpty();
            if (!bossExists) {
                GuardianEntity boss = ModEntities.GUARDIAN.get().create(level);
                if (boss != null) {
                    boss.setExpeditionBoss(true);
                    boss.setCustomName(net.minecraft.network.chat.Component.literal("Страж искажённого пространства"));
                    boss.moveTo(originX + STEP * 3 + 0.5D, Y + 1, originZ + 0.5D, 180.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                }
            }

            set(level, gatePosition(originX, originZ).below(), Blocks.PURPUR_BLOCK.defaultBlockState());
            data.markMazeGenerated(expedition.runId());
        }
        return new BlockPos(originX, Y + 1, originZ);
    }

    public static BlockPos gatePosition(int originX, int originZ) {
        return new BlockPos(originX + STEP * 4, Y + 1, originZ);
    }

    private static void buildRoom(ServerLevel level, int centerX, int centerZ, int index) {
        BlockState[] floor = {
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                Blocks.PURPUR_BLOCK.defaultBlockState()
        };
        BlockState wall = index == 4 ? Blocks.PURPUR_PILLAR.defaultBlockState()
                : index % 2 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();

        for (int dx = -7; dx <= 7; dx++) for (int dz = -6; dz <= 6; dz++) {
            BlockState tile = ((dx + dz + index) & 1) == 0
                    ? floor[index] : (index == 2 ? Blocks.SCULK.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            set(level, new BlockPos(centerX + dx, Y, centerZ + dz), tile);
            boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 6;
            for (int dy = 1; dy <= 5; dy++)
                set(level, new BlockPos(centerX + dx, Y + dy, centerZ + dz),
                        edge ? wall : Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(centerX + dx, Y + 6, centerZ + dz),
                    index % 2 == 0 ? Blocks.TINTED_GLASS.defaultBlockState() : wall);
        }

        if (index > 0) openDoor(level, centerX - 7, centerZ, true);
        if (index < 4) openDoor(level, centerX + 7, centerZ, false);

        if (index == 0) {
            set(level, new BlockPos(centerX - 3, Y + 1, centerZ), Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(level, new BlockPos(centerX + 3, Y + 1, centerZ), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        } else if (index == 1 || index == 2) {
            set(level, new BlockPos(centerX, Y + 1, centerZ), Blocks.SCULK_CATALYST.defaultBlockState());
            set(level, new BlockPos(centerX, Y + 4, centerZ), Blocks.END_ROD.defaultBlockState());
        } else if (index == 3) {
            set(level, new BlockPos(centerX - 4, Y + 1, centerZ - 3), Blocks.SOUL_LANTERN.defaultBlockState());
            set(level, new BlockPos(centerX + 4, Y + 1, centerZ + 3), Blocks.SOUL_LANTERN.defaultBlockState());
        } else {
            set(level, new BlockPos(centerX, Y + 1, centerZ - 3), Blocks.PURPUR_PILLAR.defaultBlockState());
        }
    }

    private static void openDoor(ServerLevel level, int x, int z, boolean west) {
        for (int dz = -1; dz <= 1; dz++) for (int dy = 1; dy <= 3; dy++)
            set(level, new BlockPos(x, Y + dy, z + dz), Blocks.AIR.defaultBlockState());
        int direction = west ? -1 : 1;
        for (int step = 1; step < STEP - 14; step++) for (int dz = -2; dz <= 2; dz++) {
            int px = x + direction * step;
            set(level, new BlockPos(px, Y, z + dz), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            for (int dy = 1; dy <= 5; dy++)
                set(level, new BlockPos(px, Y + dy, z + dz),
                        Math.abs(dz) == 2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                : Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(px, Y + 6, z + dz), Blocks.TINTED_GLASS.defaultBlockState());
        }
    }

    private static void buildFalsePassage(ServerLevel level, int centerX, int centerZ, boolean north) {
        int direction = north ? -1 : 1;
        int wallZ = centerZ + direction * 6;
        for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++)
            set(level, new BlockPos(centerX + dx, Y + dy, wallZ), Blocks.AIR.defaultBlockState());
        for (int step = 1; step <= 7; step++) for (int dx = -2; dx <= 2; dx++) {
            int z = wallZ + direction * step;
            set(level, new BlockPos(centerX + dx, Y, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            for (int dy = 1; dy <= 4; dy++)
                set(level, new BlockPos(centerX + dx, Y + dy, z),
                        Math.abs(dx) == 2 ? Blocks.TINTED_GLASS.defaultBlockState()
                                : Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(centerX + dx, Y + 5, z), Blocks.TINTED_GLASS.defaultBlockState());
        }
        int endZ = wallZ + direction * 7;
        for (int dx = -2; dx <= 2; dx++) for (int dy = 1; dy <= 4; dy++)
            set(level, new BlockPos(centerX + dx, Y + dy, endZ), Blocks.CRYING_OBSIDIAN.defaultBlockState());
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, 3);
    }
}
