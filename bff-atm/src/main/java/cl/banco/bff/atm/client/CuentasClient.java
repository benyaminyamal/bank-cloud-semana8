package cl.banco.bff.atm.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@FeignClient(name = "accounts-service", contextId = "atmCuentas", fallbackFactory = CuentasClient.Fallback.class)
public interface CuentasClient {

    @GetMapping("/api/cuentas/{id}")
    Map<String, Object> cuenta(@PathVariable("id") String id);

    @Component
    class Fallback implements FallbackFactory<CuentasClient> {
        private static final Logger log = LoggerFactory.getLogger(Fallback.class);

        @Override
        public CuentasClient create(Throwable cause) {
            log.warn("Cajero sin cuentas: {}", cause.toString());
            return id -> Map.of("cuentaId", id, "saldo", BigDecimal.ZERO, "disponible", false,
                    "mensaje", "El servicio de cuentas no está disponible.");
        }
    }
}

@Configuration
class FeignConfig {
    @Bean
    RequestInterceptor serviceTokenInterceptor(ServiceTokenManager tokens) {
        return template -> {
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + tokens.bearer());
        };
    }
}

@Component
class ServiceTokenManager {
    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    ServiceTokenManager(
            @Value("${app.auth.token-uri}") String tokenUri,
            @Value("${app.auth.client-id}") String clientId,
            @Value("${app.auth.client-secret}") String clientSecret,
            @Value("${app.auth.scope}") String scope) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    synchronized String bearer() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(20))) {
            return accessToken;
        }
        TokenResponse response = restClient.post()
                .uri(tokenUri)
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("El servidor OAuth2 no entregó un access token");
        }
        this.accessToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(response.expiresIn() > 0 ? response.expiresIn() : 60);
        return accessToken;
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
