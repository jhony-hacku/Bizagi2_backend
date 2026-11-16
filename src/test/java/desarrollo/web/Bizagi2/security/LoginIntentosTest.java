package desarrollo.web.Bizagi2.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginIntentos - limite de intentos de login por cuenta y por IP")
class LoginIntentosTest {

    private final LoginIntentos intentos = new LoginIntentos();

    @Test
    @DisplayName("Permite hasta 10 intentos por cuenta y bloquea el siguiente")
    void limitePorCuenta() {
        for (int i = 0; i < 10; i++) {
            assertThat(intentos.permitir("10.0.0." + i, "ana@acme.com")).isTrue();
        }

        assertThat(intentos.permitir("10.0.0.99", "ana@acme.com")).isFalse();
    }

    @Test
    @DisplayName("La cuenta se identifica sin importar mayusculas ni espacios")
    void cuentaNormalizada() {
        for (int i = 0; i < 10; i++) {
            assertThat(intentos.permitir("10.0.0." + i, "ana@acme.com")).isTrue();
        }

        assertThat(intentos.permitir("10.0.0.99", "  ANA@Acme.com ")).isFalse();
    }

    @Test
    @DisplayName("Permite hasta 100 intentos por IP y bloquea el siguiente")
    void limitePorIp() {
        for (int i = 0; i < 100; i++) {
            assertThat(intentos.permitir("10.0.0.1", "usuario" + i + "@acme.com")).isTrue();
        }

        assertThat(intentos.permitir("10.0.0.1", "otro@acme.com")).isFalse();
    }

    @Test
    @DisplayName("El bloqueo de una cuenta no afecta a las demas")
    void cuentasIndependientes() {
        for (int i = 0; i < 11; i++) {
            intentos.permitir("10.0.0." + i, "ana@acme.com");
        }

        assertThat(intentos.permitir("10.0.1.1", "luis@acme.com")).isTrue();
    }

    @Test
    @DisplayName("Rechaza emails de mas de 254 caracteres sin registrarlos")
    void emailDemasiadoLargo() {
        String email = "a".repeat(250) + "@acme.com";

        assertThat(intentos.permitir("10.0.0.1", email)).isFalse();
    }

    @Test
    @DisplayName("Acepta un email nulo (la validacion del campo ocurre despues)")
    void emailNulo() {
        assertThat(intentos.permitir("10.0.0.1", null)).isTrue();
    }
}
