package cl.banco.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class LegacyDates {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private LegacyDates() {
    }

    public static Optional<LocalDate> parse(String raw) {
        String value = LegacyTexts.clean(raw);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // El CSV legacy mezcla varios formatos en la misma columna.
            }
        }
        return Optional.empty();
    }
}
