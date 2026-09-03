package com.foody.auth.config;

import com.foody.auth.security.FoodyUserDetailsService;
import com.foody.auth.security.JwtAuthenticationFilter;
import com.foody.auth.security.JwtService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtService jwtService;
    private final FoodyUserDetailsService userDetailsService;

    // @Value binds List<String> from a comma-separated property only via this SpEL split;
    // plain "${foody.cors.allowed-origins}" would bind as a single-element list instead.
    @Value("#{'${foody.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    public WebSecurityConfig(JwtService jwtService, FoodyUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    private static final String[] PUBLIC_MATCHERS = {
            "/api/auth/**",
            "/api/businesses",          // Phase 1: Discover listing (GET ?type=&search=), no login required
            "/api/businesses/*",        // Phase 0: read-only approved business lookup
            "/api/businesses/*/menus",  // Phase 0: browse a business's menus, no login required
            "/api/menus/*/products",    // Phase 0: browse a menu's products, no login required
            "/api/products/*",          // Phase 0: single product lookup, no login required
            "/api/businesses/*/reservation-availability", // Phase 1: browse reservation slots, no login required
            "/uploads/**",              // Phase 2: publicly-readable uploaded images (profile pics, covers) — the upload *action* itself (/api/uploads/image) still requires auth
            "/actuator/health",
            "/swagger-ui/**", "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Allows the frontend (a different origin in dev: Vite on :5173 vs the API on :8080) to
     * call the API with an Authorization header. Origins are configured, never wildcarded,
     * since credentials/auth headers are involved. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
