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

@Component
public class LegacyCatalog {

    private static final Logger log = LoggerFactory.getLogger(LegacyCatalog.class);

    private final List<Cuenta> cuentas;
    private final List<MovimientoAnual> movimientosAnuales;

    public LegacyCatalog(ResourceLoader loader, @Value("${app.data.semana:semana_3}") String semana) {
        this.cuentas = CuentaParser.parse(read(loader, semana, "intereses.csv"));
        this.movimientosAnuales = MovimientoAnualParser.parse(read(loader, semana, "cuentas_anuales.csv"));
        log.info("Legacy {}: {} cuentas ({} válidas), {} movimientos anuales",
                semana, cuentas.size(), validas().size(), movimientosAnuales.size());
    }

    public List<Cuenta> cuentas(boolean soloValidas) {
        if (!soloValidas) {
            return cuentas;
        }
        return validas();
    }

    public Optional<Cuenta> porId(String cuentaId) {
        return cuentas.stream()
                .filter(cuenta -> cuenta.cuentaId().equals(cuentaId))
                .findFirst();
    }

    public List<MovimientoAnual> anuales(String cuentaId) {
        return movimientosAnuales.stream()
                .filter(movimiento -> movimiento.cuentaId().equals(cuentaId))
                .toList();
    }

    public CuentasResumen resumen() {
        List<Cuenta> validas = validas();
        BigDecimal saldo = validas.stream()
                .map(Cuenta::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuentasResumen(true, cuentas.size(), validas.size(), saldo, "ok");
    }

    private List<Cuenta> validas() {
        return cuentas.stream().filter(Cuenta::valida).toList();
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
