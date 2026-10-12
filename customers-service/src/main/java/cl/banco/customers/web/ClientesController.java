package cl.banco.customers.web;

import cl.banco.customers.catalog.ClientesCatalog;
import cl.banco.customers.domain.Cliente;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasAuthority('SCOPE_customers.read')")
public class ClientesController {

    private final ClientesCatalog catalog;

    public ClientesController(ClientesCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<Cliente> listar() {
        return catalog.clientes();
    }

    @GetMapping("/{id}")
    public Cliente uno(@PathVariable String id) {
        return catalog.porId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }
}
