package cl.banco.bff.web.client;

import cl.banco.common.CuentasResumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
class CuentasFallback implements FallbackFactory<CuentasClient> {

    private static final Logger log = LoggerFactory.getLogger(CuentasFallback.class);

    @Override
    public CuentasClient create(Throwable cause) {
        log.warn("BFF web sin cuentas: {}", cause.toString());
        return CuentasResumen::noDisponible;
    }
}

@Component
class ClientesFallback implements FallbackFactory<ClientesClient> {

    private static final Logger log = LoggerFactory.getLogger(ClientesFallback.class);

    @Override
    public ClientesClient create(Throwable cause) {
        log.warn("BFF web sin clientes: {}", cause.toString());
        return List::of;
    }
}
