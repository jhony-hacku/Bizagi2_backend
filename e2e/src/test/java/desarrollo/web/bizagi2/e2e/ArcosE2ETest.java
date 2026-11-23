package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-11 a HU-13: Arcos (flujos de secuencia)")
class ArcosE2ETest extends BaseEditorE2ETest {

    private String proceso;
    private String pool;

    // Empresa con un rol, un proceso con una lane y tres actividades
    private void prepararDiagrama() {
        registrarEmpresa();
        crearRol("Analista", "Revisa");
        proceso = "Proceso arcos " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        pool = poolEmpresa();
        agregarLane(pool, "Analista", null);
        crearActividad("Analista", "Recibir", "TAREA");
        crearActividad("Analista", "Revisar", "TAREA");
        crearActividad("Analista", "Archivar", "TAREA");
    }

    @Test
    @DisplayName("HU-11: se crea un arco entre dos elementos y se rechazan el arco a si mismo, el repetido y el que cruza de pool")
    void crearArco_conSusValidaciones() {
        prepararDiagrama();

        crearArco("Recibir", "Revisar");
        esperarConexionesEnLienzo("Arco_", 1);

        // Un elemento consigo mismo
        abrirFormularioArco();
        llenarArco("Recibir", "Recibir", null, null);
        guardarFormularioPanel();
        assertEquals("Un arco no puede unir un elemento consigo mismo", errorDelPanel());

        // El mismo arco dos veces
        llenarArco("Recibir", "Revisar", null, null);
        guardarFormularioPanel();
        espera.until(d -> errorDelPanel().equals("Ya existe un arco entre esos dos elementos"));
        clic(botonPanel("Cancelar"));

        // Otro pool de la empresa: sus elementos no se ofrecen como destino de un arco del primero
        // (entre pools se comunica con mensajes, HU-25)
        crearRol("Bodeguero", "Despacha");
        reabrirEditor(proceso);
        crearPool("Bodega", "EMPRESA_PROPIETARIA");
        agregarLane("Bodega", "Bodeguero", null);
        crearActividad("Bodega", "Bodeguero", "Despachar", "TAREA");
        abrirFormularioArco();
        seleccionarTexto(By.id("arco-pool"), pool);
        List<String> destinos = opciones(By.id("arco-destino"));
        assertTrue(destinos.contains("Revisar (Actividad)"), destinos.toString());
        assertFalse(destinos.contains("Despachar (Actividad)"), destinos.toString());

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Arco creado de 'Recibir' a 'Revisar'"));
    }

    @Test
    @DisplayName("HU-12: se cambian destino, etiqueta y condicion de un arco con las mismas validaciones que al crearlo")
    void editarArco() {
        prepararDiagrama();
        crearArco("Recibir", "Revisar");

        irAPestana("Conexiones");
        clic(accionItem(tituloArco("Recibir", "Revisar"), "Editar"));
        // Las validaciones de la creacion tambien aplican al editar
        seleccionarNodo(By.id("arco-destino"), "Recibir");
        guardarFormularioPanel();
        assertEquals("Un arco no puede unir un elemento consigo mismo", errorDelPanel());

        seleccionarNodo(By.id("arco-destino"), "Archivar");
        escribir(By.id("arco-etiqueta"), "directo");
        guardarFormularioPanel();
        esperarTexto(subtituloItem(tituloArco("Recibir", "Archivar")), "directo");
        assertFalse(existe(itemPanel(tituloArco("Recibir", "Revisar"))));
        esperarConexionesEnLienzo("Arco_", 1);

        // La condicion solo se pide cuando el arco sale de un gateway exclusivo o inclusivo
        crearGateway("Decidir", "EXCLUSIVA");
        crearArco("Decidir", "Revisar", null, "monto > 1000");
        irAPestana("Conexiones");
        clic(accionItem(tituloArco("Decidir", "Revisar"), "Editar"));
        esperarValor(By.id("arco-condicion"), "monto > 1000");
        vaciar(By.id("arco-condicion"));
        escribir(By.id("arco-condicion"), "monto > 5000");
        guardarFormularioPanel();
        esperarTexto(subtituloItem(tituloArco("Decidir", "Revisar")), "monto > 5000");

        verHistorialProceso(proceso);
        visible(entradaHistorial("EDITAR", "Arco modificado: ahora va de 'Recibir' a 'Archivar'"));
    }

    @Test
    @DisplayName("HU-13: el administrador elimina un arco con confirmacion y el editor avisa del elemento que queda sin entrada")
    void eliminarArco() {
        prepararDiagrama();
        crearArco("Recibir", "Revisar");
        esperarConexionesEnLienzo("Arco_", 1);

        irAPestana("Conexiones");
        clic(accionItem(tituloArco("Recibir", "Revisar"), "Eliminar"));
        assertEquals("Eliminar arco", visible(By.id("confirmacion-titulo")).getText().trim());
        confirmarDialogo();
        String aviso = esperarNotificacion("Elemento eliminado.");
        assertTrue(aviso.contains("'Revisar' no tiene camino de entrada"), aviso);
        esperarQueDesaparezca(itemPanel(tituloArco("Recibir", "Revisar")));
        esperarConexionesEnLienzo("Arco_", 0);

        verHistorialProceso(proceso);
        visible(entradaHistorial("ELIMINAR", "Arco eliminado de 'Recibir' a 'Revisar'"));

        // Solo el administrador elimina arcos: un editor puede editarlos pero no eliminarlos
        volverAProcesos();
        abrirEditor(proceso);
        crearArco("Revisar", "Archivar");
        volverDelEditor();
        Invitado editor = invitarUsuario("EDITOR");
        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        esperarPantallaProcesos();
        abrirEditor(proceso);
        irAPestana("Conexiones");
        assertTrue(visible(accionItem(tituloArco("Revisar", "Archivar"), "Editar")).isDisplayed());
        assertFalse(existe(accionItem(tituloArco("Revisar", "Archivar"), "Eliminar")));
    }

    private void volverDelEditor() {
        abrir("/procesos");
        esperarPantallaProcesos();
    }
}
