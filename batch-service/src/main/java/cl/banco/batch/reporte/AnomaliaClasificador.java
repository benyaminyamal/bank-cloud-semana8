package cl.banco.batch.reporte;

import cl.banco.common.LegacyDates;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class AnomaliaClasificador {

    private static final Set<String> TIPOS = Set.of("credito", "debito");

    private AnomaliaClasificador() {
    }

    public static TransaccionClasificada clasificar(
            String id, String fecha, String monto, String tipo, Set<Long> vistos) {
        List<String> anomalias = new ArrayList<>();
        Optional<Long> idOk = LegacyNumbers.longValue(id);
        boolean duplicada = idOk.isPresent() && !vistos.add(idOk.get());
        String fechaOriginal = LegacyTexts.clean(fecha);
        var fechaOk = LegacyDates.parse(fechaOriginal);
        var montoOk = LegacyNumbers.money(monto);
        String tipoLimpio = LegacyTexts.clean(tipo).toLowerCase(Locale.ROOT);

        if (idOk.isEmpty()) {
            anomalias.add("id no válido");
        }
        if (duplicada) {
            anomalias.add("transacción duplicada");
        }
        if (fechaOk.isEmpty()) {
            anomalias.add("fecha no válida");
        }
        if (montoOk.isEmpty()) {
            anomalias.add("monto vacío");
        } else if (montoOk.get().compareTo(BigDecimal.ZERO) == 0) {
            anomalias.add("monto cero");
        } else if (montoOk.get().compareTo(BigDecimal.ZERO) < 0) {
            anomalias.add("monto negativo");
        }
        if (!TIPOS.contains(tipoLimpio)) {
            anomalias.add("tipo no válido");
        }
        return new TransaccionClasificada(
                idOk.map(String::valueOf).orElse(LegacyTexts.clean(id)),
                fechaOk.map(Object::toString).orElse(fechaOriginal),
                montoOk.orElse(BigDecimal.ZERO),
                tipoLimpio,
                anomalias.isEmpty(),
                List.copyOf(anomalias));
    }
}
