package cl.banco.bff.web.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "customers-service", contextId = "webClientes", fallbackFactory = ClientesFallback.class)
public interface ClientesClient {

    @GetMapping("/api/clientes")
    List<Map<String, Object>> listar();
}
