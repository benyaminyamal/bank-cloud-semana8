package cl.banco.batch.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReejecutorCritico {

    private static final Logger log = LoggerFactory.getLogger(ReejecutorCritico.class);

    private final JobExplorer explorer;
    private final JobOperator operator;
    private final Set<Long> reejecutados = ConcurrentHashMap.newKeySet();

    public ReejecutorCritico(JobExplorer explorer, JobOperator operator) {
        this.explorer = explorer;
        this.operator = operator;
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 20000)
    public void reejecutar() {
        for (String nombre : explorer.getJobNames()) {
            explorer.getJobInstances(nombre, 0, 5).forEach(instance -> {
                var ejecuciones = explorer.getJobExecutions(instance);
                if (ejecuciones.isEmpty()) {
                    return;
                }
                var ultima = ejecuciones.get(0);
                if (ultima.getStatus() == BatchStatus.FAILED && reejecutados.add(ultima.getId())) {
                    try {
                        log.warn("Fallo crítico en {} ({}). Se reejecuta.", nombre, ultima.getId());
                        operator.restart(ultima.getId());
                    } catch (Exception ex) {
                        log.warn("No se pudo reejecutar {}: {}", ultima.getId(), ex.toString());
                    }
                }
            });
        }
    }
}
