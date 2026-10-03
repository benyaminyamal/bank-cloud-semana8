package cl.banco.notification.events;

import cl.banco.common.Topics;
import cl.banco.common.TransaccionEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class EventoInbox {

    private static final Logger log = LoggerFactory.getLogger(EventoInbox.class);
    private static final int MAXIMO = 100;

    private final Deque<TransaccionEvento> eventos = new ArrayDeque<>();

    @KafkaListener(topics = Topics.TRANSACCIONES, groupId = "notification-service")
    public void recibir(TransaccionEvento evento) {
        if (evento == null || evento.getEventoId() == null) {
            return;
        }
        synchronized (eventos) {
            eventos.addFirst(evento);
            while (eventos.size() > MAXIMO) {
                eventos.removeLast();
            }
        }
        log.info("Evento {} recibido: {}", evento.getTipoEvento(), evento.getDetalle());
    }

    public List<TransaccionEvento> recientes() {
        synchronized (eventos) {
            return List.copyOf(eventos);
        }
    }
}
