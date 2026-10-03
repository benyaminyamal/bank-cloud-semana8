package cl.banco.transactions.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class ServiceTokenManager {

    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenManager(
            @Value("${app.auth.token-uri}") String tokenUri,
            @Value("${app.auth.client-id}") String clientId,
            @Value("${app.auth.client-secret}") String clientSecret) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String bearer() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(20))) {
            return accessToken;
        }
        TokenResponse response = restClient.post()
                .uri(tokenUri)
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials&scope=accounts.read")
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("El servidor OAuth2 no entregó un access token");
        }
        this.accessToken = response.accessToken();
        long segundos = response.expiresIn() > 0 ? response.expiresIn() : 60;
        this.expiresAt = Instant.now().plusSeconds(segundos);
        return accessToken;
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
