package cl.banco.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatesTest {

    @Test
    void aceptaLosFormatosDelCsvLegacy() {
        assertEquals(LocalDate.of(2024, 6, 30), LegacyDates.parse("2024-06-30").orElseThrow());
        assertEquals(LocalDate.of(2024, 3, 18), LegacyDates.parse("2024/03/18").orElseThrow());
        assertEquals(LocalDate.of(2024, 4, 3), LegacyDates.parse("03-04-2024").orElseThrow());
        assertEquals(LocalDate.of(2024, 5, 4), LegacyDates.parse(" 04/05/2024 ").orElseThrow());
    }

    @Test
    void rechazaFechasVaciasOIrreconocibles() {
        assertEquals(Optional.empty(), LegacyDates.parse(" "));
        assertEquals(Optional.empty(), LegacyDates.parse("ayer"));
        assertTrue(LegacyNumbers.money("-200").isPresent());
        assertTrue(LegacyNumbers.money("").isEmpty());
    }
}
