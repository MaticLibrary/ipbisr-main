package pl.ans.ipbisr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Punkt startowy backendu (job workery Camunda + REST API)
@SpringBootApplication
@EnableScheduling
public class IpbisrApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpbisrApplication.class, args);
    }
}
