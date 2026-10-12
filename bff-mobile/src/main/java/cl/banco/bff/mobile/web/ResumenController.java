package cl.banco.bff.mobile.web;

import cl.banco.bff.mobile.client.CuentasClient;
import cl.banco.common.CuentasResumen;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resumen")
@PreAuthorize("hasAuthority('SCOPE_bff.mobile')")
public class ResumenController {

    private final CuentasClient cuentas;

    public ResumenController(CuentasClient cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    public Map<String, Object> resumen() {
        CuentasResumen resumen = cuentas.resumen();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("canal", "mobile");
        respuesta.put("disponible", resumen.disponible());
        respuesta.put("cuentasValidas", resumen.cuentasValidas());
        respuesta.put("saldoTotal", resumen.saldoTotal());
        return respuesta;
    }
}
