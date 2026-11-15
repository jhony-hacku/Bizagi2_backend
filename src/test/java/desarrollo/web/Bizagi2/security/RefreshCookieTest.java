package desarrollo.web.Bizagi2.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshCookieTest {

    @Test
    @DisplayName("La cookie del refresh token es HttpOnly, Secure, SameSite=Strict y limitada a /api/auth")
    void crear_aplicaAtributosDeSeguridad() {
        ResponseCookie cookie = new RefreshCookie(true, 604_800_000L).crear("abc", java.time.Instant.now().plusSeconds(604800));

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("abc");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge().toSeconds()).isBetween(604_798L, 604_800L);
    }

    @Test
    @DisplayName("Borrar la cookie la deja vacia y con duracion cero, con los mismos atributos")
    void borrar_vaciaLaCookie() {
        ResponseCookie cookie = new RefreshCookie(false, 604_800_000L).borrar();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
    }
}
