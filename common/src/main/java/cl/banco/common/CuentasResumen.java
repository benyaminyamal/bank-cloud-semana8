package cl.banco.common;

import java.math.BigDecimal;

public record CuentasResumen(
        boolean disponible,
        int totalRegistros,
        int cuentasValidas,
        BigDecimal saldoTotal,
        String mensaje
) {

    public static CuentasResumen noDisponible() {
        return new CuentasResumen(
                false,
                0,
                0,
                BigDecimal.ZERO,
                "El servicio de cuentas no está disponible."
        );
    }
}
