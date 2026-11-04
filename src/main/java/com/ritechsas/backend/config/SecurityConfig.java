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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String FRONTEND_URL =
            "https://frontend-ritech.onrender.com";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    /*
     * Configuración CORS:
     * Permite que el frontend de Render se comunique con el backend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(FRONTEND_URL)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-XSRF-TOKEN",
                        "X-Requested-With"
                )
        );

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http
            // CORS debe procesarse antes de la autenticación
            .cors(cors -> cors
                    .configurationSource(corsConfigurationSource())
            )

            .authenticationProvider(authenticationProvider)

            // Configuración CSRF
            .csrf(csrf -> csrf
                    .csrfTokenRepository(
                            CookieCsrfTokenRepository.withHttpOnlyFalse()
                    )
                    .csrfTokenRequestHandler(
                            new CsrfTokenRequestAttributeHandler()
                    )
            )

            // Permisos de las rutas
            .authorizeHttpRequests(auth -> auth

                // Permitir solicitudes CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Acceso público
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

                // Administración de usuarios
                .requestMatchers(
                        "/api/users",
                        "/api/users/**"
                ).hasRole("ADMINISTRADOR")

                // Productores: consulta
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

                // Productores: escritura
                .requestMatchers(
                        "/api/producers",
                        "/api/producers/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "COMPRAS"
                )

                // Compras: consulta
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/purchases",
                        "/api/purchases/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "COMPRAS",
                        "GERENCIA"
                )

                // Compras: escritura
                .requestMatchers(
                        "/api/purchases",
                        "/api/purchases/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "COMPRAS"
                )

                // Productos: consulta
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

                // Productos: escritura
                .requestMatchers(
                        "/api/products",
                        "/api/products/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "INVENTARIO"
                )

                // Inventario: consulta
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

                // Inventario: escritura
                .requestMatchers(
                        "/api/inventory-lots",
                        "/api/inventory-lots/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "INVENTARIO"
                )

                // Certificaciones: consulta
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

                // Certificaciones: escritura
                .requestMatchers(
                        "/api/certifications",
                        "/api/certifications/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR"
                )

                // Clientes: consulta
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

                // Clientes: escritura
                .requestMatchers(
                        "/api/clients",
                        "/api/clients/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "VENTAS"
                )

                // Órdenes e ítems de exportación: consulta
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

                // Órdenes e ítems de exportación: escritura
                .requestMatchers(
                        "/api/export-orders",
                        "/api/export-orders/**",
                        "/api/export-order-items",
                        "/api/export-order-items/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR"
                )

                // Documentos de exportación: consulta
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/export-documents",
                        "/api/export-documents/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR",
                        "GERENCIA"
                )

                // Documentos de exportación: escritura
                .requestMatchers(
                        "/api/export-documents",
                        "/api/export-documents/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR"
                )

                // Envíos: consulta
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/shipments",
                        "/api/shipments/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR",
                        "GERENCIA"
                )

                // Envíos: escritura
                .requestMatchers(
                        "/api/shipments",
                        "/api/shipments/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "EXPORTADOR"
                )

                // Trazabilidad: consulta
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/reports/traceability/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "GERENCIA",
                        "EXPORTADOR"
                )

                // Otros reportes
                .requestMatchers(
                        "/api/reports/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "GERENCIA"
                )

                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )

            // Desactivar autenticación HTTP por formulario
            .formLogin(form -> form.disable())

            // Desactivar autenticación HTTP Basic
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}