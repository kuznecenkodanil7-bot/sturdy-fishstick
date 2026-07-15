package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

public final class ServerGuard {
    private ServerGuard() {
    }

    public static boolean isAllowed(MinecraftClient client) {
        if (!RaidMineStaffMod.config().restrictToRaidMine) {
            return true;
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null || server.address == null) {
            return false;
        }
        String address = server.address.toLowerCase(Locale.ROOT);
        return RaidMineStaffMod.config().allowedAddressFragments.stream()
                .filter(fragment -> fragment != null && !fragment.isBlank())
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .anyMatch(address::contains);
    }

    public static String currentAddress(MinecraftClient client) {
        ServerInfo server = client.getCurrentServerEntry();
        return server == null || server.address == null ? "одиночная игра" : server.address;
    }
}
