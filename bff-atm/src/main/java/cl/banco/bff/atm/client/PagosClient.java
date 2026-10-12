package cl.banco.bff.atm.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payments-service", contextId = "atmPagos", fallbackFactory = PagosClient.Fallback.class)
public interface PagosClient {

    @PostMapping("/api/pagos")
    Map<String, Object> crear(@RequestBody Map<String, Object> pago);

    @Component
    class Fallback implements FallbackFactory<PagosClient> {
        private static final Logger log = LoggerFactory.getLogger(Fallback.class);

        @Override
        public PagosClient create(Throwable cause) {
            log.warn("Cajero sin pagos: {}", cause.toString());
            return pago -> Map.of("eventoPublicado", false, "alertaPublicada", false,
                    "mensaje", "El servicio de pagos no está disponible.");
        }
    }
}
