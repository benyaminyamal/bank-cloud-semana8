package cl.banco.customers.domain;

import java.util.List;

public record Cliente(
        String clienteId,
        String nombre,
        Integer edad,
        String tipoCuenta,
        String perfil,
        boolean valida,
        List<String> observaciones
) {
}
