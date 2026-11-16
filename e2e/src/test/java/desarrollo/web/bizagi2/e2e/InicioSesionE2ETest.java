package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E HU-03: Inicio y cierre de sesion")
class InicioSesionE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Cerrar sesion, volver a entrar y recargar la pagina conserva la sesion")
    void cerrarSesion_iniciarSesion_yRecargar() {
        Cuenta cuenta = registrarEmpresa();

        cerrarSesion();
        assertTrue(estaEn("/login"));

        iniciarSesion(cuenta.email(), cuenta.password());
        esperarPantallaProcesos();

        // El access token vive solo en memoria: al recargar, la sesion se recupera con la cookie del refresh token
        driver.navigate().refresh();
        esperarPantallaProcesos();
        assertTrue(estaEn("/procesos"));
    }

    @Test
    @DisplayName("Con una contrasena incorrecta se muestra un mensaje generico y no se entra")
    void iniciarSesion_contrasenaIncorrecta_muestraError() {
        Cuenta cuenta = registrarEmpresa();
        cerrarSesion();

        iniciarSesion(cuenta.email(), "contrasena-equivocada");

        // HU-03: el mensaje no revela si el correo existe
        assertEquals("Correo o contraseña incorrectos.", visible(ERROR_FORMULARIO).getText().trim());
        assertTrue(estaEn("/login"));
    }
}
