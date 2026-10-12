package cl.banco.customers.catalog;

import cl.banco.common.CsvFiles;
import cl.banco.common.LegacyNumbers;
import cl.banco.common.LegacyTexts;
import cl.banco.customers.domain.Cliente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class ClientesCatalog {

    private final List<Cliente> clientes;

    public ClientesCatalog(ResourceLoader loader, @Value("${app.data.semana:semana_3}") String semana) {
        String location = "classpath:data/" + semana + "/intereses.csv";
        try (InputStream input = loader.getResource(location).getInputStream()) {
            this.clientes = parse(CsvFiles.read(input));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + location, ex);
        }
    }

    public List<Cliente> clientes() {
        return clientes;
    }

    public Optional<Cliente> porId(String id) {
        return clientes.stream().filter(cliente -> cliente.clienteId().equals(id)).findFirst();
    }

    private static List<Cliente> parse(List<java.util.Map<String, String>> rows) {
        Set<String> vistos = new HashSet<>();
        List<Cliente> clientes = new ArrayList<>();
        for (var row : rows) {
            List<String> observaciones = new ArrayList<>();
            String id = LegacyTexts.clean(row.get("cuenta_id"));
            String nombre = LegacyTexts.clean(row.get("nombre"));
            String tipo = LegacyTexts.clean(row.get("tipo")).toLowerCase(Locale.ROOT);
            Optional<BigDecimal> saldo = LegacyNumbers.money(row.get("saldo"));
            Optional<Integer> edad = LegacyNumbers.integer(row.get("edad"));
            boolean valida = true;
            if (id.isEmpty()) {
                observaciones.add("cliente sin identificador");
                valida = false;
            } else if (!vistos.add(id)) {
                observaciones.add("cliente duplicado");
                valida = false;
            }
            if (nombre.isEmpty() || "unknown".equalsIgnoreCase(nombre)) {
                observaciones.add("nombre no válido");
                valida = false;
            }
            if (saldo.isEmpty() || saldo.get().compareTo(BigDecimal.ZERO) < 0) {
                observaciones.add("saldo no válido");
                valida = false;
            }
            if (edad.isEmpty() || edad.get() < 0 || edad.get() > 120) {
                observaciones.add("edad fuera de rango");
                valida = false;
            }
            if (!Set.of("ahorro", "prestamo", "hipoteca").contains(tipo)) {
                observaciones.add("tipo de cuenta no válido");
                valida = false;
            }
            clientes.add(new Cliente(id, nombre, edad.orElse(null), tipo, valida ? "activo" : "observado", valida, List.copyOf(observaciones)));
        }
        return List.copyOf(clientes);
    }
}
