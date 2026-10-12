package cl.banco.batch.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@SpringBatchTest
class ReporteDiarioJobTest {

    @Autowired
    private JobLauncherTestUtils utils;

    @Autowired
    @Qualifier("reporteTransaccionesDiarias")
    private Job reporteTransaccionesDiarias;

    @Autowired
    @Qualifier("calculoInteresesMensuales")
    private Job calculoInteresesMensuales;

    @Autowired
    @Qualifier("estadoCuentaAnual")
    private Job estadoCuentaAnual;

    @Autowired
    private ResultadosBatch resultados;

    @BeforeEach
    void preparar() {
        utils.setJob(reporteTransaccionesDiarias);
    }

    @Test
    void reporteDiarioEquivaleAlLegado() throws Exception {
        var execution = utils.launchJob(new JobParametersBuilder().addLong("ejecucion", System.nanoTime()).toJobParameters());
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(resultados.reporteDiario())
                .containsEntry("leidas", 1000)
                .containsEntry("validas", 401)
                .containsEntry("montoValidas", new BigDecimal("527000"));
    }

    @Test
    void interesesYEstadosCompletan() throws Exception {
        utils.setJob(calculoInteresesMensuales);
        var intereses = utils.launchJob(new JobParametersBuilder().addLong("ejecucion", System.nanoTime()).toJobParameters());
        assertThat(intereses.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat((Integer) resultados.reporteIntereses().get("aplicados")).isPositive();

        utils.setJob(estadoCuentaAnual);
        var anual = utils.launchJob(new JobParametersBuilder().addLong("ejecucion", System.nanoTime()).toJobParameters());
        assertThat(anual.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat((Integer) resultados.reporteAnual().get("movimientosValidos")).isPositive();
    }
}
