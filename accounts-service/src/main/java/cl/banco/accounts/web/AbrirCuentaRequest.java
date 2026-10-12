package cl.banco.accounts.web;

import java.math.BigDecimal;

public record AbrirCuentaRequest(String nombre, BigDecimal saldo, Integer edad, String tipo) {
}
