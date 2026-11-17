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

/** Four-room combat dungeon with a Guardian boss and a locked exit. */
public final class DungeonWorldBuilder {
    private static final int Y = 72;
    private static final int STEP = 22;
    private DungeonWorldBuilder() {}

    public static BlockPos buildIfNeeded(ServerLevel level, PocketData data,
            PocketData.Expedition expedition, int ox, int oz) {
        if (!data.isMazeGenerated(expedition.runId())) {
            for (int room = 0; room < 4; room++) buildRoom(level, ox + room * STEP, oz, room);
            BlockPos chestPos = new BlockPos(ox + STEP * 2, Y + 1, oz + 3);
            set(level, chestPos, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.clearContent();
                chest.setItem(0, new ItemStack(Items.GOLDEN_APPLE, 2));
                chest.setItem(1, new ItemStack(Items.IRON_INGOT, 12));
                chest.setItem(2, new ItemStack(Items.ENDER_PEARL, 4));
                chest.setChanged();
            }
            boolean exists = !level.getEntitiesOfClass(GuardianEntity.class,
                    new AABB(ox + STEP * 3 - 8, Y - 2, oz - 8,
                            ox + STEP * 3 + 8, Y + 12, oz + 8),
                    GuardianEntity::isExpeditionBoss).isEmpty();
            if (!exists) {
                GuardianEntity boss = ModEntities.GUARDIAN.get().create(level);
                if (boss != null) {
                    boss.setExpeditionBoss(true);
                    boss.setCustomName(net.minecraft.network.chat.Component.literal("Страж забытого данжа"));
                    boss.moveTo(ox + STEP * 3 + 0.5D, Y + 1, oz + 0.5D, 180.0F, 0.0F);
                    boss.setPersistenceRequired();
                    level.addFreshEntity(boss);
                }
            }
            set(level, gatePosition(ox, oz).below(), Blocks.PURPUR_BLOCK.defaultBlockState());
            data.markMazeGenerated(expedition.runId());
        }
        return new BlockPos(ox, Y + 1, oz);
    }

    public static BlockPos gatePosition(int ox, int oz) {
        return new BlockPos(ox + STEP * 4, Y + 1, oz);
    }

    private static void buildRoom(ServerLevel level, int cx, int cz, int index) {
        BlockState floor = index == 3 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                : Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState wall = index == 3 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        for (int dx = -7; dx <= 7; dx++) for (int dz = -6; dz <= 6; dz++) {
            set(level, new BlockPos(cx + dx, Y, cz + dz), floor);
            boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 6;
            for (int dy = 1; dy <= 5; dy++)
                set(level, new BlockPos(cx + dx, Y + dy, cz + dz), edge ? wall : Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(cx + dx, Y + 6, cz + dz), wall);
        }
        for (int dz = -1; dz <= 1; dz++) for (int dy = 1; dy <= 3; dy++) {
            set(level, new BlockPos(cx + 7, Y + dy, cz + dz), Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(cx - 7, Y + dy, cz + dz), Blocks.AIR.defaultBlockState());
        }
        for (int x = cx + 7; x < cx + STEP - 7; x++) for (int dz = -2; dz <= 2; dz++) {
            set(level, new BlockPos(x, Y, cz + dz), floor);
            for (int dy = 1; dy <= 5; dy++)
                set(level, new BlockPos(x, Y + dy, cz + dz),
                        Math.abs(dz) == 2 ? wall : Blocks.AIR.defaultBlockState());
            set(level, new BlockPos(x, Y + 6, cz + dz), wall);
        }
        if (index == 0) set(level, new BlockPos(cx, Y + 1, cz), Blocks.SOUL_LANTERN.defaultBlockState());
        if (index == 1) set(level, new BlockPos(cx, Y + 1, cz), Blocks.MAGMA_BLOCK.defaultBlockState());
        if (index == 3) {
            set(level, new BlockPos(cx, Y + 1, cz - 3), Blocks.SOUL_FIRE.defaultBlockState());
            set(level, new BlockPos(cx, Y + 1, cz + 3), Blocks.SOUL_FIRE.defaultBlockState());
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, 3);
    }
}
