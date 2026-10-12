package cl.banco.bff.web.client;

import cl.banco.common.CuentasResumen;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "accounts-service", contextId = "webCuentas", fallbackFactory = CuentasFallback.class)
public interface CuentasClient {

    @GetMapping("/api/cuentas/resumen")
    CuentasResumen resumen();
}
