package desarrollo.web.Bizagi2.controller;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.dto.ErrorResponse;
import desarrollo.web.Bizagi2.dto.auth.AuthResponse;
import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.security.RefreshCookie;
import desarrollo.web.Bizagi2.security.JwtService;
import desarrollo.web.Bizagi2.security.LoginIntentos;
import jakarta.servlet.http.HttpServletRequest;
import desarrollo.web.Bizagi2.service.AuthService;
import desarrollo.web.Bizagi2.service.AuthService.Sesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookie refreshCookie;
    private final JwtService jwtService;
    private final LoginIntentos loginIntentos;

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken(); // Materializa la cookie XSRF-TOKEN que Angular envia en X-XSRF-TOKEN.
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @Operation(
            summary = "Registrar una empresa y su creador (devuelve access token y deja el refresh token en una cookie HttpOnly)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = """
                            {
                              "nombreEmpresa": "Acme SAS",
                              "nit": "900123456",
                              "emailContacto": "contacto@acme.com",
                              "nombre": "Ana Perez",
                              "email": "ana@acme.com",
                              "password": "Clave12345"
                            }"""))))
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        Sesion sesion = authService.registrar(datos);
        return conSesion(HttpStatus.CREATED, sesion).body(sesion.cuerpo());
    }

    @Operation(
            summary = "Iniciar sesion: access token (corto) en el JSON y refresh token (largo) en cookie HttpOnly",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = """
                            {
                              "email": "ana@acme.com",
                              "password": "Clave12345"
                            }"""))))
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest datos, HttpServletRequest request) {
        if (!loginIntentos.permitir(request.getRemoteAddr(), datos.getEmail())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, "300")
                    .body(error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Intenta de nuevo mas tarde"));
        }
        Sesion sesion = authService.login(datos);
        return conSesion(HttpStatus.OK, sesion).body(sesion.cuerpo());
    }

    @Operation(summary = "Renovar la sesion: usa la cookie del refresh token y entrega un access token nuevo y un refresh nuevo (rotacion)")
    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(
            @CookieValue(name = RefreshCookie.NOMBRE, required = false) String refreshToken) {
        try {
            Sesion sesion = authService.refrescar(refreshToken);
            return conSesion(HttpStatus.OK, sesion).body(sesion.cuerpo());
        } catch (SesionInvalidaException ex) {
            // Sesion perdida: se borra la cookie para que el navegador no siga enviando un token muerto
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.borrar().toString())
                    .body(error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
        }
    }

    @Operation(summary = "Cerrar sesion: el access token y los refresh tokens del usuario dejan de ser validos")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Usuario usuario,
            @CookieValue(name = RefreshCookie.NOMBRE, required = false) String refreshToken) {
        authService.logout(usuario, refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.borrar().toString())
                .build();
    }

    // Cabeceras comunes de una sesion nueva. El refresh token nunca va en el cuerpo: solo en la cookie HttpOnly
    private ResponseEntity.BodyBuilder conSesion(HttpStatus status, Sesion sesion) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.crear(sesion.refreshToken(),
                        jwtService.leerRefreshClaims(sesion.refreshToken()).getExpiration().toInstant()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    // Mismo cuerpo de error que usa GlobalExceptionHandler en el resto de la API
    private ErrorResponse error(HttpStatus status, String mensaje) {
        return new ErrorResponse(status.value(), mensaje, Instant.now().toString(), null);
    }
}
