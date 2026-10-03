package cl.banco.transactions.web;

import cl.banco.common.CuentasResumen;
import cl.banco.transactions.catalog.TransaccionStore;
import cl.banco.transactions.client.AccountsClient;
import cl.banco.transactions.domain.Transaccion;
import cl.banco.transactions.events.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionesController {

    private final TransaccionStore store;
    private final AccountsClient accountsClient;
    private final EventPublisher publisher;

    public TransaccionesController(TransaccionStore store, AccountsClient accountsClient, EventPublisher publisher) {
        this.store = store;
        this.accountsClient = accountsClient;
        this.publisher = publisher;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_transactions.read')")
    public List<Transaccion> listar(@RequestParam(defaultValue = "false") boolean soloValidas) {
        return store.listar(soloValidas);
    }

    @GetMapping("/informe")
    @PreAuthorize("hasAuthority('SCOPE_transactions.read')")
    public InformeTransacciones informe() {
        CuentasResumen cuentas;
        try {
            cuentas = accountsClient.resumen();
        } catch (RuntimeException ex) {
            cuentas = CuentasResumen.noDisponible();
        }
        if (cuentas == null || !cuentas.disponible()) {
            return new InformeTransacciones(
                    store.total(),
                    store.validasCount(),
                    store.montoValidas(),
                    false,
                    CuentasResumen.noDisponible(),
                    "Informe parcial: el servicio de cuentas no respondió. Resilience4j entregó el fallback."
            );
        }
        return new InformeTransacciones(
                store.total(),
                store.validasCount(),
                store.montoValidas(),
                true,
                cuentas,
                "Informe completo"
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_transactions.read')")
    public Transaccion una(@PathVariable long id) {
        return store.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_transactions.write')")
    public ResponseEntity<TransaccionCreadaResponse> crear(@RequestBody CrearTransaccionRequest request) {
        if (request == null || request.fecha() == null || request.monto() == null || request.tipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha, monto y tipo son obligatorios");
        }
        Transaccion creada = store.crear(request.fecha(), request.monto(), request.tipo());
        boolean publicado = publisher.publicarCreada(creada);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransaccionCreadaResponse(creada, publicado));
    }
}
