package cl.banco.notification.web;

import cl.banco.common.TransaccionEvento;
import cl.banco.notification.events.EventoInbox;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@PreAuthorize("hasAuthority('SCOPE_events.read')")
public class EventosController {

    private final EventoInbox inbox;

    public EventosController(EventoInbox inbox) {
        this.inbox = inbox;
    }

    @GetMapping
    public List<TransaccionEvento> recientes() {
        return inbox.recientes();
    }
}
