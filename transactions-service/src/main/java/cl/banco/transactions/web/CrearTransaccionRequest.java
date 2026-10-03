package cl.banco.transactions.web;

import java.math.BigDecimal;

public record CrearTransaccionRequest(String fecha, BigDecimal monto, String tipo) {
}
