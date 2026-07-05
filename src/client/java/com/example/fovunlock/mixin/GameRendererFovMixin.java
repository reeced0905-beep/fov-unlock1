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
 * UPDATE: targeting the exact descriptor "getFov(Camera,float,boolean):float"
 * failed on 26.1.2 - Mixin reported no method matching that exact signature.
 * The name "getFov" may still be correct while the parameter list or return
 * type changed (e.g. a raw float tick delta getting replaced by a
 * RenderTickCounter object, which happened to several other methods on this
 * class around 1.21.6-1.21.11).
 *
 * So below we target the method by NAME ONLY (no descriptor). Mixin will then
 * resolve whichever "getFov" overload actually exists, as long as there's
 * only one. If our handler method's parameter types below still don't match
 * the real ones, the next crash log will spell out the actual expected
 * signature explicitly (rather than just "0 targets found") - paste that back
 * and the fix becomes a one-line change instead of another guess.
 *
 * You can also just check directly and skip the guessing entirely: go to
 * https://mcsrc.dev, pick 26.1.2, open
 * net/minecraft/client/renderer/GameRenderer.java, and search for "getFov".
 * That takes about 30 seconds and tells us the exact real signature.
 */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {

    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true
    )
    private void fovunlock$overrideFov(Camera camera, float tickDelta, boolean changingFov,
                                        CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FovUnlockClient.getEffectiveFov(cir.getReturnValueF()));
    }

    /*
     * If mcsrc.dev (or the next crash log) shows getFov returning `double`
     * instead of `float`, delete the method above and use this one instead:
     *
     * @Inject(
     *         method = "getFov",
     *         at = @At("RETURN"),
     *         cancellable = true
     * )
     * private void fovunlock$overrideFov(Camera camera, float tickDelta, boolean changingFov,
     *                                     CallbackInfoReturnable<Double> cir) {
     *     cir.setReturnValue((double) FovUnlockClient.getEffectiveFov((float) cir.getReturnValueD()));
     * }
     *
     * If the crash log instead shows a completely different parameter list
     * (e.g. a RenderTickCounter instead of a raw float), paste that log back
     * and I'll match the handler method to it exactly.
     */
}
