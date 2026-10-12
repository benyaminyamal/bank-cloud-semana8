package cl.banco.payments.web;

import java.math.BigDecimal;

public record PagoRequest(String tipo, String cuentaOrigen, String cuentaDestino, BigDecimal monto) {
}
