package cl.banco.gateway.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/cuentas")
    public Map<String, String> cuentas() {
        return respuesta("cuentas");
    }

    @RequestMapping("/fallback/transacciones")
    public Map<String, String> transacciones() {
        return respuesta("transacciones");
    }

    @RequestMapping("/fallback/eventos")
    public Map<String, String> eventos() {
        return respuesta("eventos");
    }

    private static Map<String, String> respuesta(String servicio) {
        return Map.of(
                "mensaje", "El servicio de " + servicio + " no está disponible.",
                "origen", "resilience4j"
        );
    }
}
