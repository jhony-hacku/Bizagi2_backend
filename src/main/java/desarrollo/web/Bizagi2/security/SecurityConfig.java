package desarrollo.web.Bizagi2.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Publico: registro, login, documentacion y consola H2 (perfil dev)
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()
                        // Cerrar sesion: cualquier usuario autenticado, sea cual sea su rol
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        // Cualquier usuario autenticado puede ver su propio perfil
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        // Solo el administrador gestiona colaboradores y los datos de la empresa
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/empresa").hasRole("ADMINISTRADOR")
                        // HU-17: solo el administrador crea roles de proceso. HU-23: solo el administrador comparte.
                        .requestMatchers(HttpMethod.POST, "/api/roles-proceso").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/procesos/*/compartir").hasRole("ADMINISTRADOR")
                        // HU-24: pools y lanes los puede eliminar el editor si la empresa lo permite (lo valida el servicio)
                        .requestMatchers(HttpMethod.DELETE, "/api/pools/**", "/api/lanes/**")
                        .hasAnyRole("ADMINISTRADOR", "EDITOR")
                        // Eliminar lo demas: solo el administrador (regla comun de las historias de usuario)
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMINISTRADOR")
                        // Lectura: administrador, editor y lector
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // Escritura: solo administrador y editor (el lector es de solo lectura)
                        .requestMatchers("/api/**").hasAnyRole("ADMINISTRADOR", "EDITOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
