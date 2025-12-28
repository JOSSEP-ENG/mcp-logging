package com.example.mcplogging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * 현재는 개발 모드로 모든 요청 허용
 * TODO: 추후 JWT 인증 추가 필요
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API이므로)
                .csrf(AbstractHttpConfigurer::disable)

                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔 허용 (개발 환경)
                        .requestMatchers("/h2-console/**").permitAll()
                        // API 엔드포인트 모두 허용 (임시)
                        .requestMatchers("/api/**").permitAll()
                        // 그 외 모든 요청 허용
                        .anyRequest().permitAll()
                )

                // H2 콘솔을 위한 프레임 옵션 비활성화
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                );

        return http.build();
    }
}
