package pl.ans.ipbisr.config;

import java.util.function.DoubleSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS dla frontendu (Next.js na porcie 3000) i źródło losowości dla workerów. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Losowanie 0–1 jako osobny bean, żeby w testach dało się je podmienić na stałą wartość. */
    @Bean
    public DoubleSupplier losowanie() {
        return Math::random;
    }
}
