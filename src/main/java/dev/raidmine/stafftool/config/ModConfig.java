package dev.raidmine.stafftool.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.raidmine.stafftool.RaidMineStaffMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rm-tools.json");
    private static final Path LEGACY_PATH = FabricLoader.getInstance().getConfigDir().resolve("raidmine-staff.json");

    public boolean hudEnabled = true;
    public int hudYOffset = 8;
    public float hudX = 0.5F;
    public float hudY = 0.015F;
    public float hudScale = 0.82F;
    public boolean requireConfirmation = true;
    public boolean restrictToRaidMine = true;
    public List<String> allowedAddressFragments = new ArrayList<>(List.of("raidmine"));

    public int accentColor = 0xFFFFA31A;
    public int accentColorSecondary = 0xFFFF5A00;
    public boolean autoScreenshot = true;
    public boolean mentionNotifications = true;
    public boolean forbiddenWordAlerts = true;
    public String punishmentReasonTemplate = "{rule}";
    public List<String> forbiddenWords = new ArrayList<>();

    public String warnCommand = "warn {player} {reason}";
    public String muteCommand = "mute {player} {duration} {reason}";
    public String banCommand = "ban {player} {duration} {reason}";
    public String permanentBanCommand = "ban {player} permanent {reason}";
    public String kickCommand = "kick {player} {reason}";

    public static ModConfig load() {
        Path source = Files.exists(PATH) ? PATH : LEGACY_PATH;
        if (!Files.exists(source)) {
            ModConfig created = new ModConfig();
            created.save();
            return created;
        }

        try (Reader reader = Files.newBufferedReader(source)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded == null) {
                loaded = new ModConfig();
            }
            loaded.normalize();
            if (!source.equals(PATH)) {
                loaded.save();
            }
            return loaded;
        } catch (Exception exception) {
            RaidMineStaffMod.LOGGER.error("Could not read {}. Defaults will be used.", source, exception);
            return new ModConfig();
        }
    }

    public void save() {
        normalize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            RaidMineStaffMod.LOGGER.error("Could not save {}", PATH, exception);
        }
    }

    public String formatReason(String ruleCode) {
        String rule = ruleCode == null || ruleCode.isBlank() ? "RULE" : ruleCode.trim();
        String template = punishmentReasonTemplate == null || punishmentReasonTemplate.isBlank()
                ? "{rule}" : punishmentReasonTemplate.trim();
        String formatted = template.replace("{rule}", rule).trim();
        return formatted.isBlank() ? rule : formatted;
    }

    public void setAccent(int primary, int secondary) {
        accentColor = 0xFF000000 | (primary & 0x00FFFFFF);
        accentColorSecondary = 0xFF000000 | (secondary & 0x00FFFFFF);
        save();
    }

    private void normalize() {
        hudYOffset = Math.max(0, Math.min(120, hudYOffset));
        hudX = clamp(hudX, 0F, 1F, 0.5F);
        hudY = clamp(hudY, 0F, 1F, 0.015F);
        hudScale = clamp(hudScale, 0.60F, 1.45F, 0.82F);
        accentColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
        accentColorSecondary = 0xFF000000 | (accentColorSecondary & 0x00FFFFFF);
        punishmentReasonTemplate = fallback(punishmentReasonTemplate, "{rule}");

        if (allowedAddressFragments == null || allowedAddressFragments.isEmpty()) {
            allowedAddressFragments = new ArrayList<>(List.of("raidmine"));
        }
        if (forbiddenWords == null) {
            forbiddenWords = new ArrayList<>();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String word : forbiddenWords) {
            if (word != null && !word.isBlank()) {
                unique.add(word.trim().toLowerCase(Locale.ROOT));
            }
        }
        forbiddenWords = new ArrayList<>(unique);

        warnCommand = fallback(warnCommand, "warn {player} {reason}");
        muteCommand = fallback(muteCommand, "mute {player} {duration} {reason}");
        banCommand = fallback(banCommand, "ban {player} {duration} {reason}");
        permanentBanCommand = fallback(permanentBanCommand, "ban {player} permanent {reason}");
        kickCommand = fallback(kickCommand, "kick {player} {reason}");
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
