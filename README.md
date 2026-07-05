# FOV Unlock (Fabric, Minecraft 26.1.2)

A tiny client-side Fabric mod:

- A rebindable key (default **F10**, change it any time in *Options > Controls >
  Key Binds > FOV Unlock*) toggles between your normal vanilla FOV and a custom
  FOV that **you** choose, which is allowed to go past the vanilla 110 cap.
- The custom target FOV is set in-game with `/fov <value>` (30-170 by default)
  and is remembered between sessions in `config/fovunlock.json`.
- Press the key again to snap back to your normal FOV.

## Why 26.1.2 modding looks different now

Minecraft 26.1 dropped obfuscation entirely and Fabric dropped Yarn mappings in
favor of Mojang's own official names, and Loom no longer remaps anything. That
means this project:

- Uses the `net.fabricmc.fabric-loom` plugin (not `fabric-loom`/`fabric-loom-remap`).
- Has **no** `yarn_mappings` or `mappings loom.officialMojangMappings()` line -
  there's nothing left to remap.
- Uses `implementation` everywhere instead of `modImplementation`.
- Targets Java 25.
- Uses Mojang's real class names directly, e.g. `net.minecraft.client.KeyMapping`,
  `net.minecraft.client.renderer.GameRenderer`, and `net.minecraft.resources.Identifier`
  (Mojang renamed `ResourceLocation` to `Identifier` as of 1.21.11).
- Uses Fabric API's renamed `KeyMappingHelper` (it used to be `KeyBindingHelper`).

## How the FOV override actually works

Rather than hacking at the vanilla FOV *slider* (which lives deep in `Options`/
`OptionInstance` and is the part of the game 26.1's rendering rewrite is least
likely to have left untouched), this mod hooks the one place every camera
FOV value funnels through right before it becomes a projection matrix:
`GameRenderer#getBasicProjectionMatrix(float)`. A mixin swaps out that single
float argument for your custom value whenever the override is active. Vanilla's
own FOV math (zoom, sprint FOV, nausea, the 30-110 slider) is never touched -
we only replace the final number for that one frame.

**Heads up:** 26.1 rewrote large chunks of the renderer (the "extraction vs.
render phase" split, `CameraRenderState`, etc.). I can't compile or launch the
game from here to verify byte-for-byte against 26.1.2, so if `renderLevel` or
`getBasicProjectionMatrix` were renamed in a later 26.1.x patch, the game will
print a mixin error on startup naming the missing target. If that happens:

1. Run the `genSources` Gradle task (or just open the class in your IDE) to see
   `GameRenderer`'s real current method names.
2. Update the `method = "..."` and `target = "..."` strings in
   `GameRendererFovMixin.java` to match.

This is a completely normal part of modding right after a big Minecraft
rewrite - it's not a sign anything else here is wrong.

## Building without installing anything

This project has **not** been compiled or tested here - I don't have internet
access in this sandbox to download Minecraft/Gradle/Fabric artifacts. If you
don't want to install a JDK or Gradle yourself, `.github/workflows/build.yml`
lets GitHub build it for you for free:

1. Create a free account at https://github.com if you don't have one.
2. Create a new **empty** repository (no README/license/gitignore).
3. On the repo's main page, click **Add file > Upload files**, then drag the
   *entire unzipped `fov-unlock-mod` folder* onto the page (dragging a whole
   folder works and keeps the subfolders intact). Commit the upload.
4. Click the **Actions** tab. A "Build mod" run should start automatically
   (or click **Run workflow** if it doesn't). It installs JDK 25 and Gradle on
   GitHub's servers and runs the real build - typically 5-10 minutes the first
   time, since Loom has to download Minecraft itself.
5. When it finishes (green check), open the run, scroll to **Artifacts**, and
   download `fov-unlock-mod-jar`. Unzip that and you have your `.jar`.
6. Drop the jar into `.minecraft/mods` alongside Fabric API
   `0.153.0+26.1.2` (or newer) and Fabric Loader for 26.1.2.

Nothing is installed on your own machine in this path - only your browser is
involved. If a step in the workflow fails, the Actions log will show exactly
which command failed and why, which is also the easiest way to catch it if
the mixin target mentioned above needs adjusting for a newer 26.1.x patch.

### Building locally instead

If you'd rather build on your own PC: install a JDK 25 (e.g. Temurin) and
open the folder in IntelliJ IDEA, which will offer to import the Gradle
project and download everything (including a Gradle wrapper) automatically.
Running `./gradlew build` isn't possible straight out of this zip because it
doesn't include the Gradle wrapper jar (a small binary file I can't generate
without internet access) - IntelliJ's Gradle import handles that for you, or
you can run `gradle wrapper` once if you already have Gradle installed.

The output jar ends up in `build/libs/`. If versions have moved on by the
time you build this, check the current recommended numbers at
https://fabricmc.net/develop/ and update `gradle.properties` accordingly.

## Files

```
build.gradle
gradle.properties
settings.gradle
src/main/resources/fabric.mod.json
src/main/resources/assets/fovunlock/lang/en_us.json
src/client/resources/fovunlock.mixins.json
src/client/java/com/example/fovunlock/FovUnlockClient.java   - key bind, tick loop, /fov command
src/client/java/com/example/fovunlock/FovConfig.java          - persisted custom FOV value
src/client/java/com/example/fovunlock/mixin/GameRendererFovMixin.java - the actual override
```
