package com.tiers.textures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tiers.PlayerProfileQueue;
import com.tiers.TiersClient;
import com.tiers.profile.PlayerProfile;
import com.tiers.screens.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.tiers.TiersClient.LOGGER;

public class ColorLoader implements ResourceReloader {
    public static Identifier identifier = Identifier.of("minecraft", "colors/pvptiers.json");

    @Override
    public CompletableFuture<Void> reload(Store store, Executor prepareExecutor, Synchronizer reloadSynchronizer, Executor applyExecutor) {
        if (store.getResourceManager().getResource(identifier).isPresent()) {
            try {
                ColorControl.updateColors(JsonHelper.deserialize(new Gson(), new InputStreamReader(store.getResourceManager().getResource(identifier).get().getInputStream(), StandardCharsets.UTF_8), JsonObject.class));
                TiersClient.restyleAllTexts(TiersClient.playerProfiles);
                TiersClient.updateAllTags();
            } catch (IOException ignored) {
                LOGGER.warn("Error loading colors info");
            }
        }

        if (ConfigScreen.ownProfile == null) {
            ConfigScreen.ownProfile = new PlayerProfile(MinecraftClient.getInstance().getGameProfile().name(), false);
            PlayerProfileQueue.putFirstInQueue(ConfigScreen.ownProfile);

            String defaultProfileMojang = loadStringFromResources("json/defaultProfileMojang.json");
            String defaultProfilePvPTiers = loadStringFromResources("json/defaultProfilePvPTiers.json");

            ConfigScreen.defaultProfile = new PlayerProfile(defaultProfileMojang, defaultProfilePvPTiers);

        } else {
            ArrayList<PlayerProfile> configProfiles = new ArrayList<>();
            configProfiles.add(ConfigScreen.defaultProfile);
            configProfiles.add(ConfigScreen.ownProfile);
            TiersClient.restyleAllTexts(configProfiles);
        }

        return CompletableFuture.runAsync(() -> {
        }, prepareExecutor).thenCompose(reloadSynchronizer::whenPrepared).thenRunAsync(() -> {
        }, applyExecutor);
    }

    private static String loadStringFromResources(String path) {
        try (InputStream inputStream = ColorLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append(System.lineSeparator());
                    }

                    return stringBuilder.toString();
                }
            }
        } catch (IOException ignored) {
            LOGGER.warn("Error loading default jsons");
        }

        return "";
    }
}