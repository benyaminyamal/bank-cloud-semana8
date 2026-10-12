package cl.banco.payments.events;

import cl.banco.common.Topics;
import cl.banco.common.TransaccionEvento;
import cl.banco.payments.domain.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PagoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoPublisher.class);

    private final KafkaTemplate<String, TransaccionEvento> kafkaTemplate;

    public PagoPublisher(KafkaTemplate<String, TransaccionEvento> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean publicado(Pago pago) {
        return send(Topics.PAGOS, pago, "PAGO_COMPLETADO", "Pago registrado");
    }

    public boolean alerta(Pago pago) {
        return send(Topics.ALERTAS, pago, "ALERTA_SEGURIDAD", "Retiro de monto alto");
    }

    private boolean send(String topic, Pago pago, String tipoEvento, String detalle) {
        TransaccionEvento evento = new TransaccionEvento();
        evento.setEventoId(UUID.randomUUID().toString());
        evento.setTipoEvento(tipoEvento);
        evento.setTransaccionId(pago.id());
        evento.setTipoMovimiento(pago.tipo());
        evento.setMonto(pago.monto());
        evento.setDetalle(detalle + " origen " + pago.cuentaOrigen());
        evento.setOcurridoEn(Instant.now().toString());
        try {
            kafkaTemplate.send(topic, Long.toString(pago.id()), evento).get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar {} en {}: {}", tipoEvento, topic, ex.toString());
            return false;
        }
    }
}
