package com.ritechsas.backend.config;

import java.util.List;

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

    private static final String FRONTEND_URL =
            "https://frontend-ritech.onrender.com";

    // ---------------------------------------------------------
    // Password encoder
    // ---------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ---------------------------------------------------------
    // Authentication provider
    // ---------------------------------------------------------

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // ---------------------------------------------------------
    // Authentication manager
    // ---------------------------------------------------------

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ---------------------------------------------------------
    // CORS configuration
    // ---------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(FRONTEND_URL)
        );

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // ---------------------------------------------------------
    // Security filter chain
    // ---------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        // Permite que el frontend lea la cookie CSRF.
        // SameSite=None permite solicitudes entre dominios.
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .sameSite("None")
                .secure(true)
                .path("/")
        );

        http
                // CORS
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                // Authentication provider
                .authenticationProvider(authenticationProvider)

                // CSRF
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()
                        )

                        // Prueba de diagnóstico:
                        // permite que el login no sea bloqueado por CSRF.
                        // CSRF continúa activo para las demás rutas.
                        .ignoringRequestMatchers("/api/auth/login")
                )

                // Authorization
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public routes
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        // User administration
                        .requestMatchers(
                                "/api/users",
                                "/api/users/**"
                        ).hasRole("ADMINISTRADOR")

                        // Producers: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/producers",
                                "/api/producers/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS",
                                "GERENCIA",
                                "INVENTARIO",
                                "VENTAS"
                        )

                        // Producers: write
                        .requestMatchers(
                                "/api/producers",
                                "/api/producers/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS"
                        )

                        // Purchases: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/purchases",
                                "/api/purchases/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS",
                                "GERENCIA"
                        )

                        // Purchases: write
                        .requestMatchers(
                                "/api/purchases",
                                "/api/purchases/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS"
                        )

                        // Products: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS",
                                "INVENTARIO",
                                "EXPORTADOR",
                                "VENTAS",
                                "GERENCIA"
                        )

                        // Products: write
                        .requestMatchers(
                                "/api/products",
                                "/api/products/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "INVENTARIO"
                        )

                        // Inventory lots: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/inventory-lots",
                                "/api/inventory-lots/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "COMPRAS",
                                "INVENTARIO",
                                "EXPORTADOR",
                                "VENTAS",
                                "GERENCIA"
                        )

                        // Inventory lots: write
                        .requestMatchers(
                                "/api/inventory-lots",
                                "/api/inventory-lots/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "INVENTARIO"
                        )

                        // Certifications: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/certifications",
                                "/api/certifications/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "INVENTARIO",
                                "EXPORTADOR",
                                "GERENCIA"
                        )

                        // Certifications: write
                        .requestMatchers(
                                "/api/certifications",
                                "/api/certifications/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR"
                        )

                        // Clients: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/clients",
                                "/api/clients/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR",
                                "VENTAS",
                                "GERENCIA"
                        )

                        // Clients: write
                        .requestMatchers(
                                "/api/clients",
                                "/api/clients/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "VENTAS"
                        )

                        // Export orders and items: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/export-orders",
                                "/api/export-orders/**",
                                "/api/export-order-items",
                                "/api/export-order-items/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "INVENTARIO",
                                "EXPORTADOR",
                                "VENTAS",
                                "GERENCIA"
                        )

                        // Export orders and items: write
                        .requestMatchers(
                                "/api/export-orders",
                                "/api/export-orders/**",
                                "/api/export-order-items",
                                "/api/export-order-items/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR"
                        )

                        // Export documents: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/export-documents",
                                "/api/export-documents/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR",
                                "GERENCIA"
                        )

                        // Export documents: write
                        .requestMatchers(
                                "/api/export-documents",
                                "/api/export-documents/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR"
                        )

                        // Shipments: read
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/shipments",
                                "/api/shipments/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR",
                                "GERENCIA"
                        )

                        // Shipments: write
                        .requestMatchers(
                                "/api/shipments",
                                "/api/shipments/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "EXPORTADOR"
                        )

                        // Traceability reports
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/traceability/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "GERENCIA",
                                "EXPORTADOR"
                        )

                        // Other reports
                        .requestMatchers(
                                "/api/reports/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "GERENCIA"
                        )

                        // Every other route requires authentication
                        .anyRequest().authenticated()
                )

                // API does not use form login or HTTP Basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Return 401 for unauthenticated requests
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        )
                        )
                );

        return http.build();
    }
}