package cl.banco.batch.reporte;

import java.math.BigDecimal;

public record EstadoAnual(
        String cuentaId,
        int movimientos,
        int omitidos,
        BigDecimal totalDepositos,
        BigDecimal totalRetiros
) {
}
