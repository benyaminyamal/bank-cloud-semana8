package cl.banco.bff.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BffWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}
