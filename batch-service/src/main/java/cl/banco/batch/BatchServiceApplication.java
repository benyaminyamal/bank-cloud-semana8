package cl.banco.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchServiceApplication.class, args);
    }
}
