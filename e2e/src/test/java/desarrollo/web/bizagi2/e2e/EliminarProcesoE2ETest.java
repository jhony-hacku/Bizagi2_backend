package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-06: Eliminar proceso")
class EliminarProcesoE2ETest extends BaseE2ETest {

    private static final By VER_ELIMINADOS = By.cssSelector("form.filtros input[type='checkbox']");

    @Test
    @DisplayName("Se pide confirmacion antes de eliminar, y al cancelar el proceso se conserva")
    void eliminar_pideConfirmacion_yCancelarNoElimina() {
        registrarEmpresa();
        String nombre = "Proceso a conservar " + identificadorUnico();
        crearProceso(nombre, "Ventas");

        clic(accionProceso(nombre, "Eliminar"));
        assertEquals("Eliminar proceso", visible(By.id("confirmacion-titulo")).getText().trim());
        assertTrue(visible(By.id("confirmacion-mensaje")).getText().contains(nombre));
        cancelarDialogo();

        assertTrue(visible(filaProceso(nombre)).isDisplayed());
        esperarTexto(estadoProceso(nombre), "Borrador");
    }

    @Test
    @DisplayName("La eliminacion es logica: sale del listado, se consulta con el filtro y queda en el historial")
    void eliminar_esLogica_yQuedaEnElHistorial() {
        registrarEmpresa();
        String nombre = "Proceso a eliminar " + identificadorUnico();
        crearProceso(nombre, "Ventas");

        clic(accionProceso(nombre, "Eliminar"));
        visible(DIALOGO);
        confirmarDialogo();

        // Deja de aparecer en el listado por defecto
        esperarQueDesaparezca(filaProceso(nombre));
        esperarTexto(By.cssSelector("td.vacio"), "No hay procesos que coincidan.");

        // No se borro: con el filtro "Ver eliminados" se puede consultar, marcado como eliminado
        clic(VER_ELIMINADOS);
        esperarTexto(estadoProceso(nombre), "Eliminado");
        // Un proceso eliminado ya no se puede editar ni volver a eliminar
        assertFalse(existe(accionProceso(nombre, "Editar")));
        assertFalse(existe(accionProceso(nombre, "Eliminar")));

        // La eliminacion queda registrada en el historial
        abrirHistorial(nombre);
        assertTrue(visible(entradaHistorial("ELIMINAR")).getText().contains("Admin E2E"));
    }
}
