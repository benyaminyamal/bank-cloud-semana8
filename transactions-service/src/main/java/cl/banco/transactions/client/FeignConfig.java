package cl.banco.transactions.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    RequestInterceptor serviceTokenInterceptor(ServiceTokenManager tokens) {
        return template -> {
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + tokens.bearer());
        };
    }
}
