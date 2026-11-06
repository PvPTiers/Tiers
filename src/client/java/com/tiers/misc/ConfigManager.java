package com.tiers.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("Tiers.json");
    private static final Config DEFAULT_CONFIG = new Config();
    private static Config config;
    private static String version;
    private static boolean upgradeAdjustmentDone;
    private static int launchTickCounter;

    static {
        FabricLoader.getInstance().getModContainer("tiers").ifPresent(tiers -> version = tiers.getMetadata().getVersion().getFriendlyString());
    }

    private static class Config {
        boolean toggleMod = TiersClient.toggleMod;
        boolean toggleRegion = TiersClient.toggleRegion;
        boolean togglePeak = TiersClient.togglePeak;
        boolean toggleIcons = TiersClient.toggleIcons;
        boolean toggleTab = TiersClient.toggleTab;
        boolean toggleChat = TiersClient.toggleChat;
        boolean toggleAdaptiveSeparator = TiersClient.toggleAdaptiveSeparator;
        boolean toggleAutoKitDetect = TiersClient.toggleAutoKitDetect;
        TiersClient.ModesTierDisplay displayMode = TiersClient.displayMode;
        Icons.Type activeIcons = TiersClient.activeIcons;

        TiersClient.DisplayStatus positionMCTiers = TiersClient.positionMCTiers;
        Mode activeMCTiersMode = TiersClient.activeMCTiersMode;

        TiersClient.DisplayStatus positionPvPTiers = TiersClient.positionPvPTiers;
        Mode activePvPTiersMode = TiersClient.activePvPTiersMode;

        TiersClient.DisplayStatus positionSubtiers = TiersClient.positionSubtiers;
        Mode activeSubtiersMode = TiersClient.activeSubtiersMode;

        String version;
    }

    public static void loadConfig() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                config = GSON.fromJson(fileReader, Config.class);
                if (config == null)
                    restoreFromClient();
            } catch (IOException | JsonSyntaxException ignored) {
                restoreFromClient();
            }
        } else
            restoreFromClient();

        TiersClient.toggleMod = config.toggleMod;
        TiersClient.toggleRegion = config.toggleRegion;
        TiersClient.togglePeak = config.togglePeak;
        TiersClient.toggleIcons = config.toggleIcons;
        TiersClient.toggleTab = config.toggleTab;
        TiersClient.toggleChat = config.toggleChat;
        TiersClient.toggleAdaptiveSeparator = config.toggleAdaptiveSeparator;
        TiersClient.toggleAutoKitDetect = config.toggleAutoKitDetect;

        if (Arrays.stream(TiersClient.ModesTierDisplay.values()).toList().contains(config.displayMode))
            TiersClient.displayMode = config.displayMode;

        if (Arrays.stream(Icons.Type.values()).toList().contains(config.activeIcons))
            TiersClient.activeIcons = config.activeIcons;

        if (Arrays.stream(TiersClient.DisplayStatus.values()).toList().contains(config.positionMCTiers))
            TiersClient.positionMCTiers = config.positionMCTiers;
        if (Arrays.stream(Mode.values()).toList().contains(config.activeMCTiersMode) && config.activeMCTiersMode.toString().contains("MCTIERS"))
            TiersClient.activeMCTiersMode = config.activeMCTiersMode;

        if (Arrays.stream(TiersClient.DisplayStatus.values()).toList().contains(config.positionPvPTiers))
            TiersClient.positionPvPTiers = config.positionPvPTiers;
        if (Arrays.stream(Mode.values()).toList().contains(config.activePvPTiersMode) && config.activePvPTiersMode.toString().contains("PVPTIERS"))
            TiersClient.activePvPTiersMode = config.activePvPTiersMode;

        if (Arrays.stream(TiersClient.DisplayStatus.values()).toList().contains(config.positionSubtiers))
            TiersClient.positionSubtiers = config.positionSubtiers;
        if (Arrays.stream(Mode.values()).toList().contains(config.activeSubtiersMode) && config.activeSubtiersMode.toString().contains("SUBTIERS"))
            TiersClient.activeSubtiersMode = config.activeSubtiersMode;

        if (config.version == null) {
            ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
                if (upgradeAdjustmentDone)
                    return;

                if (minecraftClient.currentScreen instanceof TitleScreen) {
                    launchTickCounter++;

                    if (launchTickCounter >= 20) {
                        minecraftClient.getToastManager().add(SystemToast.create(minecraftClient, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Thanks for updating Tiers"), Text.of("Some settings may have changed")));
                        TiersClient.toggleMod = true;
                        TiersClient.toggleRegion = true;
                        TiersClient.togglePeak = true;
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

        TiersClient.LOGGER.warn("Broken config file: Tiers has restored values from the client memory");

        saveConfig();
    }

    public static void saveConfig() {
        File file = CONFIG_PATH.toFile();
        Config config = new Config();

        updateConfig(config);

        CompletableFuture.runAsync(() -> {
            try (FileWriter fileWriter = new FileWriter(file)) {
                GSON.toJson(config, fileWriter);
            } catch (IOException ignored) {
                restoreFromClient();
            } finally {
                TiersClient.updateAllTags();
            }
        });
    }

    private static void updateConfig(Config config) {
        config.toggleMod = TiersClient.toggleMod;
        config.toggleRegion = TiersClient.toggleRegion;
        config.togglePeak = TiersClient.togglePeak;
        config.toggleIcons = TiersClient.toggleIcons;
        config.toggleTab = TiersClient.toggleTab;
        config.toggleChat = TiersClient.toggleChat;
        config.toggleAdaptiveSeparator = TiersClient.toggleAdaptiveSeparator;
        config.toggleAutoKitDetect = TiersClient.toggleAutoKitDetect;
        config.displayMode = TiersClient.displayMode;
        config.activeIcons = TiersClient.activeIcons;

        config.positionMCTiers = TiersClient.positionMCTiers;
        config.activeMCTiersMode = TiersClient.activeMCTiersMode;

        config.positionPvPTiers = TiersClient.positionPvPTiers;
        config.activePvPTiersMode = TiersClient.activePvPTiersMode;

        config.positionSubtiers = TiersClient.positionSubtiers;
        config.activeSubtiersMode = TiersClient.activeSubtiersMode;

        config.version = version;
    }

    public static void resetToDefaults() {
        TiersClient.toggleMod = DEFAULT_CONFIG.toggleMod;
        TiersClient.toggleRegion = DEFAULT_CONFIG.toggleRegion;
        TiersClient.togglePeak = DEFAULT_CONFIG.togglePeak;
        TiersClient.toggleIcons = DEFAULT_CONFIG.toggleIcons;
        TiersClient.toggleTab = DEFAULT_CONFIG.toggleTab;
        TiersClient.toggleChat = DEFAULT_CONFIG.toggleChat;
        TiersClient.toggleAdaptiveSeparator = DEFAULT_CONFIG.toggleAdaptiveSeparator;
        TiersClient.toggleAutoKitDetect = DEFAULT_CONFIG.toggleAutoKitDetect;
        TiersClient.displayMode = DEFAULT_CONFIG.displayMode;
        TiersClient.activeIcons = DEFAULT_CONFIG.activeIcons;

        TiersClient.positionMCTiers = DEFAULT_CONFIG.positionMCTiers;
        TiersClient.activeMCTiersMode = DEFAULT_CONFIG.activeMCTiersMode;

        TiersClient.positionPvPTiers = DEFAULT_CONFIG.positionPvPTiers;
        TiersClient.activePvPTiersMode = DEFAULT_CONFIG.activePvPTiersMode;

        TiersClient.positionSubtiers = DEFAULT_CONFIG.positionSubtiers;
        TiersClient.activeSubtiersMode = DEFAULT_CONFIG.activeSubtiersMode;

        config = DEFAULT_CONFIG;
        saveConfig();
    }

    public static String getCurrentConfig() {
        return "\nConfig{" +
                "\ntoggleMod=" + config.toggleMod +
                "\ntoggleRegion=" + config.toggleRegion +
                "\ntogglePeak=" + config.togglePeak +
                "\ntoggleIcons=" + config.toggleIcons +
                "\ntoggleTab=" + config.toggleTab +
                "\ntoggleChat=" + config.toggleChat +
                "\ntoggleAdaptiveSeparator=" + config.toggleAdaptiveSeparator +
                "\ntoggleAutoKitDetect=" + config.toggleAutoKitDetect +
                "\ndisplayMode=" + config.displayMode +
                "\nactiveIcons=" + config.activeIcons +
                "\npositionMCTiers=" + config.positionMCTiers +
                "\nactiveMCTiersMode=" + config.activeMCTiersMode +
                "\npositionPvPTiers=" + config.positionPvPTiers +
                "\nactivePvPTiersMode=" + config.activePvPTiersMode +
                "\npositionSubtiers=" + config.positionSubtiers +
                "\nactiveSubtiersMode=" + config.activeSubtiersMode +
                "\nversion=" + config.version +
                "\n}";
    }
}