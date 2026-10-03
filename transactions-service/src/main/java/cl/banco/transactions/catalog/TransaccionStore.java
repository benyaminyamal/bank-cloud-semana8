package cl.banco.transactions.catalog;

import cl.banco.common.CsvFiles;
import cl.banco.transactions.csv.TransaccionParser;
import cl.banco.transactions.domain.Transaccion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TransaccionStore {

    private static final Logger log = LoggerFactory.getLogger(TransaccionStore.class);

    private final List<Transaccion> transacciones = new CopyOnWriteArrayList<>();

    public TransaccionStore(ResourceLoader loader, @Value("${app.data.semana:semana_3}") String semana) {
        String location = "classpath:data/" + semana + "/transacciones.csv";
        try (InputStream input = loader.getResource(location).getInputStream()) {
            transacciones.addAll(TransaccionParser.parse(CsvFiles.read(input)));
        } catch (IOException ex) {
            throw new IllegalStateException("No se encontró el dataset " + location, ex);
        }
        log.info("Legacy {}: {} transacciones ({} válidas)", semana, transacciones.size(), validas().size());
    }

    public List<Transaccion> listar(boolean soloValidas) {
        if (!soloValidas) {
            return List.copyOf(transacciones);
        }
        return validas();
    }

    public Optional<Transaccion> porId(long id) {
        return transacciones.stream().filter(transaccion -> id == transaccion.id()).findFirst();
    }

    public synchronized Transaccion crear(String fecha, BigDecimal monto, String tipo) {
        long siguiente = transacciones.stream()
                .map(Transaccion::id)
                .filter(id -> id != null)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1;
        Transaccion creada = TransaccionParser.crear(siguiente, fecha, monto, tipo);
        if (!creada.valida()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", creada.observaciones()));
        }
        transacciones.add(creada);
        return creada;
    }

    public int total() {
        return transacciones.size();
    }

    public BigDecimal montoValidas() {
        return validas().stream()
                .map(Transaccion::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int validasCount() {
        return validas().size();
    }

    private List<Transaccion> validas() {
        return transacciones.stream().filter(Transaccion::valida).toList();
    }
}
