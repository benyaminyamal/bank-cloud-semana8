package cl.banco.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/fallback/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${app.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.jwt.issuer}") String issuer) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(createDefaultWithIssuer(issuer));
        return decoder;
    }
}
