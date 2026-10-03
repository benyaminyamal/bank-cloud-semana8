package cl.banco.accounts.csv;

import cl.banco.accounts.domain.MovimientoAnual;
import cl.banco.common.LegacyDates;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MovimientoAnualParser {

    private MovimientoAnualParser() {
    }

    public static List<MovimientoAnual> parse(List<Map<String, String>> rows) {
        List<MovimientoAnual> movimientos = new ArrayList<>();
        for (Map<String, String> row : rows) {
            List<String> observaciones = new ArrayList<>();
            String cuentaId = LegacyTexts.clean(row.get("cuenta_id"));
            String fechaOriginal = LegacyTexts.clean(row.get("fecha"));
            Optional<LocalDate> fecha = LegacyDates.parse(fechaOriginal);
            String transaccion = LegacyTexts.clean(row.get("transaccion")).toLowerCase();
            Optional<BigDecimal> monto = LegacyNumbers.money(row.get("monto"));
            String descripcion = LegacyTexts.clean(row.get("descripcion"));
            boolean valida = true;

            if (cuentaId.isEmpty()) {
                observaciones.add("cuenta_id vacío");
                valida = false;
            }
            if (fecha.isEmpty()) {
                observaciones.add("fecha no válida");
                valida = false;
            }
            if (transaccion.isEmpty()) {
                observaciones.add("transacción vacía");
                valida = false;
            }
            if (monto.isEmpty()) {
                observaciones.add("monto vacío");
                valida = false;
            } else if (monto.get().compareTo(BigDecimal.ZERO) <= 0) {
                observaciones.add("monto no positivo");
                valida = false;
            }
            if (descripcion.isEmpty()) {
                observaciones.add("descripción vacía");
            }

            movimientos.add(new MovimientoAnual(
                    cuentaId,
                    fechaOriginal,
                    fecha.orElse(null),
                    transaccion,
                    monto.orElse(null),
                    descripcion,
                    valida,
                    List.copyOf(observaciones)
            ));
        }
        return List.copyOf(movimientos);
    }
}
