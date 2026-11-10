package com.ledgerline.backend.config;

import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.entity.UserStatus;
import com.ledgerline.backend.repository.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UserRepository users) throws Exception {
        http
            .csrf(csrf -> csrf.disable())            // stateless JWT API, no cookies
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/auditor/**").hasAnyRole("AUDITOR", "ADMIN")
                .requestMatchers("/api/auditor/**").denyAll()
                .requestMatchers("/api/onboarding/status").hasAnyRole("APPLICANT", "ACCOUNT_HOLDER")
                .requestMatchers("/api/onboarding/**").hasRole("ACCOUNT_HOLDER")
                .requestMatchers("/api/accounts/**", "/api/transactions/**").hasRole("ACCOUNT_HOLDER")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter(users))));
        return http.build();
    }

    /** Valid signature is not enough: the user must still exist and be enabled. The role is read from the DB. */
    private Converter<Jwt, AbstractAuthenticationToken> jwtConverter(UserRepository users) {
        return jwt -> {
            User user = users.findById(Long.valueOf(jwt.getSubject()))
                    .filter(User::isEnabled)
                    .orElseThrow(() -> new BadCredentialsException("Account disabled or removed"));
            // Users who are not approved yet only get the APPLICANT authority
            String role = user.getStatus() == UserStatus.ACTIVE ? user.getRole().name() : "APPLICANT";
            return new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)), jwt.getSubject());
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecretKey jwtKey(@Value("${app.jwt-secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("#{'${app.cors-origins}'.split(',')}") List<String> origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins.stream().map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}