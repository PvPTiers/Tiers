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
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import java.util.stream.Collectors;

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

//    public static DisplayStatus positionMCTiers = DisplayStatus.OFF;
//    public static Mode activeMCTiersMode = Mode.MCTIERS_VANILLA;

    public static DisplayStatus positionPvPTiers = DisplayStatus.LEFT;
    public static Mode activePvPTiersMode = Mode.PVPTIERS_CRYSTAL;

//    public static DisplayStatus positionSubtiers = DisplayStatus.RIGHT;
//    public static Mode activeSubtiersMode = Mode.SUBTIERS_MINECART;

    public static KeyMapping autoDetectKey;
    public static KeyMapping openClosestPlayerProfile;
    public static KeyMapping cycleRightKey;
    public static KeyMapping cycleLeftKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        changeIcons(activeIcons, false);
        clearCache(true);
        CommandRegister.registerCommands();

        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("tiers");

        modContainer.ifPresent(tiers -> {
            ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("resourcepacks", "tiers-resources"), tiers, Component.literal("Resources for Tiers"), PackActivationType.ALWAYS_ENABLED);
            userAgent += " v" + tiers.getMetadata().getVersion().getFriendlyString();
        });

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("tiers"));
        autoDetectKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("Auto Detect Kit", GLFW.GLFW_KEY_Y, category));
        openClosestPlayerProfile = KeyMappingHelper.registerKeyMapping(new KeyMapping("Open Closest Player Profile", GLFW.GLFW_KEY_H, category));
        cycleRightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("Cycle Right Gamemodes", GLFW.GLFW_KEY_I, category));
        cycleLeftKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("Cycle Left Gamemodes", GLFW.GLFW_KEY_U, category));

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.parse("tiers"), new ColorLoader());
        ClientTickEvents.END_CLIENT_TICK.register(TiersClient::checkKeys);
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (toggleAutoKitDetect)
                InventoryChecker.checkInventory(minecraft, false);
            if (cachesDirty) {
                cachesDirty = false;
                updateCaches();
            }
        });

        LOGGER.info("Tiers initialized | User agent: {}", userAgent);
    }

    public static PlayerProfile addGetPlayer(String playerName, boolean priority) {
        return addGetPlayer(playerName, priority, null);
    }

    public static PlayerProfile addGetPlayer(String playerName, boolean priority, UUID knownUuid) {
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

        // On online-mode servers the GameProfile already carries the real Mojang UUID, so the username->UUID API round-trip is unnecessary.
        if (knownUuid != null && PlayerProfile.isRealUuid(knownUuid) && newProfile.status == Status.SEARCHING)
            newProfile.completeWithKnownUuid(knownUuid);
        else if (priority)
            PlayerProfileQueue.putFirstInQueue(newProfile);
        else
            PlayerProfileQueue.enqueue(newProfile);

        playerProfiles.add(newProfile);
        return newProfile;
    }

    public static void updateAllTags() {
        Minecraft.getInstance().execute(() -> {
            readyPlayerProfiles.values().forEach(PlayerProfile::updateAppendingText);

            if (ConfigScreen.ownProfile != null && ConfigScreen.defaultProfile != null) {
                ConfigScreen.ownProfile.updateAppendingText();
                ConfigScreen.defaultProfile.updateAppendingText();
            }
            updateCaches();
        });
    }

    public static void restyleAllTexts(List<PlayerProfile> playerProfiles) {
        Minecraft.getInstance().execute(() -> {
            playerProfiles.forEach(playerProfile -> {
                if (playerProfile.status == Status.READY) {
//                    if (playerProfile.profileMCTiers.status == Status.READY)
//                        playerProfile.profileMCTiers.parseJson(playerProfile.profileMCTiers.originalJson);
                    if (playerProfile.profilePvPTiers.status == Status.READY)
                        playerProfile.profilePvPTiers.parseJson(playerProfile.profilePvPTiers.originalJson);
//                    if (playerProfile.profileSubtiers.status == Status.READY)
//                        playerProfile.profileSubtiers.parseJson(playerProfile.profileSubtiers.originalJson);
                }
                playerProfile.updateAppendingText();
            });
            updateCaches();
        });
    }

    public static String getNearestPlayerName() {
        Minecraft minecraft = Minecraft.getInstance();
        Player self = minecraft.player;
        if (self == null)
            return null;

        try (Level level = self.level()) {
            Player playerEntity = level.players().stream()
                    .filter(player -> player != self)
                    .filter(player -> self.distanceTo(player) < Minecraft.getInstance().options.renderDistance().get() * 16)
                    .min(Comparator.comparingDouble(self::distanceTo))
                    .orElse(null);

            if (playerEntity != null)
                return playerEntity.getScoreboardName();
        } catch (IOException ignored) {

        }

        return null;
    }

    private static void checkKeys(Minecraft minecraft) {
        if (autoDetectKey.consumeClick())
            InventoryChecker.checkInventory(minecraft, true);

        if (openClosestPlayerProfile.consumeClick()) {
            String nearestPlayerName = getNearestPlayerName();
            if (nearestPlayerName != null)
                tiersCommand(nearestPlayerName);
            else
                sendMessageToPlayer(Icons.colorText("No players in render distance", "red"), true);
        }

        if (cycleRightKey.consumeClick()) {
            Component message = cycleRightMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the right display", "red"), true);
        }

        if (cycleLeftKey.consumeClick()) {
            Component message = cycleLeftMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the left display", "red"), true);
        }
    }

    public static Component cycleRightMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

//        if (positionMCTiers.toString().equalsIgnoreCase("RIGHT"))
//            return Component.literal("Right (MCTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cycleMCTiersMode());

        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return Component.literal("Right (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cyclePvPTiersMode());

//        if (positionSubtiers.toString().equalsIgnoreCase("RIGHT"))
//            return Component.literal("Right (Subtiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cycleSubtiersMode());

        return null;
    }

    public static Component cycleLeftMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

//        if (positionMCTiers.toString().equalsIgnoreCase("LEFT"))
//            return Component.literal("Left (MCTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cycleMCTiersMode());

        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return Component.literal("Left (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cyclePvPTiersMode());

//        if (positionSubtiers.toString().equalsIgnoreCase("LEFT"))
//            return Component.literal("Left (Subtiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cycleSubtiersMode());

        return null;
    }

    public static Component getRightIcon() {
//        if (positionMCTiers.toString().equalsIgnoreCase("RIGHT"))
//            return activeMCTiersMode.getIcon();

        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return activePvPTiersMode.getIcon();

//        if (positionSubtiers.toString().equalsIgnoreCase("RIGHT"))
//            return activeSubtiersMode.getIcon();

        return Component.empty();
    }

    public static Component getLeftIcon() {
//        if (positionMCTiers.toString().equalsIgnoreCase("LEFT"))
//            return activeMCTiersMode.getIcon();

        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return activePvPTiersMode.getIcon();

//        if (positionSubtiers.toString().equalsIgnoreCase("LEFT"))
//            return activeSubtiersMode.getIcon();

        return Component.empty();
    }

    public static void sendMessageToPlayer(Component message, boolean overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            if (overlay)
                minecraft.player.sendOverlayMessage(message);
            else
                minecraft.player.sendSystemMessage(message);
        }
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
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("--- Tiers help ---", CommonColors.YELLOW), false);
            sendMessageToPlayer(Component.literal("- General contact: ").append(Component.literal("flavio6561 on Discord").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://discordapp.com/users/715189608085716992"))))), false);
            sendMessageToPlayer(Component.literal("- Report a bug: ").append(Component.literal("Tiers GitHub issues").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/PvPTiers/Tiers/issues"))))), false);
            sendMessageToPlayer(Component.literal("- It's not advisable to create tickets in PvPTiers support"), false);
            sendMessageToPlayer(Component.literal("- ").append(Component.literal("Changelogs").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/PvPTiers/Tiers/wiki/Version-changelogs"))))), false);
            sendMessageToPlayer(Component.literal("- ").append(Component.literal("Modrinth page").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/tiers"))))), false);

            String[] debugInfo = getDebugInfo();
            sendMessageToPlayer(Icons.colorText("\n" + debugInfo[0], CommonColors.SOFT_YELLOW), false);
            Minecraft.getInstance().keyboardHandler.setClipboard(debugInfo[1]);

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
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else if (playerName.equalsIgnoreCase("-clear")) {
            clearCache(false);
            sendMessageToPlayer(Icons.colorText("Cleared player cache", "green"), true);
        } else if (playerName.equalsIgnoreCase("-status")) {
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Player profiles status:", "green"), false);
            sendMessageToPlayer(Icons.colorText("Cached players: " + PlayerProfile.playerProfilesRequests.get() + " (" + PlayerProfile.failedPlayerProfilesRequests.get() + " failed)", CommonColors.WHITE), false);
//            sendMessageToPlayer(Icons.colorText("MCTiers requests failed: " + SuperProfile.failedMCTiersRequests + "/" + SuperProfile.MCTiersRequests + " (failed / requested)", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("PvPTiers requests failed: " + SuperProfile.failedPvPTiersRequests + "/" + SuperProfile.PvPTiersRequests + " (failed / requested)", CommonColors.YELLOW), false);
//            sendMessageToPlayer(Icons.colorText("Subtiers requests failed: " + SuperProfile.failedSubtiersRequests + "/" + SuperProfile.SubtiersRequests + " (failed / requested)", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
//            sendMessageToPlayer(Icons.colorText("MCTiers status | is down? " + SuperProfile.isMCTiersDown + " | Failed request in last minute: " + SuperProfile.failedMCTiersRequestsLastMinute, CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("PvPTiers status | is down? " + SuperProfile.isPvPTiersDown + " | Failed request in last minute: " + SuperProfile.failedPvPTiersRequestsLastMinute, CommonColors.YELLOW), false);
//            sendMessageToPlayer(Icons.colorText("Subtiers status | is down? " + SuperProfile.isSubtiersDown + " | Failed request in last minute: " + SuperProfile.failedSubtiersRequestsLastMinute, CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("Tiers will try to recover all failed requests once the services come back up", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else if (playerName.startsWith("-")) {
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Not a valid command. Here's a list of valid commands:", "red"), false);
            sendMessageToPlayer(Icons.colorText("/tiers -toggle", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -config", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -help | /tiers -debug", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -clear", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/tiers -status", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else {
            PlayerProfile playerProfile = addGetPlayer(playerName, true);
            if (playerProfile.isPlayerValid())
                setScreen(new PlayerSearchResultScreen(playerProfile));
        }
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().executeIfPossible(() -> Minecraft.getInstance().setScreenAndShow(screen));
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
        debugInfo[1] += "Launcher brand: " + Minecraft.getLauncherBrand() + "\n";
        debugInfo[1] += "Game version: " + Minecraft.getInstance().getLaunchedVersion() + " | " + FabricLoader.getInstance().getRawGameVersion() + "\n";
        debugInfo[1] += "Version type: " + SharedConstants.getCurrentVersion().name() + "\n";
        debugInfo[1] += "Instance name: " + Minecraft.getInstance().name() + "\n";
        debugInfo[1] += "Game profile name: " + Minecraft.getInstance().getGameProfile().name() + "\n";
        debugInfo[1] += "OS info:\n\t" + System.getProperty("os.name") + "\n\t" + System.getProperty("os.version") + "\n\t" + System.getProperty("os.arch") + "\n";
        debugInfo[1] += "CPU info: " + GLX._getCpuInfo() + "\n";
        Runtime runtime = Runtime.getRuntime();
        debugInfo[1] += "RAM info (MB):\n\tMax: " + runtime.maxMemory() / (1024 * 1024) + "\n\tTotal: " + runtime.totalMemory() / (1024 * 1024) + "\n\tFree: " + runtime.freeMemory() / (1024 * 1024) + "\n\tIn use: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "\n";
        GpuDevice gpuDevice = RenderSystem.getDevice();
        debugInfo[1] += "GPU info:\n\t" + gpuDevice.getDeviceInfo().backendName() + "\n\t" + gpuDevice.getDeviceInfo().driverInfo() + "\n\t" + gpuDevice.getDeviceInfo().name() + "\n\t" + gpuDevice.getDeviceInfo().vendorName() + "\n";
        debugInfo[1] += "Java version: " + System.getProperty("java.version") + "\n";
        debugInfo[1] += "Launch args: " + Arrays.toString(FabricLoader.getInstance().getLaunchArguments(false)) + "\n";
        debugInfo[1] += "All Fabric mods: " + FabricLoader.getInstance().getAllMods() + "\n";
        debugInfo[1] += "Resource packs: " + Minecraft.getInstance().getResourceManager().listPacks().map(PackResources::packId).collect(Collectors.joining(", ")) + "\n";

        return debugInfo;
    }

    public static void changeIcons(Icons.Type iconType, boolean reload) {
        Icons.identifierMCTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierPvPTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierMCTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        Icons.identifierPvPTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        ColorLoader.identifier = Identifier.fromNamespaceAndPath("minecraft", "colors/" + iconType.name().toLowerCase(Locale.ROOT) + ".json");

        if (reload)
            Minecraft.getInstance().reloadResourcePacks();

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

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (toggleMod && minecraft.level != null)
                minecraft.level.players().forEach(playerEntity -> addGetPlayer(playerEntity.getScoreboardName(), false));
        });
    }

    public static void updateCaches() {
        cacheVersion++;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        for (Entity entity : minecraft.level.entitiesForRendering())
            if (entity instanceof Display.TextDisplay textDisplay)
                ((DataTrackerAccessor) textDisplay.getEntityData()).invokeSet(TextDisplayAccessor.getTEXT(), textDisplay.getText(), true);
    }

//    public static Component cycleMCTiersMode() {
//        activeMCTiersMode = cycleEnum(activeMCTiersMode, Mode.getMCTiersValues());
//        ConfigManager.saveConfig();
//        return activeMCTiersMode.getTextLabel();
//    }

    public static Component cyclePvPTiersMode() {
        activePvPTiersMode = cycleEnum(activePvPTiersMode, Mode.getPvPTiersValues());
        ConfigManager.saveConfig();
        return activePvPTiersMode.getTextLabel();
    }

//    public static Component cycleSubtiersMode() {
//        activeSubtiersMode = cycleEnum(activeSubtiersMode, Mode.getSubtiersValues());
//        ConfigManager.saveConfig();
//        return activeSubtiersMode.getTextLabel();
//    }

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