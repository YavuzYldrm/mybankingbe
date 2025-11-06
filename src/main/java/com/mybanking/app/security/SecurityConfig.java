package com.mybanking.app.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // public
                        .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // swagger
                        .requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                        // admin
                        .requestMatchers("/api/accounts/admin/**").hasRole("ADMIN")
                        // rest required login
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var cfg = new org.springframework.web.cors.CorsConfiguration();
        cfg.setAllowedOrigins(java.util.List.of("http://localhost:5173", "http://localhost:3000"));
        cfg.setAllowedMethods(java.util.List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(java.util.List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        cfg.setExposedHeaders(java.util.List.of("Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        var src = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
