package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-01: Registro de empresa")
class RegistroEmpresaE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Registrar una empresa deja al administrador dentro, en la lista de procesos")
    void registrarEmpresa_entraALaListaDeProcesos() {
        registrarEmpresa();

        assertTrue(estaEn("/procesos"));
        // Solo quien puede escribir (el administrador inicial) ve el boton de crear
        assertTrue(visible(By.linkText("Nuevo proceso")).isDisplayed());
        // Una empresa nueva no tiene procesos todavia
        esperarTexto(By.cssSelector("td.vacio"), "No hay procesos que coincidan.");
    }

    @Test
    @DisplayName("Si las contrasenas no coinciden se muestra el error y no se registra")
    void registrarEmpresa_contrasenasDistintas_muestraError() {
        String id = identificadorUnico();

        abrir("/register-company");
        escribir(By.id("nombreEmpresa"), "Empresa E2E " + id);
        escribir(By.id("nit"), "E2E-" + id);
        escribir(By.id("emailContacto"), "contacto." + id + "@example.com");
        escribir(By.id("nombre"), "Admin E2E");
        escribir(By.id("email"), "admin." + id + "@example.com");
        escribir(By.id("password"), PASSWORD);
        escribir(By.id("confirmPassword"), PASSWORD + "-otra");
        clic(BOTON_ENVIAR);

        assertEquals("Las contraseñas no coinciden.", visible(By.cssSelector("span.field-error")).getText().trim());
        assertTrue(estaEn("/register-company"));
    }
}
