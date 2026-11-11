package vn.newmen.store.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.newmen.store.repository.AccountRepository;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmailIgnoreCase(email)
            .filter(account -> account.active)
            .map(account -> User.withUsername(account.email)
                .password(account.passwordHash)
                .roles(account.role)
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowed = Arrays.stream(origins.split(","))
            .map(String::trim).filter(value -> !value.isBlank()).toList();
        configuration.setAllowedOrigins(allowed);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityContextRepository contextRepository,
                                            CsrfTokenRepository csrfTokenRepository,
                                            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> {
                csrf.csrfTokenRepository(csrfTokenRepository).spa();
                if (h2ConsoleEnabled) csrf.ignoringRequestMatchers("/h2-console/**");
            })
            .headers(headers -> {
                if (h2ConsoleEnabled) headers.frameOptions(frame -> frame.sameOrigin());
            })
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/health/**", "/api/v1/auth/csrf", "/api/v1/auth/register",
                    "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/me",
                    "/api/v1/catalog/**", "/api/v1/promotions/active", "/api/v1/media/**",
                    "/media/**",
                    "/api/v1/cart/**", "/api/v1/checkout").permitAll();
                if (h2ConsoleEnabled) auth.requestMatchers("/h2-console/**").permitAll();
                auth.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers("/api/v1/auth/**", "/api/v1/profile/**", "/api/v1/orders/**").authenticated()
                    .anyRequest().denyAll();
            })
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}}");
                }));
        return http.build();
    }
}
