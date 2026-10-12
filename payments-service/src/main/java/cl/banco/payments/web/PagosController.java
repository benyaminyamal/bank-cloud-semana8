package cl.banco.payments.web;

import cl.banco.payments.domain.Pago;
import cl.banco.payments.events.PagoPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/pagos")
public class PagosController {

    private static final Set<String> TIPOS = Set.of("deposito", "retiro", "transferencia");
    private static final BigDecimal UMBRAL_ALERTA = new BigDecimal("50000");

    private final AtomicLong secuencia = new AtomicLong(1);
    private final List<Pago> pagos = new CopyOnWriteArrayList<>();
    private final PagoPublisher publisher;

    public PagosController(PagoPublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_payments.read')")
    public List<Pago> listar() {
        return pagos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_payments.write')")
    public Map<String, Object> crear(@RequestBody PagoRequest request) {
        String tipo = request.tipo() == null ? "" : request.tipo().trim().toLowerCase(Locale.ROOT);
        if (!TIPOS.contains(tipo) || request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0
                || request.cuentaOrigen() == null || request.cuentaOrigen().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pago no válido");
        }
        if ("transferencia".equals(tipo) && (request.cuentaDestino() == null || request.cuentaDestino().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La transferencia necesita cuenta destino");
        }
        Pago pago = new Pago(secuencia.getAndIncrement(), tipo, request.cuentaOrigen().trim(),
                request.cuentaDestino() == null ? "" : request.cuentaDestino().trim(), request.monto(), "COMPLETADO");
        pagos.add(pago);
        boolean publicado = publisher.publicado(pago);
        boolean alerta = false;
        if ("retiro".equals(tipo) && request.monto().compareTo(UMBRAL_ALERTA) >= 0) {
            alerta = publisher.alerta(pago);
        }
        return Map.of("pago", pago, "eventoPublicado", publicado, "alertaPublicada", alerta);
    }
}
