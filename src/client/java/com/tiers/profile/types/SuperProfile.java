package com.tiers.profile.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiers.TiersClient;
import com.tiers.misc.Mode;
import com.tiers.profile.GameMode;
import com.tiers.profile.Status;
import com.tiers.textures.Icons;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tiers.TiersClient.*;

public class SuperProfile {
    private static final ScheduledExecutorService updateAndRecoverFailedRequestsScheduler = Executors.newSingleThreadScheduledExecutor();
    public static final CopyOnWriteArrayList<SuperProfile> failedSuperProfiles = new CopyOnWriteArrayList<>();
    public static final AtomicInteger PvPTiersRequests = new AtomicInteger(0);
    public static final AtomicInteger failedPvPTiersRequests = new AtomicInteger(0);
    public static final AtomicInteger failedPvPTiersRequestsLastMinute = new AtomicInteger(0);
    public static boolean isPvPTiersDown = false;
    public static int numberOfMessages;

    public Status status = Status.SEARCHING;
    private int numberOfRequests;

    private String region;
    private int points;
    private int overallPosition;

    public Text displayedRegion;
    public Text displayedOverall;
    public Text overallTooltip;
    public Text regionTooltip;

    public final ArrayList<GameMode> gameModes = new ArrayList<>();
    public GameMode highest;

    public String originalJson;
    public String apiUrl;
    public String uuid;
    public String discordId;
    public boolean drawn;
    public boolean apiErrorShown;
    private Runnable onUpdate;

    protected SuperProfile() {
        if (this instanceof PvPTiersProfile)
            PvPTiersRequests.incrementAndGet();
    }

    static {
        updateAndRecoverFailedRequestsScheduler.scheduleAtFixedRate(SuperProfile::updateAndRecoverFailedRequests, 0, 1, TimeUnit.MINUTES);
        updateAndRecoverFailedRequestsScheduler.scheduleAtFixedRate(SuperProfile::updateDownStatus, 0, 1, TimeUnit.SECONDS);
    }

    private static void updateDownStatus() {
        isPvPTiersDown = failedPvPTiersRequestsLastMinute.get() > 3;
    }

    private static void updateAndRecoverFailedRequests() {
        failedPvPTiersRequestsLastMinute.set(0);

        for (SuperProfile superProfile : SuperProfile.failedSuperProfiles) {

            if (superProfile instanceof PvPTiersProfile && isPvPTiersDown)
                continue;

            SuperProfile.failedSuperProfiles.remove(superProfile);
            superProfile.prepareToRebuild();
            superProfile.buildRequest(superProfile.apiUrl, superProfile.uuid, "");
            try {
                Thread.sleep(150);
            } catch (InterruptedException interruptedException) {
                TiersClient.LOGGER.error("Error while recovering failed super profile. Details: {}\n{}", interruptedException.getMessage(), superProfile);
            }
        }
    }

    public void prepareToRebuild() {
        status = Status.SEARCHING;
        numberOfRequests = 0;

        if (this instanceof PvPTiersProfile) {
            PvPTiersRequests.incrementAndGet();
            failedPvPTiersRequests.decrementAndGet();
        }
    }

    public void buildRequest(String apiUrl, String uuid, String extra) {
        if (apiUrl == null || uuid == null || extra == null) {
            status = Status.API_ISSUE;
            return;
        }
        this.apiUrl = apiUrl;
        this.uuid = uuid;

        if (numberOfRequests >= 5 || status != Status.SEARCHING) {
            status = Status.TIMEOUTED;
            failedRequest();
            return;
        }

        numberOfRequests++;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + uuid + extra))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            int statusCode = response.statusCode();
            if (statusCode == 404) {
                status = Status.NOT_EXISTING;
                return;
            } else if (statusCode == 502 || statusCode == 525) {
                numberOfRequests++;
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> buildRequest(apiUrl, uuid, extra));
                return;
            } else if (statusCode != 200) {
                status = Status.API_ISSUE;
                failedRequest();
                LOGGER.info("Tiers | Request failed ({}) | Code: {} | Body: {}", apiUrl + uuid + extra, statusCode, response.body());
                return;
            }

            parseJson(response.body());
        }).exceptionally(ignored -> {
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> buildRequest(apiUrl, uuid, extra));
            return null;
        });
    }

    public void parseJson(String json) {
        if (JsonParser.parseString(json).isJsonNull()) {
            status = Status.API_ISSUE;
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        if (jsonObject.has("name") && jsonObject.has("region") &&
                jsonObject.has("points") && jsonObject.has("overall") && jsonObject.has("rankings") && jsonObject.get("rankings").isJsonObject()) {
            if (!jsonObject.get("region").isJsonNull())
                region = jsonObject.get("region").getAsString();
            else
                region = "Unknown";
            points = jsonObject.get("points").getAsInt();
            overallPosition = jsonObject.get("overall").getAsInt();

            if (jsonObject.has("discord_id"))
                discordId = jsonObject.get("discord_id").getAsString();
        } else {
            status = Status.NOT_EXISTING;
            return;
        }

        displayedRegion = getRegionText();
        regionTooltip = getRegionTooltip();
        displayedOverall = getOverallText();
        overallTooltip = getOverallTooltip();

        parseRankings(jsonObject.getAsJsonObject("rankings"));

        status = Status.READY;
        originalJson = json;

        if (onUpdate != null)
            onUpdate.run();
    }

    private void parseRankings(JsonObject jsonObject) {
        for (GameMode gameMode : gameModes) {
            if (jsonObject.has(gameMode.parsingName))
                gameMode.parseTiers(jsonObject.getAsJsonObject(gameMode.parsingName));
            else
                gameMode.status = Status.NOT_EXISTING;
        }
        highest = getHighestMode();
    }

    public GameMode getGameMode(Mode gamemode) {
        for (GameMode gameMode : gameModes)
            if (gameMode.gamemode.toString().equalsIgnoreCase(gamemode.toString()))
                return gameMode;

        status = Status.NOT_EXISTING;
        return null;
    }

    private GameMode getHighestMode() {
        GameMode highest = null;
        int highestPoints = 0;
        for (GameMode gameMode : gameModes) {
            if (gameMode.status == Status.READY && gameMode.getTierPoints(false) > highestPoints) {
                highest = gameMode;
                highestPoints = gameMode.getTierPoints(false);
            }
        }
        return highest;
    }

    private Text getRegionText() {
        if (region.equalsIgnoreCase("EU"))
            return Icons.colorText(region, "eu");
        else if (region.equalsIgnoreCase("NA"))
            return Icons.colorText(region, "na");
        else if (region.equalsIgnoreCase("AS"))
            return Icons.colorText(region, "as");
        else if (region.equalsIgnoreCase("AU"))
            return Icons.colorText(region, "au");
        else if (region.equalsIgnoreCase("SA"))
            return Icons.colorText(region, "sa");
        else if (region.equalsIgnoreCase("ME"))
            return Icons.colorText(region, "me");
        else if (region.equalsIgnoreCase("AF"))
            return Icons.colorText(region, "af");
        else if (region.equalsIgnoreCase("OC"))
            return Icons.colorText(region, "oc");
        return Icons.colorText("Unknown", "unknown");
    }

    private Text getRegionTooltip() {
        if (region.equalsIgnoreCase("EU"))
            return Icons.colorText("Europe", "eu");
        else if (region.equalsIgnoreCase("NA"))
            return Icons.colorText("North America", "na");
        else if (region.equalsIgnoreCase("AS"))
            return Icons.colorText("Asia", "as");
        else if (region.equalsIgnoreCase("AU"))
            return Icons.colorText("Australia", "au");
        else if (region.equalsIgnoreCase("SA"))
            return Icons.colorText("South America", "sa");
        else if (region.equalsIgnoreCase("ME"))
            return Icons.colorText("Middle East", "me");
        else if (region.equalsIgnoreCase("AF"))
            return Icons.colorText("Africa", "af");
        else if (region.equalsIgnoreCase("OC"))
            return Icons.colorText("Oceania", "oc");
        return Icons.colorText("Unknown", "unknown");
    }

    private Text getOverallText() {
        String positionString = "#" + overallPosition;
        if (!(this instanceof PvPTiersProfile) && points >= 250) return Icons.colorText(positionString, "master");
        else if (this instanceof PvPTiersProfile && points >= 200) return Icons.colorText(positionString, "master");
        else if (points >= 100) return Icons.colorText(positionString, "ace");
        else if (points >= 50) return Icons.colorText(positionString, "specialist");
        else if (points >= 20) return Icons.colorText(positionString, "cadet");
        else if (points >= 10) return Icons.colorText(positionString, "novice");

        return Icons.colorText(positionString, "rookie");
    }

    private Text getOverallTooltip() {
        String overallTooltip = "Combat ";

        if (!(this instanceof PvPTiersProfile) && points >= 400) overallTooltip += "Grandmaster";
        else if (!(this instanceof PvPTiersProfile) && points >= 250) overallTooltip += "Master";
        else if (this instanceof PvPTiersProfile && points >= 200) overallTooltip += "Master";
        else if (points >= 100) overallTooltip += "Ace";
        else if (points >= 50) overallTooltip += "Specialist";
        else if (points >= 20) overallTooltip += "Cadet";
        else if (points >= 10) overallTooltip += "Novice";
        else overallTooltip = "Rookie";
        overallTooltip += "\n\nPoints: " + points;

        return Text.literal(overallTooltip).setStyle(displayedOverall.getStyle());
    }

    private void failedRequest() {
        String message = null;
        synchronized (SuperProfile.class) {
            if (this instanceof PvPTiersProfile) {
                failedPvPTiersRequestsLastMinute.incrementAndGet();
                if (failedPvPTiersRequests.incrementAndGet() % 20 == 0)
                    message = "[Tiers] PvPTiers might be down. " + failedPvPTiersRequests + " searches (out of " + PvPTiersRequests + ") failed so far. Use '/tiers -status' for more info";
            }
        }

        failedSuperProfiles.add(this);

        if (message != null) {
            TiersClient.LOGGER.info(message);
            if (numberOfMessages < 5) {
                Text textMessage = Icons.colorText(message, Colors.YELLOW);

                sendMessageToPlayer(textMessage, false);
                numberOfMessages++;
            }
        }
    }

    public String[] checkOutages() {
        String[] message = new String[3];

        if (this instanceof PvPTiersProfile) {
            message[0] = "PvPTiers might be down";
            message[1] = failedPvPTiersRequests + " searches (out of " + PvPTiersRequests + " profiles) failed so far";
        }

        message[2] = "Tiers will automatically try to update all failed requests";
        return message;
    }

    public static void resetRequestCounters() {
        synchronized (SuperProfile.class) {
            PvPTiersRequests.set(0);
            failedPvPTiersRequests.set(0);
        }
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public String toString() {
        StringBuilder gameModesDetails = new StringBuilder();
        gameModes.forEach(gameModesDetails::append);

        return "\nSuperProfile{" +
                "\nstatus=" + status +
                "\nnumberOfRequests=" + numberOfRequests +
                "\nregion=" + (region != null ? region : "null") +
                "\npoints=" + points +
                "\noverallPosition=" + overallPosition +
                "\ndisplayedRegion=" + (displayedRegion != null ? displayedRegion.getString() : "null") +
                "\ndisplayedOverall=" + (displayedOverall != null ? displayedOverall.getString() : "null") +
                "\noverallTooltip=" + (overallTooltip != null ? overallTooltip.getString() : "null") +
                "\nregionTooltip=" + (regionTooltip != null ? regionTooltip.getString() : "null") +
                "\ndrawn=" + drawn +
                "\napiErrorShown=" + apiErrorShown +
                "\n\ngameModes=" + gameModesDetails +
                "\n\nhighest=" + (highest != null ? highest : "null") +
                "\n\noriginalJson=" + (originalJson != null ? originalJson : "null") +
                "\n\n}";
    }
}