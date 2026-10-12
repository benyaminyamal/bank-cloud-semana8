package cl.banco.accounts.catalog;

import cl.banco.accounts.csv.CuentaParser;
import cl.banco.accounts.csv.MovimientoAnualParser;
import cl.banco.accounts.domain.Cuenta;
import cl.banco.accounts.domain.MovimientoAnual;
import cl.banco.common.CsvFiles;
import cl.banco.common.CuentasResumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LegacyCatalog {

    private static final Logger log = LoggerFactory.getLogger(LegacyCatalog.class);

    private final List<Cuenta> cuentas = new CopyOnWriteArrayList<>();
    private final Set<String> cerradas = ConcurrentHashMap.newKeySet();
    private final List<MovimientoAnual> movimientosAnuales;

    public LegacyCatalog(ResourceLoader loader, @Value("${app.data.semana:semana_3}") String semana) {
        this.cuentas.addAll(CuentaParser.parse(read(loader, semana, "intereses.csv")));
        this.movimientosAnuales = MovimientoAnualParser.parse(read(loader, semana, "cuentas_anuales.csv"));
        log.info("Legacy {}: {} cuentas ({} válidas), {} movimientos anuales",
                semana, cuentas.size(), validas().size(), movimientosAnuales.size());
    }

    public List<Cuenta> cuentas(boolean soloValidas) {
        List<Cuenta> abiertas = cuentas.stream()
                .filter(cuenta -> !cerradas.contains(cuenta.cuentaId()))
                .toList();
        if (!soloValidas) {
            return abiertas;
        }
        return abiertas.stream().filter(Cuenta::valida).toList();
    }

    public Optional<Cuenta> porId(String cuentaId) {
        if (cerradas.contains(cuentaId)) {
            return Optional.empty();
        }
        return cuentas.stream()
                .filter(cuenta -> cuenta.cuentaId().equals(cuentaId))
                .findFirst();
    }

    public synchronized Cuenta abrir(String nombre, BigDecimal saldo, Integer edad, String tipo) {
        long siguiente = cuentas.stream()
                .map(Cuenta::cuentaId)
                .filter(id -> id.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(1000L) + 1;
        Cuenta creada = new Cuenta(Long.toString(siguiente), nombre, saldo, edad, tipo, true, List.of());
        cuentas.add(creada);
        return creada;
    }

    public synchronized Cuenta cerrar(String cuentaId) {
        Cuenta cuenta = porId(cuentaId).orElseThrow();
        cerradas.add(cuentaId);
        return cuenta;
    }

    public synchronized Cuenta mantener(String cuentaId, String nombre, String tipo) {
        Cuenta actual = porId(cuentaId).orElseThrow();
        Cuenta actualizada = new Cuenta(
                actual.cuentaId(),
                nombre == null || nombre.isBlank() ? actual.nombre() : nombre,
                actual.saldo(),
                actual.edad(),
                tipo == null || tipo.isBlank() ? actual.tipo() : tipo,
                actual.valida(),
                actual.observaciones());
        cuentas.removeIf(cuenta -> cuenta.cuentaId().equals(cuentaId));
        cuentas.add(actualizada);
        return actualizada;
    }

    public List<MovimientoAnual> anuales(String cuentaId) {
        return movimientosAnuales.stream()
                .filter(movimiento -> movimiento.cuentaId().equals(cuentaId))
                .toList();
    }

    public CuentasResumen resumen() {
        List<Cuenta> validas = cuentas(true);
        BigDecimal saldo = validas.stream()
                .map(Cuenta::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuentasResumen(true, cuentas(false).size(), validas.size(), saldo, "ok");
    }

    private List<Cuenta> validas() {
        return cuentas(true);
    }

    private static List<java.util.Map<String, String>> read(ResourceLoader loader, String semana, String file) {
        String location = "classpath:data/" + semana + "/" + file;
        try (InputStream input = loader.getResource(location).getInputStream()) {
            return CsvFiles.read(input);
        } catch (IOException ex) {
            throw new IllegalStateException("No se encontró el dataset " + location, ex);
        }
    }
}
