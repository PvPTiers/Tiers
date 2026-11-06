package com.tiers.screens;

import com.tiers.PlayerProfileQueue;
import com.tiers.TiersClient;
import com.tiers.misc.ConfigManager;
import com.tiers.profile.PlayerProfile;
import com.tiers.profile.Status;
import com.tiers.profile.types.MCTiersProfile;
import com.tiers.profile.types.PvPTiersProfile;
import com.tiers.profile.types.SubtiersProfile;
import com.tiers.textures.ColorControl;
import com.tiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Style;
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
    private boolean imageError;

    private ButtonWidget toggleMod;
    private ButtonWidget toggleRegion;
    private ButtonWidget toggleIcons;
    private ButtonWidget togglePeak;
    private ButtonWidget toggleTab;
    private ButtonWidget toggleChat;
    private ButtonWidget toggleSeparatorMode;
    private ButtonWidget cycleDisplayMode;
    private ButtonWidget clearPlayerCache;
    private ButtonWidget resetSettings;
    private ButtonWidget autoKitDetect;
    private ButtonWidget leftMCTiers;
    private ButtonWidget centerMCTiers;
    private ButtonWidget rightMCTiers;
    private ButtonWidget leftPvPTiers;
    private ButtonWidget centerPvPTiers;
    private ButtonWidget rightPvPTiers;
    private ButtonWidget leftSubtiers;
    private ButtonWidget centerSubtiers;
    private ButtonWidget rightSubtiers;
    private ButtonWidget activeRightMode;
    private ButtonWidget activeLeftMode;
    private ButtonWidget enableOwnProfile;
    private boolean buttonsReady;

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
            context.drawTexture(RenderLayer::getGuiTextured, playerAvatarTexture, centerX - height / 10 / 2, height - (int) (height / 4.166) - height / 54, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
        else
            drawPlayerAvatar(context, centerX, height - (int) (height / 4.166) - height / 54);

        context.drawCenteredTextWithShadow(textRenderer, useOwnProfile ? ownProfile.getFullName() : defaultProfile.getFullName(), centerX, height - (int) (height / 4.166) - height / 54 - 12, Colors.WHITE);

        context.drawTexture(RenderLayer::getGuiTextured, MCTiersProfile.MCTIERS_IMAGE, centerX - 120 - 64, distance + 110 + 4, 0, 0, 128, 24, 128, 24);
        context.drawTexture(RenderLayer::getGuiTextured, PvPTiersProfile.PVPTIERS_IMAGE, centerX - 12, distance + 110 + 4, 0, 0, 24, 24, 24, 24);
        context.drawTexture(RenderLayer::getGuiTextured, SubtiersProfile.SUBTIERS_IMAGE, centerX + 120 - 15, distance + 110, 0, 0, 30, 30, 30, 30);

        context.drawTextWithShadow(textRenderer, TiersClient.getRightIcon(), centerX + 90 + 32, distance + 75 + 9, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, TiersClient.getLeftIcon(), centerX - 90 - 32 - 12, distance + 75 + 9, Colors.WHITE);

        checkUpdates();
    }

    private void drawIconShowcase(DrawContext context) {
        for (int i = 0; i < 8; i++) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "gamemodes/classic-medium"))), 15, 32 + 14 * i, Colors.WHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "gamemodes/pvptiers-medium"))), 40, 32 + 14 * i, Colors.WHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "gamemodes/mctiers-medium"))), 65, 32 + 14 * i, Colors.WHITE);
        }
    }

    private void checkUpdates() {
        if (!buttonsReady)
            return;

        toggleMod.setPosition(width / 2 - 88 - 2, distance);
        toggleRegion.setPosition(width / 2 - 88 - 2 + 28 + 2 + 1, distance);
        toggleIcons.setPosition(width / 2 - 88 - 2 + 28 + 2 + 28 + 2 + 1, distance);
        togglePeak.setPosition(width / 2 + 2 - 1, distance);
        toggleTab.setPosition(width / 2 + 2 + 28 + 2 - 1, distance);
        toggleChat.setPosition(width / 2 + 2 + 28 + 2 + 28 + 2 - 1, distance);
        toggleSeparatorMode.setPosition(width / 2 - 90, distance + 25);
        cycleDisplayMode.setPosition(width / 2 - 90, distance + 50);
        autoKitDetect.setPosition(width / 2 - 90, distance + 75);
        clearPlayerCache.setPosition(width - 88 - 5, height - 20 - 5);
        resetSettings.setPosition(5, height - 20 - 5);
        leftMCTiers.setPosition(centerX - 120 - 10 - 24, distance + 145);
        centerMCTiers.setPosition(centerX - 120 - 10, distance + 145);
        rightMCTiers.setPosition(centerX - 120 - 10 + 24, distance + 145);
        leftPvPTiers.setPosition(centerX - 10 - 24, distance + 145);
        centerPvPTiers.setPosition(centerX - 10, distance + 145);
        rightPvPTiers.setPosition(centerX - 10 + 24, distance + 145);
        leftSubtiers.setPosition(centerX + 120 - 10 - 24, distance + 145);
        centerSubtiers.setPosition(centerX + 120 - 10, distance + 145);
        rightSubtiers.setPosition(centerX + 120 - 10 + 24, distance + 145);
        activeRightMode.setPosition(centerX + 90 + 4, distance + 75);
        activeLeftMode.setPosition(centerX - 90 - 20 - 4, distance + 75);
        enableOwnProfile.setPosition(width - 20 - 5 - 88 - 4, height - 20 - 5);

        updateVisibilities();
    }

    @Override
    protected void init() {
        centerX = width / 2;
        distance = height / 14;

        toggleMod = ButtonWidget.builder(TiersClient.toggleMod ? Icons.LEVER_ON : Icons.LEVER_OFF, (buttonWidget) -> {
            TiersClient.toggleMod();
            updateActiveButtonsState();
            buttonWidget.setMessage(TiersClient.toggleMod ? Icons.LEVER_ON : Icons.LEVER_OFF);
            buttonWidget.setTooltip(Tooltip.of(Text.literal((TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"))));
        }).dimensions(width / 2 - 88 - 2, distance, 29, 20).tooltip(Tooltip.of(Text.literal((TiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers")))).build();

        toggleRegion = ButtonWidget.builder(TiersClient.toggleRegion ? Icons.REGION : Icons.REGION_DISABLED, (buttonWidget) -> {
            TiersClient.toggleRegion();
            buttonWidget.setMessage(TiersClient.toggleRegion ? Icons.REGION : Icons.REGION_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.toggleRegion ? "Disable regions from being displayed next to the tier" : "Enable region display next to the tier")));
        }).dimensions(width / 2 - 88 - 2 + 28 + 2 + 1, distance, 28, 20).tooltip(Tooltip.of(Text.literal(TiersClient.toggleRegion ? "Disable regions from being displayed next to the tier" : "Enable region display next to the tier"))).build();

        toggleIcons = ButtonWidget.builder(TiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED, (buttonWidget) -> {
            TiersClient.toggleIcons();
            buttonWidget.setMessage(TiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier")));
        }).dimensions(width / 2 - 88 - 2 + 28 + 2 + 28 + 2 + 1, distance, 28, 20).tooltip(Tooltip.of(Text.literal(TiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier"))).build();

        togglePeak = ButtonWidget.builder(TiersClient.togglePeak ? Icons.PEAK : Icons.PEAK_DISABLED, (buttonWidget) -> {
            TiersClient.togglePeak();
            buttonWidget.setMessage(TiersClient.togglePeak ? Icons.PEAK : Icons.PEAK_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.togglePeak ? """
                    Disable peak tiers
                    
                    Peak tiers: If you had HT3 and got demoted to LT3, you'll see ^HT3""" : """
                    Enable peak tiers
                    
                    Peak tiers: If you had HT3 and got demoted to LT3, you'll see ^HT3""")));
        }).dimensions(width / 2 + 2 - 1, distance, 28, 20).tooltip(Tooltip.of(Text.literal(TiersClient.togglePeak ? """
                Disable peak tiers
                
                Peak tiers: If you had HT3 and got demoted to LT3, you'll see ^HT3""" : """
                Enable peak tiers
                
                Peak tiers: If you had HT3 and got demoted to LT3, you'll see ^HT3"""))).build();

        toggleTab = ButtonWidget.builder(TiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED, (buttonWidget) -> {
            TiersClient.toggleTab();
            buttonWidget.setMessage(TiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist")));
        }).dimensions(width / 2 + 2 + 28 + 2 - 1, distance, 28, 20).tooltip(Tooltip.of(Text.literal(TiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist"))).build();

        toggleChat = ButtonWidget.builder(TiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED, (buttonWidget) -> {
            TiersClient.toggleChat();
            buttonWidget.setMessage(TiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED);
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat")));
        }).dimensions(width / 2 + 2 + 28 + 2 + 28 + 2 - 1, distance, 29, 20).tooltip(Tooltip.of(Text.literal(TiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat"))).build();

        toggleSeparatorMode = ButtonWidget.builder(Text.literal(TiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"), (buttonWidget) -> {
            TiersClient.toggleAdaptiveSeparator();
            buttonWidget.setMessage(Text.literal(TiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"));
            buttonWidget.setTooltip(Tooltip.of(Text.literal(TiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier / region color")));
        }).dimensions(width / 2 - 90, distance + 25, 180, 20).tooltip(Tooltip.of(Text.literal(TiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier / region color"))).build();

        cycleDisplayMode = ButtonWidget.builder(Text.literal(TiersClient.displayMode.getCurrentMode()), (buttonWidget) -> {
            TiersClient.cycleDisplayMode();
            buttonWidget.setMessage(Text.literal(TiersClient.displayMode.getCurrentMode()));
        }).dimensions(width / 2 - 90, distance + 50, 180, 20).tooltip(Tooltip.of(Text.literal(("""
                Selected: only the selected tier will be displayed
                
                Highest: only the highest tier will be displayed
                
                Adaptive Highest: the highest tier will be displayed if selected does not exist""")))).build();

        autoKitDetect = ButtonWidget.builder(Text.literal(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"), (buttonWidget) -> {
            TiersClient.toggleAutoKitDetect();
            buttonWidget.setMessage(Text.literal(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            buttonWidget.setTooltip(Tooltip.of(Text.literal((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the current gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(width / 2 - 90, distance + 75, 180, 20).tooltip(Tooltip.of(Text.literal((TiersClient.toggleAutoKitDetect ?
                "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                "Enable auto kit detect: Tiers will always scan your inventory to display the current gamemode (instead of pressing " + autoDetectKitBoundKey + ")")))).build();

        if (ownProfile.status == Status.READY) {
            enableOwnProfile = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
                useOwnProfile = !useOwnProfile;

                imageReady = false;
                loadPlayerAvatar();

                buttonWidget.setTooltip(Tooltip.of(Text.literal(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")")));
            }).dimensions(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.of(Text.literal(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")"))).build();
        } else {
            enableOwnProfile = ButtonWidget.builder(Text.literal("⚠"), (buttonWidget) -> {
                ownProfile = new PlayerProfile(MinecraftClient.getInstance().getGameProfile().getName(), false);
                PlayerProfileQueue.putFirstInQueue(ownProfile);

                close();
            }).dimensions(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.of(Text.literal("Can't switch profiles: " + ownProfile.name + " is not found or fetched yet. Click to close screen and retry"))).build();
        }

        clearPlayerCache = ButtonWidget.builder(Text.literal("Clear cache"), (buttonWidget) -> TiersClient.clearCache(false)).dimensions(width - 88 - 5, height - 20 - 5, 88, 20).tooltip(Tooltip.of(Text.literal("Clear all player cache"))).build();

        resetSettings = ButtonWidget.builder(Text.literal("Reset settings"), (buttonWidget) -> {
            close();
            TiersClient.resetSettings();
        }).dimensions(5, height - 20 - 5, 88, 20).tooltip(Tooltip.of(Text.literal("Reset Tiers settings to default"))).build();

        leftMCTiers = ButtonWidget.builder(Text.literal("←"), (buttonWidget) -> {
            TiersClient.positionMCTiers = TiersClient.DisplayStatus.LEFT;
            if (TiersClient.positionPvPTiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
                leftPvPTiers.active = true;
                centerPvPTiers.active = false;
            }
            if (TiersClient.positionSubtiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionSubtiers = TiersClient.DisplayStatus.OFF;
                leftSubtiers.active = true;
                centerSubtiers.active = false;
            }
            rightMCTiers.active = true;
            centerMCTiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 120 - 10 - 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display MCTiers on the left"))).build();

        centerMCTiers = ButtonWidget.builder(Text.literal("●"), (buttonWidget) -> {
            TiersClient.positionMCTiers = TiersClient.DisplayStatus.OFF;
            leftMCTiers.active = true;
            buttonWidget.active = false;
            rightMCTiers.active = true;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 120 - 10, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Disable MCTiers"))).build();

        rightMCTiers = ButtonWidget.builder(Text.literal("→"), (buttonWidget) -> {
            TiersClient.positionMCTiers = TiersClient.DisplayStatus.RIGHT;
            if (TiersClient.positionPvPTiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
                centerPvPTiers.active = false;
                rightPvPTiers.active = true;
            }
            if (TiersClient.positionSubtiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionSubtiers = TiersClient.DisplayStatus.OFF;
                centerSubtiers.active = false;
                rightSubtiers.active = true;
            }
            leftMCTiers.active = true;
            centerMCTiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 120 - 10 + 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display MCTiers on the right"))).build();

        leftPvPTiers = ButtonWidget.builder(Text.literal("←"), (buttonWidget) -> {
            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.LEFT;
            if (TiersClient.positionMCTiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionMCTiers = TiersClient.DisplayStatus.OFF;
                leftMCTiers.active = true;
                centerMCTiers.active = false;
            }
            if (TiersClient.positionSubtiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionSubtiers = TiersClient.DisplayStatus.OFF;
                leftSubtiers.active = true;
                centerSubtiers.active = false;
            }
            rightPvPTiers.active = true;
            centerPvPTiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 10 - 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display PvPTiers on the left"))).build();

        centerPvPTiers = ButtonWidget.builder(Text.literal("●"), (buttonWidget) -> {
            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
            leftPvPTiers.active = true;
            buttonWidget.active = false;
            rightPvPTiers.active = true;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 10, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Disable PvPTiers"))).build();

        rightPvPTiers = ButtonWidget.builder(Text.literal("→"), (buttonWidget) -> {
            TiersClient.positionPvPTiers = TiersClient.DisplayStatus.RIGHT;
            if (TiersClient.positionMCTiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionMCTiers = TiersClient.DisplayStatus.OFF;
                centerMCTiers.active = false;
                rightMCTiers.active = true;
            }
            if (TiersClient.positionSubtiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionSubtiers = TiersClient.DisplayStatus.OFF;
                centerSubtiers.active = false;
                rightSubtiers.active = true;
            }
            leftPvPTiers.active = true;
            centerPvPTiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX - 10 + 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display PvPTiers on the right"))).build();

        leftSubtiers = ButtonWidget.builder(Text.literal("←"), (buttonWidget) -> {
            TiersClient.positionSubtiers = TiersClient.DisplayStatus.LEFT;
            if (TiersClient.positionMCTiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionMCTiers = TiersClient.DisplayStatus.OFF;
                leftMCTiers.active = true;
                centerMCTiers.active = false;
            }
            if (TiersClient.positionPvPTiers == TiersClient.DisplayStatus.LEFT) {
                TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
                leftPvPTiers.active = true;
                centerPvPTiers.active = false;
            }
            rightSubtiers.active = true;
            centerSubtiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX + 120 - 10 - 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display Subtiers on the left"))).build();

        centerSubtiers = ButtonWidget.builder(Text.literal("●"), (buttonWidget) -> {
            TiersClient.positionSubtiers = TiersClient.DisplayStatus.OFF;
            leftSubtiers.active = true;
            buttonWidget.active = false;
            rightSubtiers.active = true;
            ConfigManager.saveConfig();
        }).dimensions(centerX + 120 - 10, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Disable Subtiers"))).build();

        rightSubtiers = ButtonWidget.builder(Text.literal("→"), (buttonWidget) -> {
            TiersClient.positionSubtiers = TiersClient.DisplayStatus.RIGHT;
            if (TiersClient.positionMCTiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionMCTiers = TiersClient.DisplayStatus.OFF;
                centerMCTiers.active = false;
                rightMCTiers.active = true;
            }
            if (TiersClient.positionPvPTiers == TiersClient.DisplayStatus.RIGHT) {
                TiersClient.positionPvPTiers = TiersClient.DisplayStatus.OFF;
                centerPvPTiers.active = false;
                rightPvPTiers.active = true;
            }
            leftSubtiers.active = true;
            centerSubtiers.active = true;
            buttonWidget.active = false;
            ConfigManager.saveConfig();
        }).dimensions(centerX + 120 - 10 + 24, distance + 145, 20, 20).tooltip(Tooltip.of(Text.literal("Display Subtiers on the right"))).build();

        activeRightMode = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
            TiersClient.cycleRightMode();
            autoKitDetect.setMessage(Text.literal(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.of(Text.literal((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(centerX + 90 + 4, distance + 75, 20, 20).tooltip(Tooltip.of(Text.literal("Cycle active right gamemode (press " + cycleRightBoundKey + " in game)"))).build();

        activeLeftMode = ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> {
            TiersClient.cycleLeftMode();
            autoKitDetect.setMessage(Text.literal(TiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.of(Text.literal((TiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).dimensions(centerX - 90 - 20 - 4, distance + 75, 20, 20).tooltip(Tooltip.of(Text.literal("Cycle active left gamemode (press " + cycleLeftBoundKey + " in game)"))).build();

        ButtonWidget useClassicIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.CLASSIC ? Text.literal("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.CLASSIC ? Text.literal("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.CLASSIC, true);
        }).dimensions(5, 5, 20, 20).tooltip(Tooltip.of(Text.literal("Use classic styled icons and colors"))).build();

        ButtonWidget usePvPTiersIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.PVPTIERS ? Text.literal("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.PVPTIERS ? Text.literal("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.PVPTIERS, true);
        }).dimensions(30, 5, 20, 20).tooltip(Tooltip.of(Text.literal("Use PvPTiers styled icons and colors"))).build();

        ButtonWidget useMCTiersIcons = ButtonWidget.builder(TiersClient.activeIcons == Icons.Type.MCTIERS ? Text.literal("●") : Text.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(TiersClient.activeIcons == Icons.Type.MCTIERS ? Text.literal("●") : Text.empty());
            TiersClient.changeIcons(Icons.Type.MCTIERS, true);
        }).dimensions(55, 5, 20, 20).tooltip(Tooltip.of(Text.literal("Use MCTiers styled icons and colors"))).build();

        switch (TiersClient.activeIcons) {
            case CLASSIC -> useClassicIcons.active = false;
            case PVPTIERS -> usePvPTiersIcons.active = false;
            case MCTIERS -> useMCTiersIcons.active = false;
        }

        updateVisibilities();
        updateActiveButtonsState();
        buttonsReady = true;

        Stream.of(toggleMod, toggleRegion, toggleIcons, togglePeak, toggleTab, toggleChat, toggleSeparatorMode, cycleDisplayMode, autoKitDetect, clearPlayerCache, resetSettings, leftMCTiers, centerMCTiers, rightMCTiers, leftPvPTiers, centerPvPTiers, rightPvPTiers, leftSubtiers, centerSubtiers, rightSubtiers, activeRightMode, activeLeftMode, enableOwnProfile, useClassicIcons, usePvPTiersIcons, useMCTiersIcons)
                .forEach(this::addDrawableChild);
    }

    private void updateActiveButtonsState() {
        toggleRegion.active = TiersClient.toggleMod;
        toggleIcons.active = TiersClient.toggleMod;
        togglePeak.active = TiersClient.toggleMod;
        toggleTab.active = TiersClient.toggleMod;
        toggleChat.active = TiersClient.toggleMod;
        toggleSeparatorMode.active = TiersClient.toggleMod;
        cycleDisplayMode.active = TiersClient.toggleMod;
        autoKitDetect.active = TiersClient.toggleMod;
        leftMCTiers.active = TiersClient.toggleMod;
        centerMCTiers.active = TiersClient.toggleMod;
        rightMCTiers.active = TiersClient.toggleMod;
        leftPvPTiers.active = TiersClient.toggleMod;
        centerPvPTiers.active = TiersClient.toggleMod;
        rightPvPTiers.active = TiersClient.toggleMod;
        leftSubtiers.active = TiersClient.toggleMod;
        centerSubtiers.active = TiersClient.toggleMod;
        rightSubtiers.active = TiersClient.toggleMod;
        activeLeftMode.active = TiersClient.toggleMod;
        activeRightMode.active = TiersClient.toggleMod;

        switch (TiersClient.positionMCTiers) {
            case RIGHT -> rightMCTiers.active = false;
            case OFF -> centerMCTiers.active = false;
            case LEFT -> leftMCTiers.active = false;
        }

        switch (TiersClient.positionPvPTiers) {
            case RIGHT -> rightPvPTiers.active = false;
            case OFF -> centerPvPTiers.active = false;
            case LEFT -> leftPvPTiers.active = false;
        }

        switch (TiersClient.positionSubtiers) {
            case RIGHT -> rightSubtiers.active = false;
            case OFF -> centerSubtiers.active = false;
            case LEFT -> leftSubtiers.active = false;
        }
    }

    private void updateVisibilities() {
        activeRightMode.visible = TiersClient.positionMCTiers == TiersClient.DisplayStatus.RIGHT || TiersClient.positionPvPTiers == TiersClient.DisplayStatus.RIGHT || TiersClient.positionSubtiers == TiersClient.DisplayStatus.RIGHT;
        activeLeftMode.visible = TiersClient.positionMCTiers == TiersClient.DisplayStatus.LEFT || TiersClient.positionPvPTiers == TiersClient.DisplayStatus.LEFT || TiersClient.positionSubtiers == TiersClient.DisplayStatus.LEFT;
    }

    private void drawPlayerAvatar(DrawContext context, int x, int y) {
        if (imageReady) {
            if (ownProfile.imageSaved == 1 || ownProfile.imageSaved == 2)
                context.drawTexture(RenderLayer::getGuiTextured, playerAvatarTexture, x - height / 10 / 2, y, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
            else if (ownProfile.imageSaved < 6 && ownProfile.imageSaved > 2)
                context.drawTexture(RenderLayer::getGuiTextured, playerAvatarTexture, x - height / 7 / 2, y, 0, 0, height / 7, (int) (height / 4.145), height / 7, (int) (height / 4.145));
        } else if (ownProfile.imageSaved != 0) {
            loadPlayerAvatar();
        } else if (ownProfile.numberOfImageRequests >= 6)
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(ownProfile.name + "'s skin failed to load. Restart game to retry"), x, y + 50, ColorControl.getColorMinecraftStandard("red"));
    }

    private void loadPlayerAvatar() {
        if (imageReady || imageError)
            return;

        try (FileInputStream fileInputStream = new FileInputStream(FabricLoader.getInstance().getGameDir().resolve("cache/tiers/" + (useOwnProfile ? ownProfile.uuid : defaultProfile.uuid) + ".png").toFile())) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(playerAvatarTexture, new NativeImageBackedTexture(NativeImage.read(fileInputStream)));
            imageReady = true;
        } catch (IOException ignored) {
            LOGGER.warn("Error loading player skin");
            imageError = true;
        }
    }

    public static Screen getConfigScreen(Screen ignoredScreen) {
        return new ConfigScreen();
    }
}