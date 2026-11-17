package pl.ans.ipbisr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punkt startowy backendu (job workery Camunda + REST API)
@SpringBootApplication
public class IpbisrApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpbisrApplication.class, args);
    }
}
