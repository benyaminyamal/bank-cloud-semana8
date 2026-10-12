package cl.banco.payments.domain;

import java.math.BigDecimal;

public record Pago(
        long id,
        String tipo,
        String cuentaOrigen,
        String cuentaDestino,
        BigDecimal monto,
        String estado
) {
}
