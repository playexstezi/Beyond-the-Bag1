package ru.beyondthebag.event;

import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;

/**
 * Block imitation is handled exclusively by MimicActionEvents and the delayed
 * queue in MimicEntity. Keeping this subscriber empty prevents instant duplicate
 * edits beside the mimic.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicBuildingEvents {
    private MimicBuildingEvents() {}
}
