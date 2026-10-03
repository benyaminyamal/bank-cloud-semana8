package cl.banco.accounts.domain;

import java.math.BigDecimal;
import java.util.List;

public record Cuenta(
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        Integer edad,
        String tipo,
        boolean valida,
        List<String> observaciones
) {
}
