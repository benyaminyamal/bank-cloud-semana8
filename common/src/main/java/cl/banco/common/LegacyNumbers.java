package cl.banco.common;

import java.math.BigDecimal;
import java.util.Optional;

public final class LegacyNumbers {

    private LegacyNumbers() {
    }

    public static Optional<BigDecimal> money(String raw) {
        String value = LegacyTexts.clean(raw);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> integer(String raw) {
        String value = LegacyTexts.clean(raw);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> longValue(String raw) {
        String value = LegacyTexts.clean(raw);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
