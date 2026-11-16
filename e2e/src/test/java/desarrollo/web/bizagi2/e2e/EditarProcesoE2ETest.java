package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-05: Editar proceso")
class EditarProcesoE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Editar nombre, categoria y estado se refleja en la lista y queda en el historial con su autor")
    void editarProceso_actualizaListaEHistorial() {
        registrarEmpresa();
        String original = "Proceso original " + identificadorUnico();
        String editado = "Proceso editado " + identificadorUnico();
        crearProceso(original, "Ventas");

        clic(accionProceso(original, "Editar"));
        esperarValor(By.id("nombre"), original);
        escribir(By.id("nombre"), editado);
        escribir(By.id("descripcion"), "Descripcion actualizada por una prueba E2E");
        escribir(By.id("categoria"), "Compras");
        seleccionar(By.id("estado"), "PUBLICADO");
        clic(BOTON_ENVIAR);

        esperarPantallaProcesos();
        assertTrue(visible(filaProceso(editado)).getText().contains("Compras"));
        esperarTexto(estadoProceso(editado), "Publicado");
        assertFalse(existe(filaProceso(original)));

        // Cada cambio queda en el historial, con el usuario que lo hizo
        abrirHistorial(editado);
        assertTrue(visible(entradaHistorial("EDITAR")).getText().contains("Admin E2E"));
    }

    @Test
    @DisplayName("Al editar se validan los mismos campos obligatorios que al crear")
    void editarProceso_sinNombre_muestraError() {
        registrarEmpresa();
        String nombre = "Proceso a validar " + identificadorUnico();
        crearProceso(nombre, "Ventas");

        clic(accionProceso(nombre, "Editar"));
        esperarValor(By.id("nombre"), nombre);
        vaciar(By.id("nombre"));
        clic(BOTON_ENVIAR);

        assertTrue(visible(By.cssSelector("span.field-error")).getText().contains("El nombre es obligatorio"));
        assertTrue(estaEn("/editar"));
    }

    @Test
    @DisplayName("Un usuario de solo lectura ve el proceso pero no las opciones de crear ni editar")
    void lector_veElProcesoSinOpcionDeEdicion() {
        registrarEmpresa();
        String nombre = "Proceso visible " + identificadorUnico();
        crearProceso(nombre, "Ventas");
        Invitado lector = invitarUsuario("LECTOR");

        cerrarSesion();
        iniciarSesion(lector.email(), lector.password());
        esperarPantallaProcesos();

        assertTrue(visible(filaProceso(nombre)).isDisplayed());
        assertTrue(existe(accionProceso(nombre, "Diagrama")));
        assertFalse(existe(accionProceso(nombre, "Editar")));
        assertFalse(existe(accionProceso(nombre, "Eliminar")));
        assertFalse(existe(By.linkText("Nuevo proceso")));
    }
}
