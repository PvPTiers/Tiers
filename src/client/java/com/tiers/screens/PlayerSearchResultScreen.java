package com.tiers.screens;

import com.tiers.TiersClient;
import com.tiers.profile.GameMode;
import com.tiers.profile.PlayerProfile;
import com.tiers.profile.Status;
import com.tiers.profile.types.PvPTiersProfile;
import com.tiers.profile.types.SuperProfile;
import com.tiers.textures.ColorControl;
import com.tiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.FileInputStream;
import java.io.IOException;

import static com.tiers.TiersClient.LOGGER;

public class PlayerSearchResultScreen extends Screen {
    private final PlayerProfile playerProfile;
    private final Identifier playerAvatarTexture = Identifier.of("");

    ButtonWidget dimensionsWarning;

    private int separator;
    private boolean small;
    private boolean tooSmall;
    private boolean imageReady;
    private boolean toastShown;

    public PlayerSearchResultScreen(PlayerProfile playerProfile) {
        super(Text.of(playerProfile.name));
        this.playerProfile = playerProfile;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!playerProfile.isPlayerValid()) {
            close();
            return;
        }

        if (playerProfile.nameChanged && !toastShown) {
            MinecraftClient.getInstance().getToastManager().add(SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Recent name change"), Text.of("(" + playerProfile.name + " to " + playerProfile.inGameName + ") Data should be accurate")));
            toastShown = true;
        }

        int centerX = width / 2;
        int listY = (int) (height / 2.65);
        separator = height / 23;
        small = width < 575 || height < 420;
        tooSmall = width < 450 || height < 262;
        int avatarY = height / 55 + 12;

        super.render(context, mouseX, mouseY, delta);

        dimensionsWarning.visible = small;
        if (tooSmall) {
            dimensionsWarning.setMessage(Text.of("⚠"));
            dimensionsWarning.setTooltip(Tooltip.of(Text.of("Your window dimensions (" + width + "x" + height + ") are too small\nLower the GUI scale or make the window bigger! (min: 450x262)")));
        }

        if (playerProfile.status == Status.SEARCHING) {
            context.drawCenteredTextWithShadow(textRenderer, Text.of("Searching for " + playerProfile.name + "..."), centerX, listY, ColorControl.getColorMinecraftStandard("green"));
            return;
        }

        if (playerProfile.numberOfImageRequests == 0)
            playerProfile.savePlayerImage();

        drawPlayerAvatar(context, centerX, avatarY);
        if (!imageReady)
            context.drawCenteredTextWithShadow(textRenderer, Text.of("Loading " + playerProfile.name + "'s skin"), centerX, avatarY + 50, ColorControl.getColorMinecraftStandard("green"));

        context.drawCenteredTextWithShadow(textRenderer, playerProfile.getFullName(), centerX, height / 55, Colors.WHITE);

        drawCategoryList(context, PvPTiersProfile.PVPTIERS_IMAGE, playerProfile.profilePvPTiers, centerX, listY);
    }

    private void drawCategoryList(DrawContext context, Identifier image, SuperProfile superProfile, int x, int y) {
        if (superProfile == null) {
            context.drawCenteredTextWithShadow(textRenderer, "Loading from API...", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("green"));
            return;
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, image, x - 12, (int) (y + 2.4 * separator) + 4 - 38, 0, 0, 24, 24, 24, 24);

        if (superProfile.status == Status.SEARCHING) {
            context.drawCenteredTextWithShadow(textRenderer, "Searching...", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("green"));
            return;
        } else if (superProfile.status == Status.NOT_EXISTING) {
            context.drawCenteredTextWithShadow(textRenderer, "Unranked", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            return;
        } else if (superProfile.status == Status.TIMEOUTED) {
            context.drawCenteredTextWithShadow(textRenderer, "Search timeouted", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            renderFailedRequestMessage(context, superProfile, x, y);
            return;
        } else if (superProfile.status == Status.API_ISSUE) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Search failed: API issue").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.of("hello")))), x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            renderFailedRequestMessage(context, superProfile, x, y);

            context.drawCenteredTextWithShadow(textRenderer, "Update Tiers or retry in a while", x, (int) (y + 2.8 * separator + 50), Colors.YELLOW);
            if (!superProfile.apiErrorShown) {
                addDrawableChild(ButtonWidget.builder(Text.literal("Report issue"), (buttonWidget) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.setScreen(new ConfirmLinkScreen((confirmed) -> {
                        if (confirmed)
                            Util.getOperatingSystem().open("https://github.com/PvPTiers/Tiers/issues");
                        client.setScreen(this);
                    }, "https://github.com/PvPTiers/Tiers/issues", true));
                }).dimensions(x - 40, (int) (y + 2.8 * separator + 50 + 12), 80, 20).tooltip(Tooltip.of(Text.of("Report this issue on GitHub. Make sure to report only if the same search on either mctiers.com, pvptiers.com or subtiers.com doesn't fail"))).build());
                superProfile.apiErrorShown = true;
            }
            return;
        }

        if (!superProfile.drawn) {
            TextWidget regionLabel = new TextWidget(Icons.colorText("Region", "region"), textRenderer);
            regionLabel.setPosition(x - 44, (int) (y + 2.4 * separator));
            addDrawableChild(regionLabel);

            TextWidget overallLabel = new TextWidget(Icons.colorText("Overall", "overall"), textRenderer);
            overallLabel.setPosition(x - 44, (int) (y + 2.4 * separator) + 16);
            addDrawableChild(overallLabel);

            TextWidget regionIcon = new TextWidget(Icons.GLOBE, textRenderer);
            regionIcon.setPosition(x - 64, (int) (y + 2.4 * separator + 2));
            regionIcon.setTooltip(Tooltip.of(regionLabel.getMessage()));
            addDrawableChild(regionIcon);

            TextWidget overallIcon = new TextWidget(Icons.OVERALL, textRenderer);
            overallIcon.setPosition(x - 64, (int) (y + 2.4 * separator + 2) + 16);
            overallIcon.setTooltip(Tooltip.of(overallLabel.getMessage()));
            addDrawableChild(overallIcon);

            TextWidget region = new TextWidget(superProfile.displayedRegion, textRenderer);
            region.setPosition(x + 52 - (superProfile.displayedRegion.getString().length() - 2) * 3, (int) (y + 2.4 * separator));
            region.setTooltip(Tooltip.of(superProfile.regionTooltip));
            addDrawableChild(region);

            TextWidget overall = new TextWidget(superProfile.displayedOverall, textRenderer);
            overall.setPosition(x + 52 - (superProfile.displayedOverall.getString().length() - 2) * 3, (int) (y + 2.4 * separator) + 16);
            overall.setTooltip(Tooltip.of(superProfile.overallTooltip));
            addDrawableChild(overall);

            drawTierList(superProfile, x - 64, (int) (y + 2.4 * separator) + 40);

            superProfile.drawn = true;
        }
    }

    private void renderFailedRequestMessage(DrawContext context, SuperProfile superProfile, int x, int y) {
        String[] messages = superProfile.checkOutages();
        context.drawCenteredTextWithShadow(textRenderer, messages[0], x, (int) (y + 2.8 * separator) + 15, Colors.YELLOW);
        context.drawCenteredTextWithShadow(textRenderer, messages[1], x, (int) (y + 2.8 * separator) + 25, Colors.YELLOW);
        context.drawCenteredTextWithShadow(textRenderer, messages[2], x, (int) (y + 2.8 * separator) + 35, Colors.YELLOW);
    }

    private void drawTierList(SuperProfile superProfile, int x, int y) {
        int originalX = x;
        if (small) {
            y -= 7;
            int count = 1;
            int stage = 0;
            for (GameMode gameMode : superProfile.gameModes) {
                if (drawGameModeTiers(gameMode, x + 5, y + stage * 36)) {
                    x += 35;
                    if (count % 4 == 0) {
                        stage++;
                        x = originalX;
                    }
                    count++;
                }
            }
        } else {
            for (GameMode gameMode : superProfile.gameModes)
                if (drawGameModeTiers(gameMode, x, y)) y += 15;
        }
    }

    private boolean drawGameModeTiers(GameMode mode, int x, int y) {
        if (mode.drawn || mode.status != Status.READY)
            return false;

        TextWidget icon = new TextWidget(mode.gamemode.getIcon(), textRenderer);
        icon.setPosition(x, y + 3);
        if (small)
            icon.setPosition(x, y + 3);
        icon.setTooltip(Tooltip.of(mode.gamemode.getTextLabel()));
        addDrawableChild(icon);

        TextWidget label = new TextWidget(mode.gamemode.getTextLabel(), textRenderer);
        label.setPosition(x + 20, y);
        if (!small)
            addDrawableChild(label);

        TextWidget tier = new TextWidget(mode.displayedTier, textRenderer);
        tier.setPosition(x + 114 - (mode.displayedTier.getString().length() - 3) * 3, y);
        if (small)
            tier.setPosition(x - 2 - (mode.displayedTier.getString().length() - 3) * 2, y + 14);
        tier.setTooltip(Tooltip.of(mode.tierTooltip));
        addDrawableChild(tier);

        if (mode.hasPeak && mode.peakTierTooltip.getStyle().getColor() != null) {
            TextWidget peakTier = new TextWidget(mode.displayedPeakTier, textRenderer);
            peakTier.setPosition(x + 136, y);
            if (small)
                peakTier.setPosition(x - 6, y + 24);
            peakTier.setTooltip(Tooltip.of(mode.peakTierTooltip));
            addDrawableChild(peakTier);
        }

        mode.drawn = true;

        return true;
    }

    private void drawPlayerAvatar(DrawContext context, int x, int y) {
        if (imageReady) {
            if (playerProfile.imageSaved == 1 || playerProfile.imageSaved == 2)
                context.drawTexture(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - width / 32, y, 0, 0, width / 16, (int) (width / 6.666), width / 16, (int) (width / 6.666));
            else if (playerProfile.imageSaved < 6 && playerProfile.imageSaved > 2)
                context.drawTexture(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, (int) (x - width / 22.5), y, 0, 0, (int) (width / 11.25), (int) (width / 6.666), (int) (width / 11.25), (int) (width / 6.666));
        } else if (playerProfile.imageSaved != 0) {
            loadPlayerAvatar();
        } else if (playerProfile.numberOfImageRequests >= 6)
            context.drawCenteredTextWithShadow(textRenderer, Text.of(playerProfile.name + "'s skin failed to load. Clear cache and retry"), x, y + 50, ColorControl.getColorMinecraftStandard("red"));
    }

    private void loadPlayerAvatar() {
        if (imageReady)
            return;

        try (FileInputStream fileInputStream = new FileInputStream(FabricLoader.getInstance().getGameDir().resolve("cache/tiers/players/" + playerProfile.uuid + ".png").toFile())) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(playerAvatarTexture, new NativeImageBackedTexture(null, NativeImage.read(fileInputStream)));
            imageReady = true;
        } catch (IOException ignored) {
            LOGGER.warn("Error loading player skin");
        }
    }

    @Override
    public void init() {
        playerProfile.resetDrawnStatus();

        dimensionsWarning = ButtonWidget.builder(Text.of("ℹ"), (buttonWidget) -> {
        }).dimensions(width - 20 - 5, 5, 20, 20).tooltip(Tooltip.of(Text.of("Your window dimensions (" + width + "x" + height + ") are small\nLower the GUI scale or make the window bigger to have a better experience (ideal: 575x420)"))).build();
        dimensionsWarning.active = false;
        dimensionsWarning.visible = small;
        if (tooSmall) {
            dimensionsWarning.setMessage(Text.of("⚠"));
            dimensionsWarning.setTooltip(Tooltip.of(Text.of("Your window dimensions (" + width + "x" + height + ") are too small\nLower the GUI scale or make the window bigger! (min: 430x262)")));
        }

        addDrawableChild(dimensionsWarning);

        addDrawableChild(ButtonWidget.builder(Icons.NAMEMC, (ignored) -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            minecraftClient.setScreen(new ConfirmLinkScreen((confirmed) -> {
                if (confirmed)
                    Util.getOperatingSystem().open("https://namemc.com/profile/" + playerProfile.uuid);
                minecraftClient.setScreen(this);
            }, "https://namemc.com/profile/" + playerProfile.uuid, true));
        }).dimensions(width - 20 - 5, height - 20 - 5, 20, 20).tooltip(Tooltip.of(Text.literal("Open " + playerProfile.targetName + "'s NameMC page"))).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Update"), (buttonWidget) -> TiersClient.showUpdatedPlayerProfile(playerProfile, true)).dimensions(5, height - 20 - 5, 68, 20).tooltip(Tooltip.of(Text.of("Reload the player profile"))).build());
        addDrawableChild(ButtonWidget.builder(Icons.CYCLE, (buttonWidget) -> playerProfile.updateTierlistProfiles(2)).dimensions(5, height - 20 - 5 - 22, 20, 20).tooltip(Tooltip.of(Text.literal(("Update PvPTiers results")))).build());
    }
}