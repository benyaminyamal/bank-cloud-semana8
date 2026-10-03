package cl.banco.accounts.web;

import cl.banco.accounts.catalog.LegacyCatalog;
import cl.banco.accounts.domain.Cuenta;
import cl.banco.accounts.domain.MovimientoAnual;
import cl.banco.common.CuentasResumen;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasAuthority('SCOPE_accounts.read')")
public class CuentasController {

    private final LegacyCatalog catalog;

    public CuentasController(LegacyCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<Cuenta> listar(@RequestParam(defaultValue = "false") boolean soloValidas) {
        return catalog.cuentas(soloValidas);
    }

    @GetMapping("/resumen")
    public CuentasResumen resumen() {
        return catalog.resumen();
    }

    @GetMapping("/{id}")
    public Cuenta una(@PathVariable String id) {
        return catalog.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    @GetMapping("/{id}/anual")
    public List<MovimientoAnual> anual(@PathVariable String id) {
        catalog.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        return catalog.anuales(id);
    }
}
