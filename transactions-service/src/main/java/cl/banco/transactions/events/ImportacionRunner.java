package cl.banco.transactions.events;

import cl.banco.transactions.catalog.TransaccionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ImportacionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportacionRunner.class);

    private final TransaccionStore store;
    private final EventPublisher publisher;

    public ImportacionRunner(TransaccionStore store, EventPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean publicado = publisher.publicarImportacion(store.total(), store.montoValidas());
        log.info("Evento de importación publicado: {}", publicado);
    }
}
