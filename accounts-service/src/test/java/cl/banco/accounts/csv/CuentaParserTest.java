package cl.banco.accounts.csv;

import cl.banco.accounts.domain.Cuenta;
import cl.banco.common.CsvFiles;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuentaParserTest {

    @Test
    void marcaDuplicadosSaldosVaciosYTiposInvalidos() {
        String csv = """
                cuenta_id,nombre,saldo,edad,tipo
                101,Ana,5000,30,ahorro
                101,Ana,5000,30,ahorro
                114,Unknown,,30,-1
                120,Luis,-10,200,prestamo
                """;
        List<Cuenta> cuentas = CuentaParser.parse(CsvFiles.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));

        assertEquals(4, cuentas.size());
        assertTrue(cuentas.get(0).valida());
        assertFalse(cuentas.get(1).valida());
        assertTrue(cuentas.get(1).observaciones().contains("cuenta duplicada"));
        assertFalse(cuentas.get(2).valida());
        assertFalse(cuentas.get(3).valida());
    }

    @Test
    void leeElDatasetDeLaSemana3() throws Exception {
        String csv = Files.readString(Path.of("../data/semana_3/intereses.csv"));
        List<Cuenta> cuentas = CuentaParser.parse(CsvFiles.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
        assertEquals(1000, cuentas.size());
        assertTrue(cuentas.stream().anyMatch(Cuenta::valida));
        assertTrue(cuentas.stream().anyMatch(cuenta -> !cuenta.valida()));
    }
}
