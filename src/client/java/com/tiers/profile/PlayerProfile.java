package com.tiers.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiers.TiersClient;
import com.tiers.misc.Mode;
import com.tiers.profile.types.PvPTiersProfile;
import com.tiers.profile.types.SuperProfile;
import com.tiers.textures.ColorControl;
import com.tiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tiers.TiersClient.*;

public class PlayerProfile {
    public static final CopyOnWriteArrayList<PlayerProfile> failedPlayerProfiles = new CopyOnWriteArrayList<>();
    public static final AtomicInteger playerProfilesRequests = new AtomicInteger(0);
    public static final AtomicInteger failedPlayerProfilesRequests = new AtomicInteger(0);
    public Status status;
    public int imageSaved;
    public int numberOfImageRequests;
    public String inGameName;
    public String targetName;
    public boolean nameChanged;

    public String name = "";
    public String uuid = "";

    public PvPTiersProfile profilePvPTiers;

    public Text toAppendLeft = Text.empty();
    public Text toAppendRight = Text.empty();
    private Text fullName;
    private Text deepReplaceName;

    private int numberOfRequests;
    private final boolean regular;

    private static final String UUID_API_1 = "https://playerdb.co/api/player/minecraft/";
    private static final String UUID_API_2 = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String UUID_API_3 = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static boolean forceNewRequest;

    public PlayerProfile(String name, boolean regular) {
        if (name.contains("-force")) {
            String[] content = name.split("-");
            if (content.length == 2) {
                name = content[0];
                forceNewRequest = true;
            }
        }

        playerProfilesRequests.incrementAndGet();
        this.regular = regular;
        this.name = name;
        inGameName = name;
        targetName = name;

        status = !name.matches("^[a-zA-Z0-9_]{3,16}$") ? Status.NOT_PLAYER : Status.SEARCHING;
    }

    public PlayerProfile(String mojangJson, String jsonPvPTiers) {
        regular = false;

        if (JsonParser.parseString(mojangJson).isJsonNull()) {
            status = Status.API_ISSUE;
            failedRequest();
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(mojangJson).getAsJsonObject();

        if (jsonObject.has("name") && jsonObject.has("id")) {
            name = jsonObject.get("name").getAsString();
            uuid = jsonObject.get("id").getAsString();
        } else {
            status = Status.NOT_EXISTING;
            return;
        }

        Path path = FabricLoader.getInstance().getGameDir().resolve("cache/tiers/06ec3577329945fabbdf613b1f86c8ab.png");

        try (InputStream inputStream = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of("minecraft", "textures/default.png")).orElseThrow().getInputStream()) {
            if (inputStream == null)
                throw new IOException();

            Files.createDirectories(path.getParent());
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            LOGGER.warn("Error copying default skin");
        }

        profilePvPTiers = new PvPTiersProfile(jsonPvPTiers);

        status = Status.READY;
    }

    public void prepareToRebuild() {
        status = !name.matches("^[a-zA-Z0-9_]{3,16}$") ? Status.NOT_PLAYER : Status.SEARCHING;
        numberOfRequests = 0;
        forceNewRequest = false;
        playerProfilesRequests.incrementAndGet();
        failedPlayerProfilesRequests.decrementAndGet();
    }

    public void buildRequest() {
        if (status != Status.SEARCHING)
            return;

        if (numberOfRequests >= 3) {
            buildRequest(UUID_API_2);
            return;
        }

        numberOfRequests++;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(UUID_API_1 + name))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            int statusCode = response.statusCode();

            if (statusCode == 400 || statusCode == 500) {
                status = Status.NOT_EXISTING;
                return;
            } else if (statusCode != 200) {
                buildRequest(UUID_API_2);
                return;
            }
            parseJson(response.body());
        }).exceptionally(ignored -> {
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(this::buildRequest);
            return null;
        });
    }

    public void buildRequest(String apiUrl) {
        if (numberOfRequests >= 12 || status != Status.SEARCHING) {
            status = Status.TIMEOUTED;
            return;
        }

        numberOfRequests++;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + name))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.body().contains("minecraft/profile/lookup")) {
                status = Status.API_ISSUE;
                return;
            }

            int statusCode = response.statusCode();

            if (statusCode == 404 || statusCode == 400) {
                status = Status.NOT_EXISTING;
                return;
            } else if (statusCode == 403) {
                CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS).execute(() -> buildRequest(UUID_API_3));
                return;
            } else if (statusCode != 200) {
                long delay = switch (numberOfRequests) {
                    case 1 -> 50;
                    case 2, 3 -> 100;
                    case 4, 5 -> 400;
                    case 6, 7 -> 900;
                    default -> 1500;
                };
                CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> buildRequest(apiUrl));
                return;
            }
            parseJson(response.body());
        }).exceptionally(ignored -> {
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> buildRequest(apiUrl));
            return null;
        });
    }

    public void savePlayerImage() {
        String apiUrl = "https://mc-heads.net/body/";

        if (numberOfImageRequests == 2)
            apiUrl = "https://visage.surgeplay.com/full/432/";
        else if (numberOfImageRequests == 4)
            apiUrl = "https://render.crafty.gg/3d/full/";
        else if (numberOfImageRequests >= 6)
            return;

        final String finalApiUrl = apiUrl + uuid;

        numberOfImageRequests++;

        String path = FabricLoader.getInstance().getGameDir() + "/cache/tiers/" + (regular ? "players/" : "");
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(Paths.get(path));
                URL uri = new URI(finalApiUrl).toURL();
                HttpURLConnection httpURLConnection = (HttpURLConnection) uri.openConnection();
                httpURLConnection.setRequestProperty("User-Agent", userAgent);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);

                try (InputStream inputStream = httpURLConnection.getInputStream()) {
                    ImageIO.write(ImageIO.read(inputStream), "png", new File(path + uuid + ".png"));
                    imageSaved = numberOfImageRequests;
                }
            } catch (IOException | URISyntaxException ignored) {
                CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(this::savePlayerImage);
            }
        });
    }

    private void parseJson(String json) {
        if (JsonParser.parseString(json).isJsonNull()) {
            status = Status.API_ISSUE;
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        if (jsonObject.has("code") && jsonObject.has("data") && jsonObject.has("success")) {
            if (!jsonObject.get("success").getAsString().contains("true")) {
                buildRequest(UUID_API_2);
                return;
            }
            JsonObject data = jsonObject.getAsJsonObject("data");
            if (data.has("player")) {
                JsonObject player = data.getAsJsonObject("player");
                if (player.has("username") && player.has("raw_id") && player.has("id")) {
                    name = player.get("username").getAsString();
                    uuid = player.get("raw_id").getAsString();
                }
            }
        } else if (jsonObject.has("name") && jsonObject.has("id")) {
            name = jsonObject.get("name").getAsString();
            uuid = jsonObject.get("id").getAsString();
        }

        if (uuid.isEmpty()) {
            status = Status.NOT_EXISTING;
            return;
        }

        if (!regular)
            savePlayerImage();

        if (!inGameName.equalsIgnoreCase(name))
            nameChanged = true;

        targetName = nameChanged ? inGameName : name;

        updateTierlistProfiles(0);
        status = Status.READY;
        readyPlayerProfiles.put(targetName, this);
    }

    private void failedRequest() {
        synchronized (PlayerProfile.class) {
            failedPlayerProfilesRequests.incrementAndGet();
        }

        failedPlayerProfiles.add(this);
    }

    public static void resetRequestCounters() {
        synchronized (PlayerProfile.class) {
            playerProfilesRequests.set(0);
            failedPlayerProfilesRequests.set(0);
        }
    }

    public void updateTierlistProfiles(int mode) {
        CompletableFuture.runAsync(() -> {
            String extra = (forceNewRequest || mode != 0) ? ("?" + System.currentTimeMillis()) : "";
            forceNewRequest = false;

            if (mode != 0)
                TiersClient.showUpdatedPlayerProfile(this, false);

            if (mode == 0 || mode == 2)
                profilePvPTiers = new PvPTiersProfile("https://pvptiers.com/api/profile/", uuid, extra);

            profilePvPTiers.setOnUpdate(this::updateAppendingText);
        });
    }

    public void updateAppendingText() {
        toAppendRight = Text.empty();
        toAppendLeft = Text.empty();

        if (positionPvPTiers == DisplayStatus.RIGHT)
            toAppendRight = updateProfileNameRight(profilePvPTiers, activePvPTiersMode);
        else if (positionPvPTiers == DisplayStatus.LEFT)
            toAppendLeft = updateProfileNameLeft(profilePvPTiers, activePvPTiersMode);

        cachesDirty = true;
    }

    public Text getFullName() {
        Text playerText = nameChanged ? Text.of(inGameName + " (AKA " + name + ")") : Text.of(name);

        if (!toggleMod)
            return playerText;

        return Text.empty()
                .append(toAppendLeft.copy())
                .append(playerText)
                .append(toAppendRight.copy());
    }

    public Text getFullName(Text original) {
        if (status != Status.READY)
            return original;

        return fullName = Text.empty()
                .append(toAppendLeft.copy())
                .append(original.copy())
                .append(toAppendRight.copy());
    }

    private Text updateProfileNameRight(SuperProfile superProfile, Mode activeMode) {
        MutableText returnValue = Text.empty();

        if (superProfile != null && superProfile.status == Status.READY) {
            GameMode shown = superProfile.getGameMode(activeMode);

            if ((shown == null || shown.status == Status.SEARCHING) || (shown.status == Status.NOT_EXISTING && displayMode == ModesTierDisplay.SELECTED))
                return returnValue;

            if (displayMode == ModesTierDisplay.ADAPTIVE_HIGHEST && shown.status == Status.NOT_EXISTING && superProfile.highest != null)
                shown = superProfile.highest;

            if (displayMode == ModesTierDisplay.HIGHEST && superProfile.highest != null && superProfile.highest.getTierPoints(false) > shown.getTierPoints(false))
                shown = superProfile.highest;

            if (shown == null || shown.status != Status.READY)
                return returnValue;

            MutableText separator = Text.literal(" | ").setStyle(toggleAdaptiveSeparator ? shown.displayedTier.getStyle() : Style.EMPTY.withColor(ColorControl.getColor("static_separator")));
            returnValue.append(Text.empty().append(separator).append(shown.displayedTier));

            if (toggleIcons)
                returnValue.append(Text.literal(" ").append(shown.gamemode.getIconTag()));
        }
        return returnValue;
    }

    private Text updateProfileNameLeft(SuperProfile superProfile, Mode activeMode) {
        MutableText returnValue = Text.empty();

        if (superProfile != null && superProfile.status == Status.READY) {
            GameMode shown = superProfile.getGameMode(activeMode);

            if ((shown == null || shown.status == Status.SEARCHING) || (shown.status == Status.NOT_EXISTING && displayMode == ModesTierDisplay.SELECTED))
                return returnValue;

            if (displayMode == ModesTierDisplay.ADAPTIVE_HIGHEST && shown.status == Status.NOT_EXISTING && superProfile.highest != null)
                shown = superProfile.highest;

            if (displayMode == ModesTierDisplay.HIGHEST && superProfile.highest != null && superProfile.highest.getTierPoints(false) > shown.getTierPoints(false))
                shown = superProfile.highest;

            if (shown == null || shown.status != Status.READY)
                return returnValue;

            MutableText separator = Text.literal(" | ").setStyle(toggleAdaptiveSeparator ? shown.displayedTier.getStyle() : Style.EMPTY.withColor(ColorControl.getColor("static_separator")));

            if (toggleIcons)
                returnValue = Text.empty().append(shown.gamemode.getIconTag()).append(" ");
            returnValue.append(Text.empty().append(shown.displayedTier).append(separator));
        }
        return returnValue;
    }

    public void resetDrawnStatus() {
        if (profilePvPTiers == null)
            return;
        profilePvPTiers.apiErrorShown = false;
        profilePvPTiers.drawn = false;
        profilePvPTiers.gameModes.forEach(gameMode -> gameMode.drawn = false);
    }

    public boolean isPlayerValid() {
        if (status == Status.NOT_EXISTING) {
            sendMessageToPlayer(Icons.colorText(name + " was not found or isn't a premium account", "red"), false);
            return false;
        } else if (status == Status.NOT_PLAYER) {
            sendMessageToPlayer(Icons.colorText("Not a valid player name", "red"), false);
            return false;
        } else if (status == Status.TIMEOUTED) {
            sendMessageToPlayer(Icons.colorText(name + "'s search was timeouted. Clear cache and retry", "red"), false);
            return false;
        } else if (status == Status.API_ISSUE) {
            sendMessageToPlayer(Icons.colorText(name + "'s search failed: API Issue. Update Tiers or retry in a while", "red"), false);
            return false;
        }
        return true;
    }

    public static Text getFullyReplaced(Text original) {
        String originalString = original.getString();
        for (PlayerProfile playerProfile : TiersClient.playerProfiles)
            if (playerProfile.status == Status.READY && originalString.contains(playerProfile.targetName))
                original = playerProfile.deepReplace(original);

        return original;
    }

    public Text deepReplace(Text original) {
        if (status == Status.NOT_PLAYER)
            return getFullyReplaced(original);

        if (status != Status.READY || !original.getString().contains(targetName))
            return original;

        ArrayList<StyledChar> characters = new ArrayList<>();
        StringBuilder fullBuilder = new StringBuilder();

        original.visit((style, string) -> {
            fullBuilder.append(string);
            for (int i = 0; i < string.length(); i++)
                characters.add(new StyledChar(string.charAt(i), style));
            return Optional.empty();
        }, Style.EMPTY);

        String full = fullBuilder.toString();
        if (!full.contains(targetName))
            return original;

        ArrayList<int[]> matches = new ArrayList<>();
        int searchStart = 0;
        int markerCount = 0;
        int index;

        while ((index = full.indexOf(targetName, searchStart)) != -1) {
            for (int i = searchStart; i < index; i++)
                if (full.charAt(i) == '\u200C')
                    markerCount++;

            if (markerCount % 2 == 0)
                matches.add(new int[]{index, index + targetName.length()});

            searchStart = index + targetName.length();
        }

        if (matches.isEmpty())
            return original;

        MutableText result = Text.empty();
        int i = 0;
        int matchIndex = 0;

        while (i < characters.size()) {
            if (matchIndex < matches.size() && i == matches.get(matchIndex)[0]) {
                int end = matches.get(matchIndex)[1];

                MutableText namePart = Text.empty();
                appendChunkedText(characters, i, end, namePart);

                result.append(Text.literal("\u200C"));
                result.append(getFullName(namePart));
                result.append(Text.literal("\u200C"));

                i = end;
                matchIndex++;
                continue;
            }

            int nextBoundary = (matchIndex < matches.size()) ? matches.get(matchIndex)[0] : characters.size();
            appendChunkedText(characters, i, nextBoundary, result);
            i = nextBoundary;
        }

        return deepReplaceName = result;
    }

    private void appendChunkedText(ArrayList<StyledChar> characters, int start, int end, MutableText dest) {
        if (start >= end)
            return;

        Style currentStyle = characters.get(start).style;
        StringBuilder chunkBuilder = new StringBuilder();

        for (int i = start; i < end; i++) {
            StyledChar character = characters.get(i);

            if (!character.style.equals(currentStyle)) {
                dest.append(Text.literal(chunkBuilder.toString()).setStyle(currentStyle));
                chunkBuilder.setLength(0);
                currentStyle = character.style;
            }
            chunkBuilder.append(character.character);
        }

        if (!chunkBuilder.isEmpty())
            dest.append(Text.literal(chunkBuilder.toString()).setStyle(currentStyle));
    }

    private record StyledChar(char character, Style style) {
    }

    @Override
    public String toString() {
        return name + "'sPlayerProfile{" +
                "\nstatus=" + status +
                "\nimageSaved=" + imageSaved +
                "\nnumberOfImageRequests=" + numberOfImageRequests +
                "\ninGameName=" + (inGameName != null ? inGameName : "null") +
                "\nnameChanged=" + nameChanged +
                "\ntargetName=" + targetName +
                "\nuuid=" + (uuid != null ? uuid : "null") +
                "\ntoAppendLeft=" + (toAppendLeft != null ? toAppendLeft.getString() : "null") +
                "\ntoAppendRight=" + (toAppendRight != null ? toAppendRight.getString() : "null") +
                "\nfullName=" + (fullName != null ? fullName.getString() : "null") +
                "\ndeepReplaceName=" + (deepReplaceName != null ? deepReplaceName.getString() : "null") +
                "\nnumberOfRequests=" + numberOfRequests +
                "\nregular=" + regular +
                "\n\nprofilePvPTiers=" + (profilePvPTiers != null ? profilePvPTiers : "null") +
                "}\n\n\n--- NEXT ---\n\n\n";
    }
}