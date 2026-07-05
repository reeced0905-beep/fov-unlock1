package com.example.fovunlock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Very small JSON-backed config so the desired FOV survives a game restart.
 * Stored at .minecraft/config/fovunlock.json
 */
public class FovConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("fovunlock.json");

    // Vanilla's slider caps at 110. This mod intentionally allows going higher.
    // 170 is a practical ceiling to avoid the near-180 degree "fisheye flip".
    public static final int MIN_FOV = 30;
    public static final int MAX_FOV = 170;
    private static final int DEFAULT_FOV = 130;

    public int customFov = DEFAULT_FOV;

    public static FovConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                FovConfig loaded = GSON.fromJson(reader, FovConfig.class);
                if (loaded != null) {
                    loaded.customFov = clamp(loaded.customFov);
                    return loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        FovConfig fresh = new FovConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCustomFov(int value) {
        this.customFov = clamp(value);
        save();
    }

    public static int clamp(int value) {
        return Math.max(MIN_FOV, Math.min(MAX_FOV, value));
    }
}
