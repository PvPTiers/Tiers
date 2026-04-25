package com.tiers;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import com.tiers.misc.CommandRegister;
import com.tiers.misc.ConfigManager;
import com.tiers.misc.Mode;
import com.tiers.mixin.client.DataTrackerAccessor;
import com.tiers.mixin.client.TextDisplayAccessor;
import com.tiers.profile.PlayerProfile;
import com.tiers.profile.Status;
import com.tiers.profile.types.SuperProfile;
import com.tiers.screens.ConfigScreen;
import com.tiers.screens.PlayerSearchResultScreen;
import com.tiers.textures.ColorLoader;
import com.tiers.textures.Icons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class TiersClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(TiersClient.class);
    public static String userAgent = "Tiers (modrinth.com/mod/tiers)";
    public static final CopyOnWriteArrayList<PlayerProfile> playerProfiles = new CopyOnWriteArrayList<>();
    public static final HashMap<String, PlayerProfile> readyPlayerProfiles = new HashMap<>();
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static int cacheVersion;
    public static volatile boolean cachesDirty = false;

    public static boolean toggleMod = true;
    public static boolean toggleIcons = true;
    public static boolean toggleTab = true;
    public static boolean toggleChat = true;
    public static boolean toggleAdaptiveSeparator = true;
    public static boolean toggleAutoKitDetect = false;
    public static ModesTierDisplay displayMode = ModesTierDisplay.ADAPTIVE_HIGHEST;
    public static Icons.Type activeIcons = Icons.Type.PVPTIERS;

    public static DisplayStatus positionPvPTiers = DisplayStatus.LEFT;
    public static Mode activePvPTiersMode = Mode.PVPTIERS_CRYSTAL;

    public static KeyBinding autoDetectKey;
    public static KeyBinding openClosestPlayerProfile;
    public static KeyBinding cycleRightKey;
    public static KeyBinding cycleLeftKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        changeIcons(activeIcons, false);
        clearCache(true);
        CommandRegister.registerCommands();

        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("tiers");

        modContainer.ifPresent(tiers -> {
            ResourceLoader.registerBuiltinPack(Identifier.of("resourcepacks", "tiers-resources"), tiers, Text.of("Resources for Tiers"), PackActivationType.ALWAYS_ENABLED);
            userAgent += " v" + tiers.getMetadata().getVersion().getFriendlyString();
        });

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("tiers"));
        autoDetectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Auto Detect Kit", GLFW.GLFW_KEY_Y, category));
        openClosestPlayerProfile = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open Closest Player Profile", GLFW.GLFW_KEY_H, category));
        cycleRightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Cycle Right Gamemodes", GLFW.GLFW_KEY_I, category));
        cycleLeftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Cycle Left Gamemodes", GLFW.GLFW_KEY_U, category));

        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(Identifier.of("tiers"), new ColorLoader());
        ClientTickEvents.END_CLIENT_TICK.register(TiersClient::checkKeys);
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            if (toggleAutoKitDetect)
                InventoryChecker.checkInventory(minecraftClient, false);
            if (cachesDirty) {
                cachesDirty = false;
                updateCaches();
            }
        });

        LOGGER.info("Tiers initialized | User agent: {}", userAgent);
    }

    public static PlayerProfile addGetPlayer(String playerName, boolean priority) {
        PlayerProfile gotFromReady = readyPlayerProfiles.get(playerName);
        if (gotFromReady != null)
            return gotFromReady;

        for (PlayerProfile playerProfile : playerProfiles) {
            if (playerProfile.targetName.equalsIgnoreCase(playerName)) {
                if (priority)
                    PlayerProfileQueue.changeToFirstInQueue(playerProfile);
                return playerProfile;
            }
        }
        PlayerProfile newProfile = new PlayerProfile(playerName, true);

        if (priority)
            PlayerProfileQueue.putFirstInQueue(newProfile);
        else
            PlayerProfileQueue.enqueue(newProfile);

        playerProfiles.add(newProfile);
        return newProfile;
    }

    public static void updateAllTags() {
        MinecraftClient.getInstance().execute(() -> {
            readyPlayerProfiles.values().forEach(PlayerProfile::updateAppendingText);

            if (ConfigScreen.ownProfile != null && ConfigScreen.defaultProfile != null) {
                ConfigScreen.ownProfile.updateAppendingText();
                ConfigScreen.defaultProfile.updateAppendingText();
            }
            updateCaches();
        });
    }

    public static void restyleAllTexts(List<PlayerProfile> playerProfiles) {
        MinecraftClient.getInstance().execute(() -> {
            playerProfiles.forEach(playerProfile -> {
                if (playerProfile.status == Status.READY) {
                    if (playerProfile.profilePvPTiers.status == Status.READY)
                        playerProfile.profilePvPTiers.parseJson(playerProfile.profilePvPTiers.originalJson);
                }
                playerProfile.updateAppendingText();
            });
            updateCaches();
        });
    }

    public static String getNearestPlayerName() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity self = minecraftClient.player;
        if (self == null || self.getEntityWorld() == null)
            return null;

        PlayerEntity playerEntity = self.getEntityWorld().getPlayers().stream()
                .filter(player -> player != self)
                .filter(player -> self.distanceTo(player) < MinecraftClient.getInstance().gameRenderer.getViewDistanceBlocks())
                .min(Comparator.comparingDouble(self::distanceTo))
                .orElse(null);

        if (playerEntity != null)
            return playerEntity.getNameForScoreboard();
        return null;
    }

    private static void checkKeys(MinecraftClient minecraftClient) {
        if (autoDetectKey.wasPressed())
            InventoryChecker.checkInventory(minecraftClient, true);

        if (openClosestPlayerProfile.wasPressed()) {
            String nearestPlayerName = getNearestPlayerName();
            if (nearestPlayerName != null)
                tiersCommand(nearestPlayerName);
            else
                sendMessageToPlayer(Icons.colorText("No players in render distance", "red"), true);
        }

        if (cycleRightKey.wasPressed()) {
            Text message = cycleRightMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the right display", "red"), true);
        }

        if (cycleLeftKey.wasPressed()) {
            Text message = cycleLeftMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the left display", "red"), true);
        }
    }

    public static Text cycleRightMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return Text.literal("Right (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(Colors.WHITE)).append(cyclePvPTiersMode());

        return null;
    }

    public static Text cycleLeftMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return Text.literal("Left (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(Colors.WHITE)).append(cyclePvPTiersMode());

        return null;
    }

    public static Text getRightIcon() {
        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return activePvPTiersMode.getIcon();

        return Text.empty();
    }

    public static Text getLeftIcon() {
        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return activePvPTiersMode.getIcon();

        return Text.empty();
    }

    public static void sendMessageToPlayer(Text message, boolean overlay) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player != null)
            minecraftClient.execute(() -> minecraftClient.player.sendMessage(message, overlay));
    }

    public static void toggleMod(CommandContext<FabricClientCommandSource> ignoredFabricClientCommandSourceCommandContext) {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
        sendMessageToPlayer(Icons.colorText("Tiers is now " + (toggleMod ? "enabled" : "disabled"), toggleMod ? "green" : "red"), true);
    }

    public static void toggleMod() {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
    }

    public static void toggleIcons() {
        toggleIcons = !toggleIcons;
        ConfigManager.saveConfig();
    }

    public static void toggleTab() {
        toggleTab = !toggleTab;
        ConfigManager.saveConfig();
    }

    public static void toggleChat() {
        toggleChat = !toggleChat;
        ConfigManager.saveConfig();
    }

    public static void toggleAdaptiveSeparator() {
        toggleAdaptiveSeparator = !toggleAdaptiveSeparator;
        ConfigManager.saveConfig();
    }

    public static void toggleAutoKitDetect() {
        toggleAutoKitDetect = !toggleAutoKitDetect;
        ConfigManager.saveConfig();
    }

    public static void tiersCommand(String playerName) {
        if (playerName.equalsIgnoreCase("-toggle"))
            toggleMod(null);
        else if (playerName.equalsIgnoreCase("-config"))
            setScreen(ConfigScreen.getConfigScreen(null));
        else if (playerName.equalsIgnoreCase("-help") || playerName.equalsIgnoreCase("-debug")) {
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("--- Tiers help ---", Colors.YELLOW), false);
            sendMessageToPlayer(Text.literal("- General contact: ").append(Text.literal("flavio6561 on Discord").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://discordapp.com/users/715189608085716992"))))), false);
            sendMessageToPlayer(Text.literal("- Report a bug: ").append(Text.literal("Tiers GitHub issues").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/PvPTiers/Tiers/issues"))))), false);
            sendMessageToPlayer(Text.literal("- It's not advisable to create tickets in PvPTiers support"), false);
            sendMessageToPlayer(Text.literal("- ").append(Text.literal("Changelogs").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/PvPTiers/Tiers/wiki/Version-changelogs"))))), false);
            sendMessageToPlayer(Text.literal("- ").append(Text.literal("Modrinth page").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/tiers"))))), false);

            String[] debugInfo = getDebugInfo();
            sendMessageToPlayer(Icons.colorText("\n" + debugInfo[0], Colors.LIGHT_YELLOW), false);
            MinecraftClient.getInstance().keyboard.setClipboard(debugInfo[1]);

            try (PrintWriter printWriter = new PrintWriter(FabricLoader.getInstance().getGameDir() + "/cache/tiers/debug.log")) {
                printWriter.println("--- Tiers Debug log ---");
                printWriter.println("--- Section 1 ---");
                printWriter.println(debugInfo[0]);
                printWriter.println("--- End of section 1 ---");
                printWriter.println("--- Section 2 ---");
                printWriter.println(debugInfo[1]);
                printWriter.println("--- End of section 2 ---");
                printWriter.println("--- Section 3 ---");
                printWriter.println(debugInfo[2]);
                printWriter.println("--- End of section 3 ---");
                printWriter.println("--- Section 4 ---");
                printWriter.println(debugInfo[3]);
                printWriter.println("--- End ---");
            } catch (IOException ignored) {
                LOGGER.warn("An error occurred while trying to write the debug log");
            }

            sendMessageToPlayer(Icons.colorText("A complete debug log has been copied to the clipboard and saved in your cache folder", "green"), false);
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
        } else if (playerName.equalsIgnoreCase("-clear")) {
            clearCache(false);
            sendMessageToPlayer(Icons.colorText("Cleared player cache", "green"), true);
        } else if (playerName.equalsIgnoreCase("-status")) {
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Player profiles status:", "green"), false);
            sendMessageToPlayer(Icons.colorText("Cached players: " + PlayerProfile.playerProfilesRequests.get() + " (" + PlayerProfile.failedPlayerProfilesRequests.get() + " failed)", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("PvPTiers requests failed: " + SuperProfile.failedPvPTiersRequests + "/" + SuperProfile.PvPTiersRequests + " (failed / requested)", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("PvPTiers status | is down? " + SuperProfile.isPvPTiersDown + " | Failed request in last minute: " + SuperProfile.failedPvPTiersRequestsLastMinute, Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("Tiers will try to recover all failed requests once the services come back up", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
        } else if (playerName.startsWith("-")) {
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Not a valid command. Here's a list of valid commands:", "red"), false);
            sendMessageToPlayer(Icons.colorText("/tiers -toggle", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -config", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -help | /tiers -debug", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -clear", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -status", Colors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", Colors.WHITE), false);
        } else {
            PlayerProfile playerProfile = addGetPlayer(playerName, true);
            if (playerProfile.isPlayerValid())
                setScreen(new PlayerSearchResultScreen(playerProfile));
        }
    }

    public static void setScreen(Screen screen) {
        MinecraftClient.getInstance().executeAsync(ignored -> MinecraftClient.getInstance().setScreen(screen));
    }

    public static String[] getDebugInfo() {
        String[] debugInfo = new String[4];

        StringBuilder fullInfo = new StringBuilder();
        playerProfiles.forEach(fullInfo::append);

        debugInfo[2] = fullInfo.toString();

        debugInfo[3] = ConfigManager.getCurrentConfig();

        final String[] version = new String[1];
        FabricLoader.getInstance().getModContainer("tiers").ifPresent(tiers -> version[0] = "Tiers version: " + tiers.getMetadata().getVersion().getFriendlyString());
        debugInfo[0] = version[0] + "\n";
        debugInfo[1] = debugInfo[0];
        debugInfo[1] += "Launcher brand: " + MinecraftClient.getLauncherBrand() + "\n";
        debugInfo[1] += "Game version: " + MinecraftClient.getInstance().getGameVersion() + " | " + FabricLoader.getInstance().getRawGameVersion() + "\n";
        debugInfo[1] += "Version type: " + MinecraftClient.getInstance().getVersionType() + "\n";
        debugInfo[1] += "Instance name: " + MinecraftClient.getInstance().getName() + "\n";
        debugInfo[1] += "Game profile name: " + MinecraftClient.getInstance().getGameProfile().name() + "\n";
        debugInfo[1] += "OS info:\n\t" + System.getProperty("os.name") + "\n\t" + System.getProperty("os.version") + "\n\t" + System.getProperty("os.arch") + "\n";
        debugInfo[1] += "CPU info: " + GLX._getCpuInfo() + "\n";
        Runtime runtime = Runtime.getRuntime();
        debugInfo[1] += "RAM info (MB):\n\tMax: " + runtime.maxMemory() / (1024 * 1024) + "\n\tTotal: " + runtime.totalMemory() / (1024 * 1024) + "\n\tFree: " + runtime.freeMemory() / (1024 * 1024) + "\n\tIn use: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "\n";
        GpuDevice gpuDevice = RenderSystem.getDevice();
        debugInfo[1] += "GPU info:\n\t" + gpuDevice.getBackendName() + "\n\t" + gpuDevice.getImplementationInformation() + "\n\t" + gpuDevice.getRenderer() + "\n\t" + gpuDevice.getVersion() + "\n";
        debugInfo[1] += "Java version: " + System.getProperty("java.version") + "\n";
        debugInfo[1] += "Launch args: " + Arrays.toString(FabricLoader.getInstance().getLaunchArguments(false)) + "\n";
        debugInfo[1] += "All Fabric mods: " + FabricLoader.getInstance().getAllMods() + "\n";
        debugInfo[1] += "Resource packs: " + ResourcePackManager.listPacks(MinecraftClient.getInstance().getResourcePackManager().getEnabledProfiles()) + "\n";

        return debugInfo;
    }

    public static void changeIcons(Icons.Type iconType, boolean reload) {
        Icons.identifierMCTiers = Identifier.of("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierPvPTiers = Identifier.of("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierMCTiersTags = Identifier.of("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        Icons.identifierPvPTiersTags = Identifier.of("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        ColorLoader.identifier = Identifier.of("minecraft", "colors/" + iconType.name().toLowerCase(Locale.ROOT) + ".json");

        if (reload)
            MinecraftClient.getInstance().reloadResources();

        activeIcons = iconType;
        ConfigManager.saveConfig();
    }

    public static void showUpdatedPlayerProfile(PlayerProfile playerProfile, boolean removeOld) {
        if (removeOld) {
            playerProfiles.remove(playerProfile);
            PlayerProfileQueue.removeFromQueue(playerProfile);
        }

        tiersCommand(playerProfile.targetName + (removeOld ? "-force" : ""));
    }

    public static void clearCache(boolean start) {
        playerProfiles.clear();
        readyPlayerProfiles.clear();
        PlayerProfileQueue.clearQueue();
        PlayerProfile.failedPlayerProfiles.clear();
        SuperProfile.failedSuperProfiles.clear();
        PlayerProfile.resetRequestCounters();
        SuperProfile.resetRequestCounters();

        CompletableFuture.runAsync(() -> {
            try {
                FileUtils.deleteDirectory(new File(FabricLoader.getInstance().getGameDir() + (start ? "/cache/tiers" : "/cache/tiers/players")));
            } catch (IOException ignored) {
                LOGGER.warn("Error deleting cache folder");
            }
        });

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.execute(() -> {
            if (toggleMod && minecraftClient.world != null)
                minecraftClient.world.getPlayers().forEach(playerEntity -> addGetPlayer(playerEntity.getNameForScoreboard(), false));
        });
    }

    public static void updateCaches() {
        cacheVersion++;

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null)
            return;

        for (Entity entity : minecraftClient.world.getEntities())
            if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay)
                ((DataTrackerAccessor) textDisplay.getDataTracker()).invokeSet(TextDisplayAccessor.getTEXT(), textDisplay.getText(), true);
    }

    public static Text cyclePvPTiersMode() {
        activePvPTiersMode = cycleEnum(activePvPTiersMode, Mode.getPvPTiersValues());
        ConfigManager.saveConfig();
        return activePvPTiersMode.getTextLabel();
    }

    public static void cycleDisplayMode() {
        displayMode = cycleEnum(displayMode, ModesTierDisplay.values());
        ConfigManager.saveConfig();
    }

    private static <T extends Enum<T>> T cycleEnum(T current, T[] values) {
        return values[(current.ordinal() + 1) % values.length];
    }

    public enum ModesTierDisplay {
        HIGHEST,
        SELECTED,
        ADAPTIVE_HIGHEST;

        public String getCurrentMode() {
            if (toString().equalsIgnoreCase("HIGHEST"))
                return "Displayed Tiers: Highest";
            else if (toString().equalsIgnoreCase("SELECTED"))
                return "Displayed Tiers: Selected";
            return "Displayed Tiers: Adaptive Highest";
        }
    }

    public enum DisplayStatus {
        RIGHT,
        LEFT,
        OFF
    }
}