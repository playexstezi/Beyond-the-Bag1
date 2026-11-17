package ru.beyondthebag.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.network.MimicNetwork;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MimicKeybinds {
    public static final KeyMapping USE_ABILITY = new KeyMapping(
            "key.beyondthebag.use_mimic_ability",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.beyondthebag");
    private MimicKeybinds() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(USE_ABILITY);
    }

    @Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID, value = Dist.CLIENT)
    public static final class InputHandler {
        private InputHandler() {}
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (USE_ABILITY.consumeClick()) MimicNetwork.requestAbility();
        }
    }
}
