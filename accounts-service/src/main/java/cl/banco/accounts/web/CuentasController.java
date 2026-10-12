package cl.banco.accounts.web;

import cl.banco.accounts.catalog.LegacyCatalog;
import cl.banco.accounts.domain.Cuenta;
import cl.banco.accounts.domain.MovimientoAnual;
import cl.banco.common.CuentasResumen;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentasController {

    private final LegacyCatalog catalog;

    public CuentasController(LegacyCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_accounts.read')")
    public List<Cuenta> listar(@RequestParam(defaultValue = "false") boolean soloValidas) {
        return catalog.cuentas(soloValidas);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('SCOPE_accounts.read')")
    public CuentasResumen resumen() {
        return catalog.resumen();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_accounts.write')")
    public Cuenta abrir(@RequestBody AbrirCuentaRequest request) {
        return catalog.abrir(request.nombre(), request.saldo(), request.edad(), request.tipo());
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('SCOPE_accounts.write')")
    public Cuenta cerrar(@PathVariable String id) {
        try {
            return catalog.cerrar(id);
        } catch (java.util.NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_accounts.write')")
    public Cuenta mantener(@PathVariable String id, @RequestBody AbrirCuentaRequest request) {
        try {
            return catalog.mantener(id, request.nombre(), request.tipo());
        } catch (java.util.NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_accounts.read')")
    public Cuenta una(@PathVariable String id) {
        return catalog.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    @GetMapping("/{id}/anual")
    @PreAuthorize("hasAuthority('SCOPE_accounts.read')")
    public List<MovimientoAnual> anual(@PathVariable String id) {
        catalog.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        return catalog.anuales(id);
    }
}
