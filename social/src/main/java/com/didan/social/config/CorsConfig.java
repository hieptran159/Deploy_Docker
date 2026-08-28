package com.didan.social.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {

    // Danh sách origin được phép, ngăn cách bằng dấu phẩy. Mặc định "*" (giữ nguyên hành vi cũ).
    // Nên đặt cụ thể ở môi trường thật: APP_CORS_ALLOWED_ORIGINS=https://hipe.id.vn,https://didan.id.vn
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split("\\s*,\\s*");
        registry.addMapping("/**")
                .allowedOriginPatterns(origins) // hỗ trợ cả "*" lẫn danh sách domain cụ thể
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
