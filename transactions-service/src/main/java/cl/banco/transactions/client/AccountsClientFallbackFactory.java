package cl.banco.transactions.client;

import cl.banco.common.CuentasResumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountsClientFallbackFactory implements FallbackFactory<AccountsClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountsClientFallbackFactory.class);

    @Override
    public AccountsClient create(Throwable cause) {
        log.warn("Circuit breaker de cuentas activo: {}", cause.toString());
        return CuentasResumen::noDisponible;
    }
}
