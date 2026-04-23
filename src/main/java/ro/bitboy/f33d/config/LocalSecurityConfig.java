package ro.bitboy.f33d.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.bitboy.f33d.service.AuditService;
import ro.bitboy.f33d.service.TokenService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@Profile("!keycloak")
public class LocalSecurityConfig {

    @Value("${f33d.admin-user:#{null}}")
    private String adminUser;

    @Value("${f33d.admin-password:#{null}}")
    private String adminPassword;

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, TokenService tokenService, AuditService auditService) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/stream", "/api/health").permitAll()
                .requestMatchers("/api/message").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new TokenAuthFilter(tokenService, auditService), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            ));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String user = resolveAdminUser();
        String pass = resolveAdminPassword();

        System.out.println("┌─ f33d  Web UI login ──────────────────────────┐");
        System.out.printf( "│  username   %-33s│%n", user);
        System.out.printf( "│  password   %-33s│%n", pass);
        System.out.println("└───────────────────────────────────────────────┘");

        return new InMemoryUserDetailsManager(
            User.withDefaultPasswordEncoder()
                .username(user)
                .password(pass)
                .roles("ADMIN")
                .build()
        );
    }

    private String resolveAdminUser() {
        return (adminUser != null && !adminUser.isBlank()) ? adminUser : "admin";
    }

    private String resolveAdminPassword() {
        return (adminPassword != null && !adminPassword.isBlank())
            ? adminPassword
            : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    static class TokenAuthFilter extends OncePerRequestFilter {

        private final TokenService tokenService;
        private final AuditService auditService;

        TokenAuthFilter(TokenService tokenService, AuditService auditService) {
            this.tokenService = tokenService;
            this.auditService = auditService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {

            String token = extractToken(req);
            if (token != null) {
                var resolved = tokenService.resolveName(token);
                if (resolved.isPresent()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        resolved.get(), null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    auditService.recordFailedAttempt(clientIp(req), req.getRequestURI(), req.getHeader("User-Agent"));
                }
            }
            chain.doFilter(req, res);
        }

        private static String clientIp(HttpServletRequest req) {
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }

        private String extractToken(HttpServletRequest req) {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
            return req.getHeader("X-Token");
        }
    }
}
