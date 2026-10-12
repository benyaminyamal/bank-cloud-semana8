package cl.banco.batch.reporte;

import java.math.BigDecimal;
import java.util.List;

public record TransaccionClasificada(
        String id,
        String fecha,
        BigDecimal monto,
        String tipo,
        boolean valida,
        List<String> anomalias
) {
}
