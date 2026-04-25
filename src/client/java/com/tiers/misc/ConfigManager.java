package com.tiers.misc;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tiers.TiersClient;
import com.tiers.textures.Icons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private static Config config;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("Tiers.json");
    private static String version;
    private static boolean upgradeAdjustmentDone;
    private static int launchTickCounter;

    static {
        FabricLoader.getInstance().getModContainer("tiers").ifPresent(tiers -> version = tiers.getMetadata().getVersion().getFriendlyString());
    }

    private static class Config {
        boolean toggleMod;
        boolean toggleIcons;
        boolean toggleTab;
        boolean toggleChat;
        boolean toggleAdaptiveSeparator;
        boolean toggleAutoKitDetect;
        TiersClient.ModesTierDisplay displayMode;
        Icons.Type activeIcons;

        TiersClient.DisplayStatus positionPvPTiers;
        Mode activePvPTiersMode;

        String version;
    }

    public static void loadConfig() {
        Gson gson = new Gson();
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                config = gson.fromJson(fileReader, Config.class);
                if (config == null)
                    restoreFromClient();
            } catch (IOException | JsonSyntaxException ignored) {
                restoreFromClient();
            }
        } else
            restoreFromClient();

        TiersClient.toggleMod = config.toggleMod;
        TiersClient.toggleIcons = config.toggleIcons;
        TiersClient.toggleTab = config.toggleTab;
        TiersClient.toggleChat = config.toggleChat;
        TiersClient.toggleAdaptiveSeparator = config.toggleAdaptiveSeparator;
        TiersClient.toggleAutoKitDetect = config.toggleAutoKitDetect;

        if (Arrays.stream(TiersClient.ModesTierDisplay.values()).toList().contains(config.displayMode))
            TiersClient.displayMode = config.displayMode;

        if (Arrays.stream(Icons.Type.values()).toList().contains(config.activeIcons))
            TiersClient.activeIcons = config.activeIcons;

        if (Arrays.stream(TiersClient.DisplayStatus.values()).toList().contains(config.positionPvPTiers))
            TiersClient.positionPvPTiers = config.positionPvPTiers;
        if (Arrays.stream(Mode.values()).toList().contains(config.activePvPTiersMode) && config.activePvPTiersMode.toString().contains("PVPTIERS"))
            TiersClient.activePvPTiersMode = config.activePvPTiersMode;

        if (config.version == null) {
            ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
                if (upgradeAdjustmentDone)
                    return;

                if (minecraftClient.currentScreen instanceof TitleScreen) {
                    launchTickCounter++;

                    if (launchTickCounter >= 20) {
                        minecraftClient.getToastManager().add(SystemToast.create(minecraftClient, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Thanks for updating Tiers"), Text.of("Some settings may have changed")));
                        TiersClient.toggleMod = true;
                        TiersClient.toggleIcons = true;
                        TiersClient.toggleTab = true;
                        TiersClient.toggleChat = true;
                        TiersClient.toggleAdaptiveSeparator = true;
                        TiersClient.toggleAutoKitDetect = false;

                        saveConfig();
                        upgradeAdjustmentDone = true;
                    }
                }
            });
        }

        saveConfig();
    }

    private static void restoreFromClient() {
        config = new Config();
        updateConfig(config);

        TiersClient.LOGGER.info("Broken config file: Tiers has restored values from the client memory");

        saveConfig();
    }

    public static void saveConfig() {
        Gson gson = new Gson();
        File file = CONFIG_PATH.toFile();
        Config config = new Config();

        updateConfig(config);

        CompletableFuture.runAsync(() -> {
            try (FileWriter fileWriter = new FileWriter(file)) {
                gson.toJson(config, fileWriter);
            } catch (IOException ignored) {
                restoreFromClient();
            } finally {
                TiersClient.updateAllTags();
            }
        });
    }

    private static void updateConfig(Config config) {
        config.toggleMod = TiersClient.toggleMod;
        config.toggleIcons = TiersClient.toggleIcons;
        config.toggleTab = TiersClient.toggleTab;
        config.toggleChat = TiersClient.toggleChat;
        config.toggleAdaptiveSeparator = TiersClient.toggleAdaptiveSeparator;
        config.toggleAutoKitDetect = TiersClient.toggleAutoKitDetect;
        config.displayMode = TiersClient.displayMode;
        config.activeIcons = TiersClient.activeIcons;

        config.positionPvPTiers = TiersClient.positionPvPTiers;
        config.activePvPTiersMode = TiersClient.activePvPTiersMode;

        config.version = version;
    }

    public static String getCurrentConfig() {
        return "\nConfig{" +
                "\ntoggleMod=" + config.toggleMod +
                "\ntoggleIcons=" + config.toggleIcons +
                "\ntoggleTab=" + config.toggleTab +
                "\ntoggleChat=" + config.toggleChat +
                "\ntoggleAdaptiveSeparator=" + config.toggleAdaptiveSeparator +
                "\ntoggleAutoKitDetect=" + config.toggleAutoKitDetect +
                "\ndisplayMode=" + config.displayMode +
                "\nactiveIcons=" + config.activeIcons +
                "\npositionPvPTiers=" + config.positionPvPTiers +
                "\nactivePvPTiersMode=" + config.activePvPTiersMode +
                "\nversion=" + config.version +
                "\n}";
    }
}