package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@DisplayName("E2E HU-04: Crear proceso")
class ProcesoE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Crear un proceso lo muestra en la lista como borrador")
    void crearProceso_apareceEnLaListaComoBorrador() {
        registrarEmpresa();
        String nombre = "Solicitud de vacaciones " + identificadorUnico();

        clic(By.linkText("Nuevo proceso"));
        escribir(By.id("nombre"), nombre);
        escribir(By.id("descripcion"), "Proceso creado por una prueba E2E");
        escribir(By.id("categoria"), "Recursos humanos");
        clic(BOTON_ENVIAR);

        esperarPantallaProcesos();
        // La fila del proceso recien creado: nombre, categoria y estado inicial
        WebElement fila = visible(By.xpath("//tr[td/strong[normalize-space()='" + nombre + "']]"));
        assertTrue(fila.getText().contains("Recursos humanos"));
        assertEquals("Borrador", fila.findElement(By.cssSelector("span.badge")).getText().trim());
    }

    @Test
    @DisplayName("Sin nombre no se guarda el proceso y se muestra el error del campo")
    void crearProceso_sinNombre_muestraError() {
        registrarEmpresa();

        clic(By.linkText("Nuevo proceso"));
        visible(By.id("nombre"));
        clic(BOTON_ENVIAR);

        assertTrue(visible(By.cssSelector("span.field-error")).getText().contains("El nombre es obligatorio"));
        assertTrue(estaEn("/procesos/nuevo"));
    }
}
