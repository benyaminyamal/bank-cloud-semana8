package cl.banco.bff.web.web;

import cl.banco.bff.web.client.ClientesClient;
import cl.banco.bff.web.client.CuentasClient;
import cl.banco.common.CuentasResumen;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/panel")
@PreAuthorize("hasAuthority('SCOPE_bff.web')")
public class PanelController {

    private final CuentasClient cuentas;
    private final ClientesClient clientes;

    public PanelController(CuentasClient cuentas, ClientesClient clientes) {
        this.cuentas = cuentas;
        this.clientes = clientes;
    }

    @GetMapping
    public Map<String, Object> panel() {
        CuentasResumen resumen = cuentas.resumen();
        var clientes = this.clientes.listar();
        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("canal", "web");
        panel.put("cuentas", resumen);
        panel.put("totalClientes", clientes.size());
        panel.put("clientes", clientes);
        return panel;
    }
}
