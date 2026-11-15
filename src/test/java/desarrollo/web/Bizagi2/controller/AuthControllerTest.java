package desarrollo.web.Bizagi2.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

import desarrollo.web.Bizagi2.dto.ErrorResponse;
import desarrollo.web.Bizagi2.dto.auth.AuthResponse;
import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.security.JwtService;
import desarrollo.web.Bizagi2.security.LoginIntentos;
import desarrollo.web.Bizagi2.security.RefreshCookie;
import desarrollo.web.Bizagi2.service.AuthService;
import desarrollo.web.Bizagi2.service.AuthService.Sesion;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - HU-01 registro y HU-03 inicio, renovacion y cierre de sesion")
class AuthControllerTest {

    private static final String REFRESH = "refresh.token.firmado";

    @Mock
    private AuthService authService;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginIntentos loginIntentos;
    @Mock
    private HttpServletRequest request;

    // Cookie real (no Secure, 7 dias): asi se comprueba la cabecera Set-Cookie completa
    private final RefreshCookie refreshCookie = new RefreshCookie(false, 604_800_000L);

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, refreshCookie, jwtService, loginIntentos);
    }

    // El controlador lee el vencimiento del refresh token para fijar la duracion de la cookie
    private void refreshVigente() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(jwtService.leerRefreshClaims(REFRESH)).thenReturn(claims);
    }

    private void assertCookieConRefresh(ResponseEntity<?> respuesta) {
        String cookie = respuesta.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith(RefreshCookie.NOMBRE + "=" + REFRESH).contains("HttpOnly").contains("SameSite=Strict");
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
    }

    private void assertCookieBorrada(ResponseEntity<?> respuesta) {
        String cookie = respuesta.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith(RefreshCookie.NOMBRE + "=;").contains("Max-Age=0");
    }

    @Test
    @DisplayName("GET /api/auth/csrf responde 204 y materializa el token CSRF")
    void csrf() {
        CsrfToken token = mock(CsrfToken.class);

        ResponseEntity<Void> respuesta = controller.csrf(token);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(token).getToken();
    }

    @Test
    @DisplayName("POST /api/auth/register responde 201 con el access token y deja el refresh en la cookie")
    void registrar() {
        RegistroRequest datos = new RegistroRequest();
        AuthResponse cuerpo = new AuthResponse();
        when(authService.registrar(datos)).thenReturn(new Sesion(cuerpo, REFRESH));
        refreshVigente();

        ResponseEntity<AuthResponse> respuesta = controller.registrar(datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(cuerpo);
        assertCookieConRefresh(respuesta);
    }

    @Test
    @DisplayName("POST /api/auth/login responde 200 con el access token y deja el refresh en la cookie")
    void login() {
        LoginRequest datos = new LoginRequest("ana@acme.com", "Clave12345");
        AuthResponse cuerpo = new AuthResponse();
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(loginIntentos.permitir("10.0.0.1", "ana@acme.com")).thenReturn(true);
        when(authService.login(datos)).thenReturn(new Sesion(cuerpo, REFRESH));
        refreshVigente();

        ResponseEntity<?> respuesta = controller.login(datos, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(cuerpo);
        assertCookieConRefresh(respuesta);
    }

    @Test
    @DisplayName("POST /api/auth/login responde 429 cuando se supera el limite de intentos")
    void loginLimitado() {
        LoginRequest datos = new LoginRequest("ana@acme.com", "Clave12345");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(loginIntentos.permitir("10.0.0.1", "ana@acme.com")).thenReturn(false);

        ResponseEntity<?> respuesta = controller.login(datos, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("300");
        assertThat(respuesta.getBody()).isInstanceOf(ErrorResponse.class);
        assertThat(((ErrorResponse) respuesta.getBody()).getStatus()).isEqualTo(429);
        verify(authService, never()).login(datos);
    }

    @Test
    @DisplayName("POST /api/auth/refresh responde 200 con un access nuevo y rota la cookie")
    void refresh() {
        AuthResponse cuerpo = new AuthResponse();
        when(authService.refrescar("refresh.anterior")).thenReturn(new Sesion(cuerpo, REFRESH));
        refreshVigente();

        ResponseEntity<?> respuesta = controller.refresh("refresh.anterior");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(cuerpo);
        assertCookieConRefresh(respuesta);
    }

    @Test
    @DisplayName("POST /api/auth/refresh con una sesion invalida responde 401 y borra la cookie")
    void refreshInvalido() {
        when(authService.refrescar("refresh.usado")).thenThrow(new SesionInvalidaException("Sesion invalida"));

        ResponseEntity<?> respuesta = controller.refresh("refresh.usado");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).isInstanceOf(ErrorResponse.class);
        assertThat(((ErrorResponse) respuesta.getBody()).getMessage()).isEqualTo("Sesion invalida");
        assertCookieBorrada(respuesta);
    }

    @Test
    @DisplayName("POST /api/auth/logout responde 204, invalida la sesion y borra la cookie")
    void logout() {
        Usuario usuario = UsuarioDePrueba.administrador();

        ResponseEntity<Void> respuesta = controller.logout(usuario, REFRESH);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout(usuario, REFRESH);
        assertCookieBorrada(respuesta);
    }
}
