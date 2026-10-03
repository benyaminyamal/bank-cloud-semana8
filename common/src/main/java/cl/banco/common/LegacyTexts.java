package cl.banco.common;

public final class LegacyTexts {

    private LegacyTexts() {
    }

    public static String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static boolean blank(String raw) {
        return clean(raw).isEmpty();
    }
}
