package cl.banco.batch.reporte;

import java.math.BigDecimal;

public record InteresCalculado(
        String cuentaId,
        String tipo,
        BigDecimal saldo,
        BigDecimal interes,
        boolean aplicado,
        String detalle
) {
}
