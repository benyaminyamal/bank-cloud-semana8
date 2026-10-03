package cl.banco.transactions.web;

import cl.banco.common.CuentasResumen;

import java.math.BigDecimal;

public record InformeTransacciones(
        int totalRegistros,
        int transaccionesValidas,
        BigDecimal montoTotal,
        boolean cuentasDisponibles,
        CuentasResumen cuentas,
        String mensaje
) {
}
