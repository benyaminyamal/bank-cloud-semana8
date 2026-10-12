package cl.banco.batch.web;

import cl.banco.batch.job.ResultadosBatch;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@PreAuthorize("hasAuthority('SCOPE_batch.run')")
public class BatchController {

    private final Map<String, Job> jobs;
    private final JobLauncher jobLauncher;
    private final JobOperator jobOperator;
    private final ResultadosBatch resultados;

    public BatchController(Map<String, Job> jobs, JobLauncher jobLauncher, JobOperator jobOperator, ResultadosBatch resultados) {
        this.jobs = jobs;
        this.jobLauncher = jobLauncher;
        this.jobOperator = jobOperator;
        this.resultados = resultados;
    }

    @PostMapping("/jobs/{nombre}")
    public Map<String, Object> lanzar(@PathVariable String nombre) throws Exception {
        Job job = jobs.get(nombre);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job no encontrado");
        }
        var execution = jobLauncher.run(job, new JobParametersBuilder()
                .addLong("ejecucion", System.currentTimeMillis())
                .toJobParameters());
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("job", nombre);
        respuesta.put("ejecucionId", execution.getId());
        respuesta.put("estado", execution.getStatus().toString());
        respuesta.put("reporte", reporteDe(nombre));
        return respuesta;
    }

    @PostMapping("/ejecuciones/{id}/reiniciar")
    public Map<String, Object> reiniciar(@PathVariable long id) throws Exception {
        Long nueva = jobOperator.restart(id);
        return Map.of("ejecucionAnterior", id, "ejecucionId", nueva);
    }

    @GetMapping("/reportes/{nombre}")
    public Map<String, Object> reporte(@PathVariable String nombre) {
        return reporteDe(nombre);
    }

    private Map<String, Object> reporteDe(String nombre) {
        return switch (nombre) {
            case "reporteTransaccionesDiarias" -> resultados.reporteDiario();
            case "calculoInteresesMensuales" -> resultados.reporteIntereses();
            case "estadoCuentaAnual" -> resultados.reporteAnual();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        };
    }
}
