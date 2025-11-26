package md.dpscs.cch.iis.config; // Ensure this package matches yours

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Standard for REST APIs)
                .csrf(csrf -> csrf.disable())

                // 2. Link the CORS config defined below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. THE CRITICAL PART: Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // You MUST list /api/hello here to allow it without login
                        .requestMatchers("/api/ident/**").permitAll()

                        // All other URLs are locked down
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow your IIS Origins
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://ident-index-app-dev.dpscs.ad.icj.mdstate"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}