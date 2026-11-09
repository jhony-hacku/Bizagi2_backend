package desarrollo.web.Bizagi2.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Cookie que transporta el refresh token. El navegador la guarda y la envia solo a /api/auth:
//  - HttpOnly: JavaScript no puede leerla, asi un XSS no se lleva el token largo.
//  - Secure:   solo viaja por HTTPS (se desactiva unicamente en desarrollo local con http).
//  - SameSite=Strict: no se envia en peticiones originadas desde otros sitios (defensa CSRF).
@Component
public class RefreshCookie {

    public static final String NOMBRE = "refresh_token";
    private static final String RUTA = "/api/auth";

    private final boolean secure;
    private final Duration duracion;

    public RefreshCookie(
            @Value("${jwt.refresh-cookie-secure:true}") boolean secure,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.secure = secure;
        this.duracion = Duration.ofMillis(refreshExpirationMs);
    }

    public ResponseCookie crear(String refreshToken) {
        return base(refreshToken).maxAge(duracion).build();
    }

    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(RUTA);
    }
}
