package com.tiers.screens;

import com.tiers.PlayerProfileQueue;
import com.tiers.TiersClient;
import com.tiers.misc.ConfigManager;
import com.tiers.profile.PlayerProfile;
import com.tiers.profile.Status;
import com.tiers.profile.types.PvPTiersProfile;
import com.tiers.textures.ColorControl;
import com.tiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static com.tiers.TiersClient.LOGGER;

public class ConfigScreen extends Screen {
    public static PlayerProfile ownProfile;
    public static PlayerProfile defaultProfile;

    private boolean useOwnProfile;
    private String autoDetectKitBoundKey;
    private String cycleRightBoundKey;
    private String cycleLeftBoundKey;
    private final Identifier playerAvatarTexture = Identifier.of("");
    private boolean imageReady;

    private ButtonWidget toggleMod;
    private ButtonWidget toggleIcons;
    private ButtonWidget toggleTab;
    private ButtonWidget toggleChat;
    private ButtonWidget toggleSeparatorMode;
    private ButtonWidget cycleDisplayMode;
    private ButtonWidget clearPlayerCache;
    private ButtonWidget autoKitDetect;
    private ButtonWidget leftPvPTiers;
//    private ButtonWidget centerPvPTiers;
    private ButtonWidget rightPvPTiers;
    private ButtonWidget activeRightMode;
    private ButtonWidget activeLeftMode;
    private ButtonWidget enableOwnProfile;

    private int centerX;
    private int distance;

    private ConfigScreen() {
        super(Text.literal("Tiers config"));

        autoDetectKitBoundKey = String.valueOf(TiersClient.autoDetectKey.getBoundKeyLocalizedText()).replace("literal{", "\"").replace("}", "\"");
        if (autoDetectKitBoundKey.length() != 3)
            autoDetectKitBoundKey = "the assigned keybind";

        cycleRightBoundKey = String.valueOf(TiersClient.cycleRightKey.getBoundKeyLocalizedText()).replace("literal{", "\"").replace("}", "\"");
        if (cycleRightBoundKey.length() != 3)
            cycleRightBoundKey = "the assigned keybind";

        cycleLeftBoundKey = String.valueOf(TiersClient.cycleLeftKey.getBoundKeyLocalizedText()).replace("literal{", "\"").replace("}", "\"");
        if (cycleLeftBoundKey.length() != 3)
            cycleLeftBoundKey = "the assigned keybind";

        loadPlayerAvatar();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        centerX = width / 2;
        distance = height / 14;

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, Text.of("Tiers config"), centerX, height / 50, Colors.WHITE);

        drawIconShowcase(context);

        if (!useOwnProfile)
            context.drawTexture(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, centerX - height / 10 / 2, height - (int) (height / 4.166) - height / 54, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
        else
            drawPlayerAvatar(context, centerX, height - (int) (height / 4.166) - height / 54);

        context.drawCenteredTextWithShadow(textRenderer, useOwnProfile ? ownProfile.getFullName() : defaultProfile.getFullName(), centerX, height - (int) (height / 4.166) - height / 54 - 12, Colors.WHITE);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, PvPTiersProfile.PVPTIERS_IMAGE, centerX - 12, distance + 110 + 4, 0, 0, 24, 24, 24, 24);

        context.drawTextWithShadow(textRenderer, TiersClient.getRightIcon(), centerX + 90 + 32, distance + 75 + 9, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, TiersClient.getLeftIcon(), centerX - 90 - 32 - 12, distance + 75 + 9, Colors.WHITE);

        checkUpdates();
    }

    private void drawIconShowcase(DrawContext context) {
        for (int i = 0; i < 8; i++) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "gamemodes/classic-medium")))), 34 + 14 * i, 13, Colors.WHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "gamemodes/pvptiers-medium")))), 34 + 14 * i, 38, Colors.WHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "gamemodes/mctiers-medium")))), 34 + 14 * i, 63, Colors.WHITE);
        }
    }

    private void checkUpdates() {
        toggleMod.setPosition(width / 2 - 88 - 2, distance);
        toggleIcons.setPosition(width / 2 + 2, distance);
        toggleTab.setPosition(width / 2 + 2 + 28 + 2, distance);
        toggleChat.setPosition(width / 2 + 2 + 28 + 2 + 28 + 2, distance);
        toggleSeparatorMode.setPosition(width / 2 - 90, distance + 25);
        cycleDisplayMode.setPosition(width / 2 - 90, distance + 50);
        autoKitDetect.setPosition(width / 2 - 90, distance + 75);
        clearPlayerCache.setPosition(width - 88 - 5, height - 20 - 5);
        leftPvPTiers.setPosition(centerX - 10 - 12, distance + 145);
//        centerPvPTiers.setPosition(centerX - 10, distance + 145);
        rightPvPTiers.setPosition(centerX - 10 + 12, distance + 145);
        activeRightMode.setPosition(centerX + 90 + 4, distance + 75);
        activeLeftMode.setPosition(centerX - 90 - 20 - 4, distance + 75);
        enableOwnProfile.setPosition(width - 20 - 5 - 88 - 4, height - 20 - 5);

        updateVisibilities();
    }

    @Override
    protected void init() {
        centerX = width / 2;
        distance = height / 14;

        toggleMod = ButtonWidget.builder(Text.of(TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"), (buttonWidget) -> {
            TiersClient.toggleMod();
            toggleTab.active = TiersClient.toggleMod;
            toggleChat.active = TiersClient.toggleMod;
            buttonWidget.setMessage(Text.of(TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"));
            buttonWidget.setTooltip(Tooltip.of(Text.of((TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"))));
        }).dimensions(width / 2 - 88 - 2, distance, 88, 20).tooltip(Tooltip.of(Text.of((TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers")))).build();

        toggleIcons = ButtonWidget.builder(TiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED, (buttonWidget) -> {
            TiersClient.toggleIcons();
            buttonWidget.setMessage(TiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.of(TiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier")));
        }).dimensions(width / 2 + 2, distance, 28, 20).tooltip(Tooltip.of(Text.of(TiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier"))).build();

        toggleTab = ButtonWidget.builder(TiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED, (buttonWidget) -> {
            TiersClient.toggleTab();
            buttonWidget.setMessage(TiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.of(TiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist")));
        }).dimensions(width / 2 + 2 + 28 + 2, distance, 28, 20).tooltip(Tooltip.of(Text.of(TiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist"))).build();

        toggleChat = ButtonWidget.builder(TiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED, (buttonWidget) -> {
            TiersClient.toggleChat();
            buttonWidget.setMessage(TiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.of(TiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat")));
        }).dimensions(width / 2 + 2 + 28 + 2 + 28 + 2, distance, 28, 20).tooltip(Tooltip.of(Text.of(TiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat"))).build();

        toggleTab.active = TiersClient.toggleMod;
        toggleChat.active = TiersClient.toggleMod;

        toggleSeparatorMode = ButtonWidget.builder(Text.of(TiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"), (buttonWidget) -> {
            TiersClient.toggleAdaptiveSeparator();
            buttonWidget.setMessage(Text.of(TiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"));
            buttonWidget.setTooltip(Tooltip.of(Text.of(TiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier color")));
        }).dimensions(width / 2 - 90, distance + 25, 180, 20).tooltip(Tooltip.of(Text.of(TiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier color"))).build();

        cycleDisplayMode = ButtonWidget.builder(Text.of(TiersClient.displayMode.getCurrentMode()), (buttonWidget) -> {
            TiersClient.cycleDisplayMode();
            buttonWidget.setMessage(Text.of(TiersClient.displayMode.getCurrentMode()));
        }).dimensions(width / 2 - 90, distance + 50, 180, 20).tooltip(Tooltip.of(Text.of(("""
                Selected: only the selected tier will be displayed
                
                Highest: only the highest tier will be displayed
                
                Adaptive Highest: the highest tier will be displayed if selected does not exist""")))).build();

        autoKitDetect = ButtonWidget.builder(Text.of(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"), (buttonWidget) -> {
            TiersClient.toggleAutoKitDetect();
            buttonWidget.setMessage(Text.of(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            buttonWidget.setTooltip(Tooltip.of(Text.of((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(width / 2 - 90, distance + 75, 180, 20).tooltip(Tooltip.of(Text.of((TiersClient.toggleAutoKitDetect ?
                "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")")))).build();

        if (ownProfile.status == Status.READY) {
            enableOwnProfile = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
                useOwnProfile = !useOwnProfile;

                imageReady = false;
                loadPlayerAvatar();

                buttonWidget.setTooltip(Tooltip.of(Text.of(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")")));
            }).dimensions(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.of(Text.of(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")"))).build();
        } else {
            enableOwnProfile = ButtonWidget.builder(Text.of("⚠"), (buttonWidget) -> {
                ownProfile = new PlayerProfile(MinecraftClient.getInstance().getGameProfile().name(), false);
                PlayerProfileQueue.putFirstInQueue(ownProfile);

                close();
            }).dimensions(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.of(Text.of("Can't switch profiles: " + ownProfile.name + " is not found or fetched yet. Click to close screen and retry"))).build();
        }

        clearPlayerCache = ButtonWidget.builder(Text.of("Clear cache"), (buttonWidget) -> TiersClient.clearCache(false)).dimensions(width - 88 - 5, height - 20 - 5, 88, 20).tooltip(Tooltip.of(Text.of("Clear all player cache"))).build();

        leftPvPTiers = ButtonWidget.builder(Text.of("←"), (buttonWidget) -> {
            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.LEFT;
//            updateLeftSwitcher(buttonWidget, centerPvPTiers, rightPvPTiers);
            updateLeftSwitcher(buttonWidget, rightPvPTiers);
        }).dimensions(centerX - 10 - 12, distance + 145, 20, 20).tooltip(Tooltip.of(Text.of("Display PvPTiers on the left"))).build();

//        centerPvPTiers = ButtonWidget.builder(Text.of("●"), (buttonWidget) -> {
//            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
//            leftPvPTiers.active = true;
//            buttonWidget.active = false;
//            rightPvPTiers.active = true;
//            ConfigManager.saveConfig();
//        }).dimensions(centerX - 10, distance + 145, 20, 20).tooltip(Tooltip.of(Text.of("Disable PvPTiers"))).build();

        rightPvPTiers = ButtonWidget.builder(Text.of("→"), (buttonWidget) -> {
            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.RIGHT;
//            updateRightSwitcher(buttonWidget, leftPvPTiers, centerPvPTiers);
            updateRightSwitcher(buttonWidget, leftPvPTiers);
        }).dimensions(centerX - 10 + 12, distance + 145, 20, 20).tooltip(Tooltip.of(Text.of("Display PvPTiers on the right"))).build();

        switch (TiersClient.positionPvPTiers) {
            case RIGHT -> rightPvPTiers.active = false;
//            case OFF -> centerPvPTiers.active = false;
            case LEFT -> leftPvPTiers.active = false;
        }

        activeRightMode = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
            TiersClient.cycleRightMode();
            autoKitDetect.setMessage(Text.of(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.of(Text.of((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(centerX + 90 + 4, distance + 75, 20, 20).tooltip(Tooltip.of(Text.of("Cycle active right gamemode (press " + cycleRightBoundKey + " in game)"))).build();

        activeLeftMode = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
            TiersClient.cycleLeftMode();
            autoKitDetect.setMessage(Text.of(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.of(Text.of((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(centerX - 90 - 20 - 4, distance + 75, 20, 20).tooltip(Tooltip.of(Text.of("Cycle active left gamemode (press " + cycleLeftBoundKey + " in game)"))).build();

        ButtonWidget useClassicIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.CLASSIC ? Text.of("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.CLASSIC ? Text.of("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.CLASSIC, true);
        }).dimensions(5, 5, 20, 20).tooltip(Tooltip.of(Text.of("Use classic styled icons and colors"))).build();

        ButtonWidget usePvPTiersIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.PVPTIERS ? Text.of("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.PVPTIERS ? Text.of("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.PVPTIERS, true);
        }).dimensions(5, 30, 20, 20).tooltip(Tooltip.of(Text.of("Use PvPTiers styled icons and colors"))).build();

        ButtonWidget useMCTiersIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.MCTIERS ? Text.of("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.MCTIERS ? Text.of("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.MCTIERS, true);
        }).dimensions(5, 55, 20, 20).tooltip(Tooltip.of(Text.of("Use MCTiers styled icons and colors"))).build();

        switch (TiersClient.activeIcons) {
            case CLASSIC -> useClassicIcons.active = false;
            case PVPTIERS -> usePvPTiersIcons.active = false;
            case MCTIERS -> useMCTiersIcons.active = false;
        }

        updateVisibilities();

//        Stream.of(toggleMod, toggleIcons, toggleTab, toggleChat, toggleSeparatorMode, cycleDisplayMode, autoKitDetect, clearPlayerCache, leftPvPTiers, centerPvPTiers, rightPvPTiers, activeRightMode, activeLeftMode, enableOwnProfile, useClassicIcons, usePvPTiersIcons, useMCTiersIcons)
//                .forEach(this::addDrawableChild);
        Stream.of(toggleMod, toggleIcons, toggleTab, toggleChat, toggleSeparatorMode, cycleDisplayMode, autoKitDetect, clearPlayerCache, leftPvPTiers, rightPvPTiers, activeRightMode, activeLeftMode, enableOwnProfile, useClassicIcons, usePvPTiersIcons, useMCTiersIcons)
                .forEach(this::addDrawableChild);
    }

//    private void updateRightSwitcher(ButtonWidget buttonWidget, ButtonWidget leftMCTiers, ButtonWidget centerMCTiers) {
//        leftMCTiers.active = true;
//        centerMCTiers.active = true;
//        buttonWidget.active = false;
//        ConfigManager.saveConfig();
//    }
//
//    private void updateLeftSwitcher(ButtonWidget buttonWidget, ButtonWidget centerMCTiers, ButtonWidget rightMCTiers) {
//        buttonWidget.active = false;
//        centerMCTiers.active = true;
//        rightMCTiers.active = true;
//        ConfigManager.saveConfig();
//    }

    private void updateRightSwitcher(ButtonWidget buttonWidget, ButtonWidget leftMCTiers) {
        leftMCTiers.active = true;
        buttonWidget.active = false;
        ConfigManager.saveConfig();
    }

    private void updateLeftSwitcher(ButtonWidget buttonWidget, ButtonWidget rightMCTiers) {
        buttonWidget.active = false;
        rightMCTiers.active = true;
        ConfigManager.saveConfig();
    }

    private void updateVisibilities() {
        activeRightMode.visible = TiersClient.positionPvPTiers == TiersClient.DisplayStatus.RIGHT;
        activeLeftMode.visible = TiersClient.positionPvPTiers == TiersClient.DisplayStatus.LEFT;
    }

    private void drawPlayerAvatar(DrawContext context, int x, int y) {
        if (imageReady) {
            if (ownProfile.imageSaved == 1 || ownProfile.imageSaved == 2)
                context.drawTexture(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - height / 10 / 2, y, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
            else if (ownProfile.imageSaved < 6 && ownProfile.imageSaved > 2)
                context.drawTexture(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - height / 7 / 2, y, 0, 0, height / 7, (int) (height / 4.145), height / 7, (int) (height / 4.145));
        } else if (ownProfile.imageSaved != 0) {
            loadPlayerAvatar();
        } else if (ownProfile.numberOfImageRequests >= 6)
            context.drawCenteredTextWithShadow(textRenderer, Text.of(ownProfile.name + "'s skin failed to load. Restart game to retry"), x, y + 50, ColorControl.getColorMinecraftStandard("red"));
    }

    private void loadPlayerAvatar() {
        if (imageReady)
            return;

        try (FileInputStream fileInputStream = new FileInputStream(FabricLoader.getInstance().getGameDir().resolve("cache/tiers/" + (useOwnProfile ? ownProfile.uuid : defaultProfile.uuid) + ".png").toFile())) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(playerAvatarTexture, new NativeImageBackedTexture(null, NativeImage.read(fileInputStream)));
            imageReady = true;
        } catch (IOException ignored) {
            LOGGER.warn("Error loading player skin");
        }
    }

    public static Screen getConfigScreen(Screen ignoredScreen) {
        return new ConfigScreen();
    }
}