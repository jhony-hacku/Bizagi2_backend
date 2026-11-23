package desarrollo.web.Bizagi2.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] ROLES_ADMINISTRACION = {"CREADOR", "ADMINISTRADOR"};
    private static final String[] ROLES_ESCRITURA = {"CREADOR", "ADMINISTRADOR", "EDITOR"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${jwt.refresh-cookie-secure:true}")
    private boolean secureCookie;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Patron "double submit": Angular debe poder leer la cookie XSRF-TOKEN para reenviarla en la cabecera
        // X-XSRF-TOKEN, por eso no puede ser HttpOnly. No contiene credenciales (el refresh token si es HttpOnly).
        CookieCsrfTokenRepository csrfCookies = CookieCsrfTokenRepository.withHttpOnlyFalse(); // NOSONAR ver comentario
        csrfCookies.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Strict").secure(secureCookie));
        var csrfHeader = new CsrfTokenRequestAttributeHandler() {
            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
                return request.getHeader(token.getHeaderName());
            }
        };
        http
                // Solo /auth usa cookies para autenticar, y ahi el CSRF sigue activo. El resto de la API exige
                // la cabecera Authorization: Bearer, que el navegador nunca envia por su cuenta: no es vulnerable a CSRF.
                .csrf(csrf -> csrf.csrfTokenRepository(csrfCookies)
                        .csrfTokenRequestHandler(csrfHeader)
                        // Cada Bearer autentica una peticion, no abre una nueva sesion de navegador.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .requireCsrfProtectionMatcher(request -> request.getRequestURI().startsWith("/api/auth/") // NOSONAR ver comentario
                                && !java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Publico: registro, login, renovacion (la protege la cookie del refresh token),
                        // documentacion y consola H2 (perfil dev)
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/csrf",
                                "/api/auth/logout", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()
                        // Logout valida access o refresh en el servicio; permite borrar una cookie vencida.
                        // Cualquier usuario autenticado puede ver su propio perfil
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        // Solo el administrador gestiona colaboradores y los datos de la empresa
                        .requestMatchers("/api/usuarios/**").hasAnyRole(ROLES_ADMINISTRACION)
                        .requestMatchers(HttpMethod.PUT, "/api/empresa").hasAnyRole(ROLES_ADMINISTRACION)
                        // HU-17: solo el administrador crea roles de proceso. HU-23: solo el administrador comparte.
                        .requestMatchers(HttpMethod.POST, "/api/roles-proceso").hasAnyRole(ROLES_ADMINISTRACION)
                        .requestMatchers(HttpMethod.POST, "/api/procesos/*/compartir").hasAnyRole(ROLES_ADMINISTRACION)
                        // HU-24: pools y lanes los puede eliminar el editor si la empresa lo permite (lo valida el servicio)
                        .requestMatchers(HttpMethod.DELETE, "/api/pools/**", "/api/lanes/**")
                        .hasAnyRole(ROLES_ESCRITURA)
                        // Eliminar lo demas: solo el administrador (regla comun de las historias de usuario)
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole(ROLES_ADMINISTRACION)
                        // Lectura: administrador, editor y lector
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // Escritura: solo administrador y editor (el lector es de solo lectura)
                        .requestMatchers("/api/**").hasAnyRole(ROLES_ESCRITURA)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        var registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
