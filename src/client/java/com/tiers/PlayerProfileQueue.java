package com.tiers;

import com.tiers.profile.PlayerProfile;
import com.tiers.profile.Status;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerProfileQueue {
    private static final ConcurrentLinkedDeque<PlayerProfile> queue = new ConcurrentLinkedDeque<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tiers-profile-queue");
        t.setDaemon(true);
        return t;
    });

    private static PlayerProfile currentProfile = null;

    static {
        scheduler.scheduleAtFixedRate(PlayerProfileQueue::processNext, 0, 50, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(PlayerProfileQueue::recoverFailedRequests, 0, 1, TimeUnit.MINUTES);
    }

    public static void enqueue(PlayerProfile playerProfile) {
        queue.add(playerProfile);
    }

    private synchronized static void processNext() {
        if (currentProfile != null && currentProfile.status == Status.SEARCHING)
            return;

        currentProfile = queue.poll();
        if (currentProfile != null)
            currentProfile.buildRequest();
    }

    public synchronized static void putFirstInQueue(PlayerProfile playerProfile) {
        if (currentProfile == playerProfile)
            return;

        queue.remove(playerProfile);
        queue.addFirst(playerProfile);
    }

    public synchronized static void putLastInQueue(PlayerProfile playerProfile) {
        if (currentProfile == playerProfile)
            return;

        queue.remove(playerProfile);
        queue.addLast(playerProfile);
    }

    public synchronized static void changeToFirstInQueue(PlayerProfile playerProfile) {
        if (currentProfile == playerProfile)
            return;

        if (queue.contains(playerProfile)) {
            queue.remove(playerProfile);
            queue.addFirst(playerProfile);
        }
    }

    public synchronized static void clearQueue() {
        queue.clear();
        currentProfile = null;
    }

    public synchronized static void removeFromQueue(PlayerProfile playerProfile) {
        if (currentProfile == playerProfile)
            currentProfile = null;

        queue.remove(playerProfile);
    }

    private static void recoverFailedRequests() {
        for (PlayerProfile playerProfile : PlayerProfile.failedPlayerProfiles) {
            PlayerProfile.failedPlayerProfiles.remove(playerProfile);
            playerProfile.prepareToRebuild();
            putLastInQueue(playerProfile);
        }
    }
}