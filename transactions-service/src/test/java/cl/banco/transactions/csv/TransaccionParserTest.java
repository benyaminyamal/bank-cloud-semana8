package cl.banco.transactions.csv;

import cl.banco.common.CsvFiles;
import cl.banco.transactions.domain.Transaccion;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransaccionParserTest {

    @Test
    void marcaMontosVaciosNegativosYTiposInvalidos() {
        String csv = """
                id,fecha,monto,tipo
                1,2024-06-30,3000,credito
                1,2024-06-30,3000,credito
                4,04/05/2024,,invalid
                9,2024-01-03,-200,debito
                """;
        List<Transaccion> transacciones = TransaccionParser.parse(CsvFiles.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
        assertTrue(transacciones.get(0).valida());
        assertFalse(transacciones.get(1).valida());
        assertFalse(transacciones.get(2).valida());
        assertFalse(transacciones.get(3).valida());
        assertTrue(TransaccionParser.crear(20, "2024-12-01", new BigDecimal("1500"), "credito").valida());
    }

    @Test
    void leeElDatasetDeLaSemana3() throws Exception {
        String csv = Files.readString(Path.of("../data/semana_3/transacciones.csv"));
        List<Transaccion> transacciones = TransaccionParser.parse(CsvFiles.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
        assertEquals(1000, transacciones.size());
        assertTrue(transacciones.stream().anyMatch(Transaccion::valida));
        assertTrue(transacciones.stream().anyMatch(transaccion -> !transaccion.valida()));
    }
}
