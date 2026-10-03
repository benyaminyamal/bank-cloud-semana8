package cl.banco.transactions.csv;

import cl.banco.common.LegacyDates;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;
import cl.banco.transactions.domain.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TransaccionParser {

    private static final Set<String> TIPOS_VALIDOS = Set.of("credito", "debito");

    private TransaccionParser() {
    }

    public static List<Transaccion> parse(List<Map<String, String>> rows) {
        Set<Long> vistos = new HashSet<>();
        List<Transaccion> transacciones = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Optional<Long> id = LegacyNumbers.longValue(row.get("id"));
            boolean duplicada = id.isPresent() && !vistos.add(id.get());
            transacciones.add(evaluar(
                    id.orElse(null),
                    LegacyTexts.clean(row.get("fecha")),
                    LegacyNumbers.money(row.get("monto")).orElse(null),
                    LegacyTexts.clean(row.get("tipo")),
                    duplicada,
                    id.isEmpty()
            ));
        }
        return transacciones;
    }

    public static Transaccion crear(long id, String fecha, BigDecimal monto, String tipo) {
        return evaluar(id, LegacyTexts.clean(fecha), monto, LegacyTexts.clean(tipo), false, false);
    }

    private static Transaccion evaluar(
            Long id,
            String fechaOriginal,
            BigDecimal monto,
            String tipoRaw,
            boolean duplicada,
            boolean idInvalido) {
        List<String> observaciones = new ArrayList<>();
        Optional<LocalDate> fecha = LegacyDates.parse(fechaOriginal);
        String tipo = tipoRaw.toLowerCase(Locale.ROOT);
        boolean valida = true;

        if (idInvalido || id == null) {
            observaciones.add("id no válido");
            valida = false;
        }
        if (duplicada) {
            observaciones.add("transacción duplicada");
            valida = false;
        }
        if (fecha.isEmpty()) {
            observaciones.add("fecha no válida");
            valida = false;
        }
        if (monto == null) {
            observaciones.add("monto vacío");
            valida = false;
        } else if (monto.compareTo(BigDecimal.ZERO) == 0) {
            observaciones.add("monto cero");
            valida = false;
        } else if (monto.compareTo(BigDecimal.ZERO) < 0) {
            observaciones.add("monto negativo");
            valida = false;
        }
        if (!TIPOS_VALIDOS.contains(tipo)) {
            observaciones.add("tipo no válido");
            valida = false;
        }

        return new Transaccion(
                id,
                fechaOriginal,
                fecha.orElse(null),
                monto,
                tipo,
                valida,
                List.copyOf(observaciones)
        );
    }
}
