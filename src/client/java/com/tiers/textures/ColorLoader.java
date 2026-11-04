package com.tiers.textures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tiers.PlayerProfileQueue;
import com.tiers.TiersClient;
import com.tiers.profile.PlayerProfile;
import com.tiers.screens.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ColorLoader implements PreparableReloadListener {
    public static Identifier identifier = Identifier.fromNamespaceAndPath("minecraft", "colors/pvptiers.json");

    @Override
    public @NonNull CompletableFuture<Void> reload(SharedState currentReload, @NonNull Executor taskExecutor, @NonNull PreparationBarrier preparationBarrier, @NonNull Executor reloadExecutor) {
        if (currentReload.resourceManager().getResource(identifier).isPresent()) {
            try {
                ColorControl.updateColors(GsonHelper.fromJson(new Gson(), new InputStreamReader(currentReload.resourceManager().getResource(identifier).get().open(), StandardCharsets.UTF_8), JsonObject.class));
                TiersClient.restyleAllTexts(TiersClient.playerProfiles);
                TiersClient.updateAllTags();
            } catch (IOException ignored) {
                TiersClient.LOGGER.warn("Error loading colors info");
            }
        }

        if (ConfigScreen.ownProfile == null) {
            ConfigScreen.ownProfile = new PlayerProfile(Minecraft.getInstance().getGameProfile().name(), false);
            PlayerProfileQueue.putFirstInQueue(ConfigScreen.ownProfile);

            String defaultProfileMojang = loadStringFromResources("json/defaultProfileMojang.json");
            String defaultProfileMCTiers = loadStringFromResources("json/defaultProfileMCTiers.json");
            String defaultProfilePvPTiers = loadStringFromResources("json/defaultProfilePvPTiers.json");
            String defaultProfileSubtiers = loadStringFromResources("json/defaultProfileSubtiers.json");

            ConfigScreen.defaultProfile = new PlayerProfile(defaultProfileMojang,
                    defaultProfileMCTiers,
                    defaultProfilePvPTiers,
                    defaultProfileSubtiers);

        } else {
            ArrayList<PlayerProfile> configProfiles = new ArrayList<>();
            configProfiles.add(ConfigScreen.defaultProfile);
            configProfiles.add(ConfigScreen.ownProfile);
            TiersClient.restyleAllTexts(configProfiles);
        }

        return CompletableFuture.runAsync(() -> {
        }, taskExecutor).thenCompose(preparationBarrier::wait).thenRunAsync(() -> {
        }, reloadExecutor);
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
            TiersClient.LOGGER.warn("Error loading default jsons");
        }

        return "";
    }
}