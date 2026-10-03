package cl.banco.transactions.events;

import cl.banco.common.Topics;
import cl.banco.common.TransaccionEvento;
import cl.banco.transactions.domain.Transaccion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, TransaccionEvento> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, TransaccionEvento> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean publicarCreada(Transaccion transaccion) {
        TransaccionEvento evento = base("TRANSACCION_CREADA");
        evento.setTransaccionId(transaccion.id());
        evento.setTipoMovimiento(transaccion.tipo());
        evento.setMonto(transaccion.monto());
        evento.setFecha(transaccion.fecha() == null ? transaccion.fechaOriginal() : transaccion.fecha().toString());
        evento.setDetalle("Transacción registrada");
        return send(String.valueOf(transaccion.id()), evento);
    }

    public boolean publicarImportacion(int total, BigDecimal monto) {
        TransaccionEvento evento = base("IMPORTACION");
        evento.setMonto(monto);
        evento.setDetalle("Importación legacy: " + total + " transacciones");
        return send("importacion", evento);
    }

    private TransaccionEvento base(String tipoEvento) {
        TransaccionEvento evento = new TransaccionEvento();
        evento.setEventoId(UUID.randomUUID().toString());
        evento.setTipoEvento(tipoEvento);
        evento.setOcurridoEn(Instant.now().toString());
        return evento;
    }

    private boolean send(String key, TransaccionEvento evento) {
        try {
            kafkaTemplate.send(Topics.TRANSACCIONES, key, evento).get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar {} en Kafka: {}", evento.getTipoEvento(), ex.toString());
            return false;
        }
    }
}
