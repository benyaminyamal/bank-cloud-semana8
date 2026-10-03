package cl.banco.transactions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Transaccion(
        Long id,
        String fechaOriginal,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean valida,
        List<String> observaciones
) {
}
