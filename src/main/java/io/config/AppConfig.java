package io.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.filter.SessionAuthFilter;
import io.service.SessionService;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("io")
@Import(DataBaseConfig.class)
public class AppConfig {

    @Bean(name = "sessionAuthFilter")
    public Filter sessionAuthFilter(SessionService sessionService) {
        return new SessionAuthFilter(sessionService);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
