package ru.beyondthebag.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** A blade that binds a defeated creature's essence instead of killing it. */
public final class MimicSoulbladeItem extends SwordItem {
    public MimicSoulbladeItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }
}
