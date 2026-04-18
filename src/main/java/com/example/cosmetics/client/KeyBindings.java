package com.example.cosmetics.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final KeyBinding OPEN_MENU = new KeyBinding(
            "key.cosmeticsmod.open_menu",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.cosmeticsmod"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_MENU);
    }
}
