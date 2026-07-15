package dev.raidmine.stafftool.chat;

public final class UiNotificationCenter {
    private static volatile Notice topNotice;
    private static volatile Notice mentionNotice;

    private UiNotificationCenter() {
    }

    public static void violation(String author, String word) {
        String source = author == null || author.isBlank() ? "Чат" : author;
        topNotice = new Notice(Kind.VIOLATION, "Нарушение в чате", source + " • «" + word + "»",
                System.currentTimeMillis(), 5200L);
    }

    public static void info(String title, String message) {
        topNotice = new Notice(Kind.INFO, title, message, System.currentTimeMillis(), 3600L);
    }

    public static void mention(String author) {
        String from = author == null || author.isBlank() ? "Игрок" : author;
        mentionNotice = new Notice(Kind.MENTION, "Вас упомянули в чате", "Сообщение от " + from,
                System.currentTimeMillis(), 4200L);
    }

    public static Notice top() {
        return active(topNotice) ? topNotice : null;
    }

    public static Notice mention() {
        return active(mentionNotice) ? mentionNotice : null;
    }

    public static float progress(Notice notice) {
        if (notice == null) return 0F;
        long age = System.currentTimeMillis() - notice.createdAt();
        float in = Math.min(1F, age / 260F);
        long remaining = notice.duration() - age;
        float out = remaining >= 450L ? 1F : Math.max(0F, remaining / 450F);
        return easeOutCubic(in) * out;
    }

    private static boolean active(Notice notice) {
        return notice != null && System.currentTimeMillis() - notice.createdAt() < notice.duration();
    }

    private static float easeOutCubic(float t) {
        float p = 1F - Math.max(0F, Math.min(1F, t));
        return 1F - p * p * p;
    }

    public enum Kind { VIOLATION, MENTION, INFO }

    public record Notice(Kind kind, String title, String message, long createdAt, long duration) {
    }
}
