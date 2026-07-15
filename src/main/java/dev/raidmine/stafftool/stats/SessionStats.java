package dev.raidmine.stafftool.stats;

import dev.raidmine.stafftool.rules.PunishmentType;
import net.minecraft.client.MinecraftClient;

public final class SessionStats {
    private long startedAtMillis = System.currentTimeMillis();
    private Object lastNetworkHandler;
    private int bans;
    private int mutes;
    private int kicks;
    private int warns;
    private long lastActionMillis;
    private PunishmentType lastType;

    public void observeConnection(MinecraftClient client) {
        Object current = client.getNetworkHandler();
        if (current != null && current != lastNetworkHandler) {
            reset();
            lastNetworkHandler = current;
        }
        if (current == null) {
            lastNetworkHandler = null;
        }
    }

    public void record(PunishmentType type) {
        switch (type) {
            case BAN, PERMANENT_BAN -> bans++;
            case MUTE -> mutes++;
            case KICK -> kicks++;
            case WARN -> warns++;
        }
        lastType = type;
        lastActionMillis = System.currentTimeMillis();
    }

    public void reset() {
        bans = 0;
        mutes = 0;
        kicks = 0;
        warns = 0;
        startedAtMillis = System.currentTimeMillis();
        lastActionMillis = 0L;
        lastType = null;
    }

    public int bans() {
        return bans;
    }

    public int mutes() {
        return mutes;
    }

    public int kicks() {
        return kicks;
    }

    public int warns() {
        return warns;
    }

    public long elapsedSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - startedAtMillis) / 1000L);
    }

    public float pulse(PunishmentType type) {
        if (type != lastType || lastActionMillis == 0L) {
            return 0F;
        }
        long age = System.currentTimeMillis() - lastActionMillis;
        if (age >= 700L) {
            return 0F;
        }
        float t = age / 700F;
        return (1F - t) * (1F - t);
    }
}
