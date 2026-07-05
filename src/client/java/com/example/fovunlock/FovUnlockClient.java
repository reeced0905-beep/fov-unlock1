package com.example.fovunlock;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for the FOV Unlock mod.
 *
 * What this mod does:
 *  - Registers a rebindable key (Options > Controls > Key Binds > FOV Unlock).
 *  - Pressing it toggles between your normal vanilla FOV and a custom FOV
 *    that you set yourself, which is allowed to go above the vanilla 110 cap.
 *  - The custom FOV value is set with the "/fov <value>" client-side command
 *    and is remembered between game sessions.
 *
 * The actual override happens in {@link com.example.fovunlock.mixin.GameRendererFovMixin}.
 */
public class FovUnlockClient implements ClientModInitializer {

    public static final String MOD_ID = "fovunlock";

    private static FovConfig config;
    private static volatile boolean overrideActive = false;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        config = FovConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fovunlock.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F10, // change the default here, or rebind in-game
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                overrideActive = !overrideActive;
                if (client.player != null) {
                    String message = overrideActive
                            ? "FOV override ON (" + config.customFov + ")"
                            : "FOV override OFF";
                    client.player.displayClientMessage(Component.literal(message), true);
                }
            }
        });

        registerCommand();
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fov")
                        .then(ClientCommandManager.argument(
                                        "value",
                                        IntegerArgumentType.integer(FovConfig.MIN_FOV, FovConfig.MAX_FOV))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    config.setCustomFov(value);
                                    context.getSource().sendFeedback(Component.literal(
                                            "Custom FOV set to " + value
                                                    + ". Press your bound key to switch to it."
                                    ));
                                    return 1;
                                }))
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                    "Custom FOV is " + config.customFov
                                            + " (currently " + (overrideActive ? "ACTIVE" : "inactive") + "). "
                                            + "Use /fov <" + FovConfig.MIN_FOV + "-" + FovConfig.MAX_FOV + "> to change it."
                            ));
                            return 1;
                        })
                )
        );
    }

    public static boolean isOverrideActive() {
        return overrideActive;
    }

    public static float getCustomFov() {
        return config.customFov;
    }

    /**
     * Called from the mixin every frame the world's projection matrix is built.
     */
    public static float getEffectiveFov(float vanillaFovDegrees) {
        return overrideActive ? getCustomFov() : vanillaFovDegrees;
    }
}
