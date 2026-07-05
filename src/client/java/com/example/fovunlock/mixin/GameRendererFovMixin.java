package com.example.fovunlock.mixin;

import com.example.fovunlock.FovUnlockClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GameRenderer#renderLevel builds the world's perspective projection matrix each
 * frame by calling getBasicProjectionMatrix(float fovDegrees). We intercept that
 * single float argument right before it's used, and substitute our own value
 * whenever the mod's override is toggled on. Vanilla's own FOV calculation
 * (sprint FOV, zoom, nausea wobble, the 30-110 options slider, etc.) is left
 * completely untouched - we only ever replace the final number.
 *
 * NOTE: Minecraft 26.1 rewrote large parts of the renderer (see the "extraction
 * vs. render phase" split). If a future 26.x patch renames renderLevel or
 * getBasicProjectionMatrix, this mixin will fail to apply and the game will log
 * an error naming the missing target - open GameRenderer in your IDE (Loom's
 * generated sources) and update the "method" / "target" strings below to match.
 */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {

    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;"
            )
    )
    private float fovunlock$overrideFov(float fovDegrees) {
        return FovUnlockClient.getEffectiveFov(fovDegrees);
    }
}
