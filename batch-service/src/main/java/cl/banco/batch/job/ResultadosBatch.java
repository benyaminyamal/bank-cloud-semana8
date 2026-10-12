package cl.banco.batch.job;

import cl.banco.batch.reporte.EstadoAnual;
import cl.banco.batch.reporte.InteresCalculado;
import cl.banco.batch.reporte.TransaccionClasificada;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResultadosBatch {

    private int txLeidas;
    private int txValidas;
    private int txAnomalas;
    private BigDecimal montoValidas = BigDecimal.ZERO;
    private final List<TransaccionClasificada> muestraAnomalias = new ArrayList<>();

    private int interesesAplicados;
    private int interesesOmitidos;
    private BigDecimal interesAhorro = BigDecimal.ZERO;
    private BigDecimal interesPrestamo = BigDecimal.ZERO;

    private int movimientosValidos;
    private int movimientosOmitidos;
    private final Map<String, Acumulado> estados = new ConcurrentHashMap<>();

    public synchronized void reiniciarDiario() {
        txLeidas = 0;
        txValidas = 0;
        txAnomalas = 0;
        montoValidas = BigDecimal.ZERO;
        muestraAnomalias.clear();
    }

    public synchronized void acumularDiario(TransaccionClasificada item) {
        txLeidas++;
        if (item.valida()) {
            txValidas++;
            montoValidas = montoValidas.add(item.monto());
        } else {
            txAnomalas++;
            if (muestraAnomalias.size() < 20) {
                muestraAnomalias.add(item);
            }
        }
    }

    public synchronized Map<String, Object> reporteDiario() {
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("leidas", txLeidas);
        reporte.put("validas", txValidas);
        reporte.put("anomalias", txAnomalas);
        reporte.put("montoValidas", montoValidas);
        reporte.put("muestraAnomalias", List.copyOf(muestraAnomalias));
        return reporte;
    }

    public synchronized void reiniciarIntereses() {
        interesesAplicados = 0;
        interesesOmitidos = 0;
        interesAhorro = BigDecimal.ZERO;
        interesPrestamo = BigDecimal.ZERO;
    }

    public synchronized void acumularInteres(InteresCalculado item) {
        if (!item.aplicado()) {
            interesesOmitidos++;
            return;
        }
        interesesAplicados++;
        if ("ahorro".equals(item.tipo())) {
            interesAhorro = interesAhorro.add(item.interes());
        } else if ("prestamo".equals(item.tipo())) {
            interesPrestamo = interesPrestamo.add(item.interes());
        }
    }

    public synchronized Map<String, Object> reporteIntereses() {
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("aplicados", interesesAplicados);
        reporte.put("omitidos", interesesOmitidos);
        reporte.put("interesAhorro", interesAhorro);
        reporte.put("interesPrestamo", interesPrestamo);
        reporte.put("tasaAhorroMensual", "0.5%");
        reporte.put("tasaPrestamoMensual", "1.2%");
        return reporte;
    }

    public synchronized void reiniciarAnual() {
        movimientosValidos = 0;
        movimientosOmitidos = 0;
        estados.clear();
    }

    public synchronized void acumularAnual(String cuentaId, String tipo, BigDecimal monto, boolean valida) {
        if (!valida || cuentaId == null || cuentaId.isBlank()) {
            movimientosOmitidos++;
            return;
        }
        movimientosValidos++;
        estados.compute(cuentaId, (id, actual) -> {
            Acumulado acumulado = actual == null ? new Acumulado() : actual;
            acumulado.movimientos++;
            if ("deposito".equals(tipo)) {
                acumulado.depositos = acumulado.depositos.add(monto);
            } else if ("retiro".equals(tipo)) {
                acumulado.retiros = acumulado.retiros.add(monto);
            }
            return acumulado;
        });
    }

    public synchronized Map<String, Object> reporteAnual() {
        List<EstadoAnual> cuentas = estados.entrySet().stream()
                .map(entry -> new EstadoAnual(
                        entry.getKey(),
                        entry.getValue().movimientos,
                        0,
                        entry.getValue().depositos,
                        entry.getValue().retiros))
                .sorted(Comparator.comparing(EstadoAnual::cuentaId))
                .toList();
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("cuentas", cuentas.size());
        reporte.put("movimientosValidos", movimientosValidos);
        reporte.put("movimientosOmitidos", movimientosOmitidos);
        reporte.put("muestra", cuentas.stream().limit(20).toList());
        return reporte;
    }

    private static final class Acumulado {
        private int movimientos;
        private BigDecimal depositos = BigDecimal.ZERO;
        private BigDecimal retiros = BigDecimal.ZERO;
    }
}
