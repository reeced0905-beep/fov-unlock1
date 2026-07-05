package com.example.fovunlock.mixin;

import com.example.fovunlock.FovUnlockClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WHY THIS CHANGED FROM THE ORIGINAL VERSION:
 *
 * The old mixin used @ModifyArg to intercept a call to
 * GameRenderer#getBasicProjectionMatrix(float) inside a method named
 * "renderLevel". Neither of those identifiers exist in Minecraft 26.1.2:
 *
 *   - "getBasicProjectionMatrix" was always a Yarn-mappings name. Mojang's own
 *     official name has long been "getProjectionMatrix", and as of the 1.21.6
 *     GPU-buffer rendering rewrite (which 26.1 builds on), that method itself
 *     was restructured again into buffer-based code that no longer just takes
 *     a float and returns a Matrix4f.
 *   - 26.1 is the first Minecraft version shipped fully unobfuscated, with no
 *     Yarn mappings at all - so a Yarn-only symbol can never resolve.
 *   - "renderLevel" has historically lived on the level/world renderer, not
 *     GameRenderer, in every version checked.
 *
 * Any one of those is enough to make Mixin scan 0 targets and crash on launch.
 *
 * THE FIX: hook GameRenderer#getFov(Camera, float, boolean) instead. This
 * private method computes the plain FOV-in-degrees float BEFORE it is handed
 * off to whatever internal matrix/buffer machinery renders the frame, and it
 * has kept this same name and rough signature since at least 1.16 through the
 * most recent confirmed Yarn build (1.21.11) - including on mods actively
 * maintained for 1.21.10 today. That makes it a far more stable injection
 * point than whatever consumes its result.
 *
 * ONE THING TO DOUBLE-CHECK: getFov's return type has been `float` in every
 * recent (1.21.x) mapping, but was `double` in older versions. If Minecraft
 * 26.1.2 still returns double, swap every `Float`/`float` marked below for
 * `Double`/`double` (see the commented block at the bottom). You can confirm
 * in about 10 seconds at https://mcsrc.dev - pick 26.1.2, open
 * net/minecraft/client/renderer/GameRenderer.java, and search for "getFov".
 */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {

    @Inject(
            method = "getFov(Lnet/minecraft/client/Camera;FZ)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void fovunlock$overrideFov(Camera camera, float tickDelta, boolean changingFov,
                                        CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FovUnlockClient.getEffectiveFov(cir.getReturnValueF()));
    }

    /*
     * If mcsrc.dev shows getFov returning `double` instead of `float` on
     * 26.1.2, delete the method above and use this one instead:
     *
     * @Inject(
     *         method = "getFov(Lnet/minecraft/client/Camera;FZ)D",
     *         at = @At("RETURN"),
     *         cancellable = true
     * )
     * private void fovunlock$overrideFov(Camera camera, float tickDelta, boolean changingFov,
     *                                     CallbackInfoReturnable<Double> cir) {
     *     cir.setReturnValue((double) FovUnlockClient.getEffectiveFov((float) cir.getReturnValueD()));
     * }
     */
}
