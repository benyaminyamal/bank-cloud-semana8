package cl.banco.transactions.web;

import cl.banco.transactions.domain.Transaccion;

public record TransaccionCreadaResponse(Transaccion transaccion, boolean eventoPublicado) {
}
