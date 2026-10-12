package cl.banco.bff.atm.web;

import cl.banco.bff.atm.client.CuentasClient;
import cl.banco.bff.atm.client.PagosClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('SCOPE_bff.atm')")
public class CajeroController {

    private final CuentasClient cuentas;
    private final PagosClient pagos;

    public CajeroController(CuentasClient cuentas, PagosClient pagos) {
        this.cuentas = cuentas;
        this.pagos = pagos;
    }

    @GetMapping("/saldo/{id}")
    public Map<String, Object> saldo(@PathVariable String id) {
        Map<String, Object> cuenta = cuentas.cuenta(id);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("canal", "cajero");
        respuesta.put("cuentaId", id);
        respuesta.put("saldo", cuenta.getOrDefault("saldo", BigDecimal.ZERO));
        respuesta.put("disponible", cuenta.getOrDefault("disponible", true));
        return respuesta;
    }

    @PostMapping("/retiros")
    public Map<String, Object> retirar(@RequestBody RetiroRequest request) {
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("tipo", "retiro");
        pago.put("cuentaOrigen", request.cuentaId());
        pago.put("monto", request.monto());
        Map<String, Object> resultado = new LinkedHashMap<>(pagos.crear(pago));
        resultado.put("canal", "cajero");
        return resultado;
    }

    public record RetiroRequest(String cuentaId, BigDecimal monto) {
    }
}
