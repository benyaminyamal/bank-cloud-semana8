package cl.banco.batch.job;

import cl.banco.batch.reporte.AnomaliaClasificador;
import cl.banco.batch.reporte.InteresCalculado;
import cl.banco.batch.reporte.InteresCalculador;
import cl.banco.batch.reporte.TransaccionClasificada;
import cl.banco.common.LegacyDates;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class BatchJobsConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchJobsConfig.class);

    @Bean
    public Set<Long> idsDiarios() {
        return ConcurrentHashMap.newKeySet();
    }

    @Bean(name = "reporteTransaccionesDiarias")
    @Primary
    Job reporteTransaccionesDiarias(JobRepository repository, Step reporteDiarioStep) {
        return new JobBuilder("reporteTransaccionesDiarias", repository)
                .start(reporteDiarioStep)
                .build();
    }

    @Bean
    Step reporteDiarioStep(
            JobRepository repository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<TransaccionCruda> transaccionesReader,
            Set<Long> idsDiarios,
            ResultadosBatch resultados) {
        return new StepBuilder("leerProcesarEscribirTransacciones", repository)
                .<TransaccionCruda, TransaccionClasificada>chunk(200, transactionManager)
                .reader(transaccionesReader)
                .processor(cruda -> AnomaliaClasificador.clasificar(
                        cruda.id(), cruda.fecha(), cruda.monto(), cruda.tipo(), idsDiarios))
                .writer(diarioWriter(resultados))
                .listener(reinicio(idsDiarios, resultados))
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(5000)
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .listener(skipListener("reporte diario"))
                .startLimit(3)
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    FlatFileItemReader<TransaccionCruda> transaccionesReader(@Value("${app.data.semana:semana_3}") String semana) {
        return new FlatFileItemReaderBuilder<TransaccionCruda>()
                .name("transaccionesReader")
                .resource(new ClassPathResource("data/" + semana + "/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("id", "fecha", "monto", "tipo")
                .strict(false)
                .fieldSetMapper(fields -> new TransaccionCruda(
                        fields.readString("id"),
                        fields.readString("fecha"),
                        fields.readString("monto"),
                        fields.readString("tipo")))
                .build();
    }

    @Bean(name = "calculoInteresesMensuales")
    Job calculoInteresesMensuales(JobRepository repository, Step interesesStep) {
        return new JobBuilder("calculoInteresesMensuales", repository)
                .start(interesesStep)
                .build();
    }

    @Bean
    Step interesesStep(
            JobRepository repository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<CuentaCruda> interesesReader,
            ResultadosBatch resultados) {
        return new StepBuilder("leerProcesarEscribirIntereses", repository)
                .<CuentaCruda, InteresCalculado>chunk(200, transactionManager)
                .reader(interesesReader)
                .processor(cruda -> {
                    boolean cuentaValida = cruda.valida();
                    if (!cuentaValida) {
                        return new InteresCalculado(cruda.cuentaId(), cruda.tipo(), cruda.saldo(),
                                BigDecimal.ZERO, false, "cuenta legacy no válida");
                    }
                    return InteresCalculador.calcular(cruda.cuentaId(), cruda.tipo(), cruda.saldo());
                })
                .writer(chunk -> chunk.getItems().forEach(resultados::acumularInteres))
                .listener((StepExecutionListener) new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        resultados.reiniciarIntereses();
                    }
                })
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(5000)
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .listener(skipListener("intereses"))
                .startLimit(3)
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    FlatFileItemReader<CuentaCruda> interesesReader(@Value("${app.data.semana:semana_3}") String semana) {
        return new FlatFileItemReaderBuilder<CuentaCruda>()
                .name("interesesReader")
                .resource(new ClassPathResource("data/" + semana + "/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuenta_id", "nombre", "saldo", "edad", "tipo")
                .strict(false)
                .fieldSetMapper(campos(new HashSet<>()))
                .build();
    }

    @Bean(name = "estadoCuentaAnual")
    Job estadoCuentaAnual(JobRepository repository, Step estadoAnualStep) {
        return new JobBuilder("estadoCuentaAnual", repository)
                .start(estadoAnualStep)
                .build();
    }

    @Bean
    Step estadoAnualStep(
            JobRepository repository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<MovimientoCrudo> anualesReader,
            ResultadosBatch resultados) {
        return new StepBuilder("leerProcesarEscribirEstadosAnuales", repository)
                .<MovimientoCrudo, MovimientoCrudo>chunk(200, transactionManager)
                .reader(anualesReader)
                .processor(movimiento -> movimiento)
                .writer(chunk -> chunk.getItems().forEach(item -> resultados.acumularAnual(
                        item.cuentaId(), item.transaccion(), item.monto(), item.valida())))
                .listener((StepExecutionListener) new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        resultados.reiniciarAnual();
                    }
                })
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(5000)
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .listener(skipListener("estado anual"))
                .startLimit(3)
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    FlatFileItemReader<MovimientoCrudo> anualesReader(@Value("${app.data.semana:semana_3}") String semana) {
        return new FlatFileItemReaderBuilder<MovimientoCrudo>()
                .name("anualesReader")
                .resource(new ClassPathResource("data/" + semana + "/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .strict(false)
                .fieldSetMapper(fields -> MovimientoCrudo.desde(
                        fields.readString("cuenta_id"),
                        fields.readString("fecha"),
                        fields.readString("transaccion"),
                        fields.readString("monto"),
                        fields.readString("descripcion")))
                .build();
    }

    private static org.springframework.batch.item.file.mapping.FieldSetMapper<CuentaCruda> campos(Set<String> vistos) {
        return fields -> CuentaCruda.desde(
                fields.readString("cuenta_id"),
                fields.readString("nombre"),
                fields.readString("saldo"),
                fields.readString("edad"),
                fields.readString("tipo"),
                vistos);
    }

    private static ItemWriter<TransaccionClasificada> diarioWriter(ResultadosBatch resultados) {
        return (Chunk<? extends TransaccionClasificada> chunk) -> chunk.getItems().forEach(resultados::acumularDiario);
    }

    private static StepExecutionListener reinicio(Set<Long> idsDiarios, ResultadosBatch resultados) {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                idsDiarios.clear();
                resultados.reiniciarDiario();
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                return stepExecution.getExitStatus();
            }
        };
    }

    private static SkipListener<Object, Object> skipListener(String paso) {
        return new SkipListener<>() {
            @Override
            public void onSkipInRead(Throwable error) {
                log.warn("Fila omitida al leer {}: {}", paso, error.toString());
            }

            @Override
            public void onSkipInProcess(Object item, Throwable error) {
                log.warn("Fila omitida al procesar {}: {}", paso, error.toString());
            }

            @Override
            public void onSkipInWrite(Object item, Throwable error) {
                log.warn("Fila omitida al escribir {}: {}", paso, error.toString());
            }
        };
    }

    public record TransaccionCruda(String id, String fecha, String monto, String tipo) {
    }

    public record CuentaCruda(String cuentaId, String tipo, BigDecimal saldo, boolean valida) {
        static CuentaCruda desde(String cuentaId, String nombre, String saldo, String edad, String tipo, Set<String> vistos) {
            String id = LegacyTexts.clean(cuentaId);
            String tipoLimpio = LegacyTexts.clean(tipo).toLowerCase(Locale.ROOT);
            var saldoOk = LegacyNumbers.money(saldo);
            var edadOk = LegacyNumbers.integer(edad);
            String nombreLimpio = LegacyTexts.clean(nombre);
            boolean duplicada = !id.isEmpty() && !vistos.add(id);
            boolean valida = !id.isEmpty()
                    && !duplicada
                    && !nombreLimpio.isEmpty()
                    && !"unknown".equalsIgnoreCase(nombreLimpio)
                    && saldoOk.isPresent()
                    && saldoOk.get().compareTo(BigDecimal.ZERO) >= 0
                    && edadOk.isPresent()
                    && edadOk.get() >= 0
                    && edadOk.get() <= 120
                    && Set.of("ahorro", "prestamo", "hipoteca").contains(tipoLimpio);
            return new CuentaCruda(id, tipoLimpio, saldoOk.orElse(BigDecimal.ZERO), valida);
        }
    }

    public record MovimientoCrudo(String cuentaId, String transaccion, BigDecimal monto, boolean valida) {
        static MovimientoCrudo desde(String cuentaId, String fecha, String transaccion, String monto, String descripcion) {
            String id = LegacyTexts.clean(cuentaId);
            String tipo = LegacyTexts.clean(transaccion).toLowerCase(Locale.ROOT);
            var fechaOk = LegacyDates.parse(fecha);
            var montoOk = LegacyNumbers.money(monto);
            boolean valida = !id.isEmpty()
                    && fechaOk.isPresent()
                    && !tipo.isEmpty()
                    && montoOk.isPresent()
                    && montoOk.get().compareTo(BigDecimal.ZERO) > 0;
            return new MovimientoCrudo(id, tipo, montoOk.orElse(BigDecimal.ZERO), valida);
        }
    }
}
