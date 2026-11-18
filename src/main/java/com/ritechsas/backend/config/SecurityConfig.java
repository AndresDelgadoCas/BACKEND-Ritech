package com.ritechsas.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ritechsas.backend.service.DatabaseUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.cors.allowed-origin:https://frontend-ritech.onrender.com}")
    private String frontendOrigin;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new ArrayList<>();
        origins.add(frontendOrigin);
        origins.add("http://localhost:5173");
        origins.add("http://127.0.0.1:5173");
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.sameSite("None").secure(true).path("/"));

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(authenticationProvider)
            .csrf(c -> c.csrfTokenRepository(csrf)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/auth/login"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/marketplace/catalog").hasRole("COMPRADOR")
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/users/**", "/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/seller-applications/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/support/management/**").hasAnyRole("SOPORTE", "ADMINISTRADOR")
                .requestMatchers("/api/support/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/support/**").authenticated()
                .requestMatchers("/api/marketplace/favorites/**").hasRole("COMPRADOR")
                .requestMatchers("/api/seller-applications/**").authenticated()
                .requestMatchers("/api/marketplace/seller/**").hasRole("VENDEDOR")
                .requestMatchers("/api/marketplace/cart/**", "/api/marketplace/checkout").hasRole("COMPRADOR")
                .requestMatchers(HttpMethod.GET, "/api/marketplace/cart").hasRole("COMPRADOR")
                .requestMatchers(HttpMethod.GET, "/api/marketplace/products").hasRole("VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/marketplace/orders").hasAnyRole("VENDEDOR", "COMPRADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/marketplace/orders/*/status").hasRole("VENDEDOR")
                .anyRequest().authenticated())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }
}
