package cl.banco.batch.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

public final class InteresCalculador {

    private static final Set<String> AHORRO = Set.of("ahorro");
    private static final Set<String> PRESTAMO = Set.of("prestamo");

    private InteresCalculador() {
    }

    public static InteresCalculado calcular(String cuentaId, String tipo, BigDecimal saldo) {
        String tipoLimpio = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if (saldo == null || saldo.compareTo(BigDecimal.ZERO) < 0) {
            return new InteresCalculado(cuentaId, tipoLimpio, saldo, BigDecimal.ZERO, false, "saldo no válido");
        }
        BigDecimal tasa;
        if (AHORRO.contains(tipoLimpio)) {
            tasa = new BigDecimal("0.005");
        } else if (PRESTAMO.contains(tipoLimpio)) {
            tasa = new BigDecimal("0.012");
        } else {
            return new InteresCalculado(cuentaId, tipoLimpio, saldo, BigDecimal.ZERO, false, "tipo fuera de ahorro y préstamo");
        }
        BigDecimal interes = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        return new InteresCalculado(cuentaId, tipoLimpio, saldo, interes, true, "ok");
    }
}
