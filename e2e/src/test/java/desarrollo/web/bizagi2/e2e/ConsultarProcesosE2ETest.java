package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

@DisplayName("E2E HU-07: Consultar procesos")
class ConsultarProcesosE2ETest extends BaseE2ETest {

    private static final By BUSCAR_NOMBRE = By.cssSelector("form.filtros input[type='search']");
    private static final By FILTRO_ESTADO = By.cssSelector("form.filtros select");
    private static final By FILTRO_CATEGORIA = By.cssSelector("form.filtros input[type='text']");

    @Test
    @DisplayName("La lista se puede buscar por nombre y filtrar por categoria y por estado")
    void buscarPorNombre_yFiltrarPorCategoriaYEstado() {
        registrarEmpresa();
        String id = identificadorUnico();
        String ventas = "Cotizacion " + id;
        String compras = "Orden de compra " + id;
        crearProceso(ventas, "Ventas");
        crearProceso(compras, "Compras");

        // Busqueda por nombre
        escribir(BUSCAR_NOMBRE, ventas);
        esperarQueDesaparezca(filaProceso(compras));
        assertTrue(visible(filaProceso(ventas)).isDisplayed());
        vaciar(BUSCAR_NOMBRE);
        visible(filaProceso(compras));

        // Filtro por categoria
        escribir(FILTRO_CATEGORIA, "Compras");
        esperarQueDesaparezca(filaProceso(ventas));
        assertTrue(visible(filaProceso(compras)).isDisplayed());
        vaciar(FILTRO_CATEGORIA);
        visible(filaProceso(ventas));

        // Filtro por estado: los dos son borradores, asi que no hay publicados
        seleccionar(FILTRO_ESTADO, "PUBLICADO");
        esperarTexto(By.cssSelector("td.vacio"), "No hay procesos que coincidan.");
        seleccionar(FILTRO_ESTADO, "BORRADOR");
        visible(filaProceso(ventas));
        visible(filaProceso(compras));
    }

    @Test
    @DisplayName("Desde la lista se abre el diagrama del proceso y se consulta su historial de cambios")
    void abrirDiagrama_yConsultarHistorial() {
        registrarEmpresa();
        String nombre = "Proceso a consultar " + identificadorUnico();
        crearProceso(nombre, "Ventas");

        clic(accionProceso(nombre, "Diagrama"));
        espera.until(ExpectedConditions.urlContains("/editor"));
        // El titulo dice solo "Diagrama" hasta que el editor termina de cargar el proceso
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".editor-page h1"), nombre));

        abrir("/procesos");
        esperarPantallaProcesos();
        abrirHistorial(nombre);
        assertTrue(visible(entradaHistorial("CREAR")).getText().contains("Admin E2E"));
    }

    @Test
    @DisplayName("Solo se muestran los procesos de la empresa del usuario autenticado")
    void otraEmpresa_noVeLosProcesosAjenos() {
        registrarEmpresa();
        String nombre = "Proceso privado " + identificadorUnico();
        crearProceso(nombre, "Ventas");

        cerrarSesion();
        registrarEmpresa();

        esperarTexto(By.cssSelector("td.vacio"), "No hay procesos que coincidan.");
        assertFalse(existe(filaProceso(nombre)));
    }
}
