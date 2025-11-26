package md.dpscs.cch.iis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply to ALL endpoints
                        .allowedOrigins("https://localhost", "http://localhost") // Allow IIS Origins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow all standard actions
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
