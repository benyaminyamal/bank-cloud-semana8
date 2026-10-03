package cl.banco.accounts.csv;

import cl.banco.accounts.domain.Cuenta;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CuentaParser {

    private static final Set<String> TIPOS_VALIDOS = Set.of("ahorro", "prestamo", "hipoteca");

    private CuentaParser() {
    }

    public static List<Cuenta> parse(List<Map<String, String>> rows) {
        Set<String> vistos = new HashSet<>();
        List<Cuenta> cuentas = new ArrayList<>();
        for (Map<String, String> row : rows) {
            List<String> observaciones = new ArrayList<>();
            String cuentaId = LegacyTexts.clean(row.get("cuenta_id"));
            String nombre = LegacyTexts.clean(row.get("nombre"));
            String tipo = LegacyTexts.clean(row.get("tipo")).toLowerCase(Locale.ROOT);
            Optional<BigDecimal> saldo = LegacyNumbers.money(row.get("saldo"));
            Optional<Integer> edad = LegacyNumbers.integer(row.get("edad"));
            boolean valida = true;

            if (cuentaId.isEmpty()) {
                observaciones.add("cuenta_id vacío");
                valida = false;
            } else if (!vistos.add(cuentaId)) {
                observaciones.add("cuenta duplicada");
                valida = false;
            }
            if (nombre.isEmpty() || "unknown".equalsIgnoreCase(nombre)) {
                observaciones.add("nombre no válido");
                valida = false;
            }
            if (saldo.isEmpty()) {
                observaciones.add("saldo vacío");
                valida = false;
            } else if (saldo.get().compareTo(BigDecimal.ZERO) < 0) {
                observaciones.add("saldo negativo");
                valida = false;
            }
            if (edad.isEmpty()) {
                observaciones.add("edad vacía");
                valida = false;
            } else if (edad.get() < 0 || edad.get() > 120) {
                observaciones.add("edad fuera de rango");
                valida = false;
            }
            if (!TIPOS_VALIDOS.contains(tipo)) {
                observaciones.add("tipo no válido");
                valida = false;
            }

            cuentas.add(new Cuenta(
                    cuentaId,
                    nombre,
                    saldo.orElse(null),
                    edad.orElse(null),
                    tipo,
                    valida,
                    List.copyOf(observaciones)
            ));
        }
        return List.copyOf(cuentas);
    }
}
