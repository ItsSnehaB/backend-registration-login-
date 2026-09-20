package com.example.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3036}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split("\\s*,\\s*");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedOriginPatterns("https://*.vercel.app", "http://localhost:*")
                .allowedMethods("POST", "GET", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
