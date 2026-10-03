package cl.banco.accounts.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovimientoAnual(
        String cuentaId,
        String fechaOriginal,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion,
        boolean valida,
        List<String> observaciones
) {
}
