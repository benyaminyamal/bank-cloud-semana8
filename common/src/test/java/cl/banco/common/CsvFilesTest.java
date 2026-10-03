package cl.banco.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvFilesTest {

    @Test
    void conservaComasDentroDeComillas() {
        String csv = "id,descripcion\n1,\"pago, parcial\"\n";
        List<Map<String, String>> rows = CsvFiles.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, rows.size());
        assertEquals("pago, parcial", rows.get(0).get("descripcion"));
    }
}
