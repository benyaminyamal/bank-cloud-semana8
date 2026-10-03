package cl.banco.transactions.client;

import cl.banco.common.CuentasResumen;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "accounts-service", fallbackFactory = AccountsClientFallbackFactory.class)
public interface AccountsClient {

    @GetMapping("/api/cuentas/resumen")
    CuentasResumen resumen();
}
