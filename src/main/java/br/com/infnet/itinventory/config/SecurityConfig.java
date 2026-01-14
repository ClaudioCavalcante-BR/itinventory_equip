package br.com.infnet.itinventory.config;

import br.com.infnet.itinventory.exception.RestAccessDeniedHandler;
import br.com.infnet.itinventory.exception.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Mantém: usa o CorsConfigurationSource(@Bean) abaixo
                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // IMPORTANTE: liberar página/dispatcher de erro
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()

                        // Preflight CORS (OPTIONS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Login público
                        .requestMatchers(POST, "/api/usuarios/login").permitAll()

                        // Actuator público
                        .requestMatchers("/actuator/**").permitAll()

                        // my-profile: qualquer autenticado
                        .requestMatchers(GET, "/api/usuarios/my-profile").authenticated()

                        // USUÁRIOS (CRIAR) - somente ADMIN
                        .requestMatchers(POST, "/api/usuarios").hasRole("ADMIN")

                        // EQUIPMENTS
                        .requestMatchers(DELETE, "/api/equipments/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/equipments/**").hasAnyRole("ADMIN", "GESTOR_TI", "ANALISTA_TI")
                        .requestMatchers(PUT, "/api/equipments/**").hasAnyRole("ADMIN", "GESTOR_TI")
                        .requestMatchers(GET, "/api/equipments/**").hasAnyRole("ADMIN", "GESTOR_TI", "USUARIO", "ANALISTA_TI")

                        // USUÁRIOS (LISTAR/DETALHAR) - somente ADMIN
                        .requestMatchers(GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(PUT, "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(DELETE, "/api/usuarios/**").hasRole("ADMIN")

                        // USUÁRIOS (ATIVAR/INATIVAR) - somente ADMIN
                        .requestMatchers(PATCH, "/api/usuarios/*/ativar").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/usuarios/*/inativar").hasRole("ADMIN")

                        // PROFILES - somente ADMIN (para alimentar o select)
                        .requestMatchers(GET, "/api/profiles/**").hasRole("ADMIN")

                        // USUÁRIOS (Exportar CSV) - somente ADMIN
                        .requestMatchers(GET, "/api/usuarios/export").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // Padronização 401/403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Filtro JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://claudio-itinventory-front.vercel.app",
                "https://*.vercel.app"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // ✅ Ajuste mínimo e mais robusto para não falhar preflight por header inesperado
        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        // JWT via Authorization header (sem cookies)
        config.setAllowCredentials(false);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}