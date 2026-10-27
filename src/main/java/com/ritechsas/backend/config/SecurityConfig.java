package com.ritechsas.backend.config;

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

import com.ritechsas.backend.service.DatabaseUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {

        http
            .authenticationProvider(authenticationProvider)
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(
                    new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth

                // Acceso público
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                // Administración de usuarios
                .requestMatchers("/api/users", "/api/users/**")
                    .hasRole("ADMINISTRADOR")

                // Productores: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/producers", "/api/producers/**")
                    .hasAnyRole(
                        "ADMINISTRADOR",
                        "COMPRAS",
                        "GERENCIA",
                        "INVENTARIO",
                        "VENTAS"
                    )

                // Productores: escritura
                .requestMatchers("/api/producers", "/api/producers/**")
                    .hasAnyRole("ADMINISTRADOR", "COMPRAS")

                // Compras: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/purchases", "/api/purchases/**")
                    .hasAnyRole("ADMINISTRADOR", "COMPRAS", "GERENCIA")

                // Compras: escritura
                .requestMatchers("/api/purchases", "/api/purchases/**")
                    .hasAnyRole("ADMINISTRADOR", "COMPRAS")

                // Productos: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/products", "/api/products/**")
                    .hasAnyRole(
                        "ADMINISTRADOR", "COMPRAS", "INVENTARIO",
                        "EXPORTADOR", "VENTAS", "GERENCIA")

                // Productos: escritura
                .requestMatchers("/api/products", "/api/products/**")
                    .hasAnyRole("ADMINISTRADOR", "INVENTARIO")

                // Inventario: consulta
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/inventory-lots",
                    "/api/inventory-lots/**")
                    .hasAnyRole(
                        "ADMINISTRADOR", "COMPRAS", "INVENTARIO",
                        "EXPORTADOR", "VENTAS", "GERENCIA")

                // Inventario: escritura
                .requestMatchers(
                    "/api/inventory-lots",
                    "/api/inventory-lots/**")
                    .hasAnyRole("ADMINISTRADOR", "INVENTARIO")

                // Certificaciones: consulta
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/certifications",
                    "/api/certifications/**")
                    .hasAnyRole(
                        "ADMINISTRADOR", "INVENTARIO",
                        "EXPORTADOR", "GERENCIA")

                // Certificaciones: escritura
                .requestMatchers(
                    "/api/certifications",
                    "/api/certifications/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR")

                // Clientes: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/clients", "/api/clients/**")
                    .hasAnyRole(
                        "ADMINISTRADOR", "EXPORTADOR", "VENTAS", "GERENCIA")

                // Clientes: escritura
                .requestMatchers("/api/clients", "/api/clients/**")
                    .hasAnyRole("ADMINISTRADOR", "VENTAS")

                // Órdenes e ítems de exportación: consulta
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/export-orders",
                    "/api/export-orders/**",
                    "/api/export-order-items",
                    "/api/export-order-items/**")
                    .hasAnyRole(
                        "ADMINISTRADOR", "INVENTARIO",
                        "EXPORTADOR", "VENTAS", "GERENCIA")

                // Órdenes e ítems de exportación: escritura
                .requestMatchers(
                    "/api/export-orders",
                    "/api/export-orders/**",
                    "/api/export-order-items",
                    "/api/export-order-items/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR")

                // Documentos de exportación: consulta
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/export-documents",
                    "/api/export-documents/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR", "GERENCIA")

                // Documentos de exportación: escritura
                .requestMatchers(
                    "/api/export-documents",
                    "/api/export-documents/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR")

                // Envíos: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/shipments", "/api/shipments/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR", "GERENCIA")

                // Envíos: escritura
                .requestMatchers("/api/shipments", "/api/shipments/**")
                    .hasAnyRole("ADMINISTRADOR", "EXPORTADOR")

                // Trazabilidad: consulta
                .requestMatchers(
                    HttpMethod.GET, "/api/reports/traceability/**")
                    .hasAnyRole("ADMINISTRADOR", "GERENCIA", "EXPORTADOR")

                // Otros reportes
                .requestMatchers("/api/reports/**")
                    .hasAnyRole("ADMINISTRADOR", "GERENCIA")

                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}