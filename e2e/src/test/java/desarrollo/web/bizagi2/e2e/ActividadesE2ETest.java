package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-08 a HU-10: Actividades del diagrama")
class ActividadesE2ETest extends BaseEditorE2ETest {

    private String proceso;
    private String pool;

    // Empresa con dos roles de proceso, un proceso y una lane por rol en el pool de la empresa
    private void prepararDiagrama() {
        registrarEmpresa();
        crearRol("Analista", "Revisa");
        crearRol("Supervisor", "Aprueba");
        proceso = "Proceso actividades " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        pool = poolEmpresa();
        agregarLane(pool, "Analista", null);
        agregarLane(pool, "Supervisor", null);
    }

    @Test
    @DisplayName("HU-08: se crea una actividad con nombre, tipo y lane; se dibuja, el nombre es unico y queda en el historial")
    void crearActividad() {
        prepararDiagrama();

        crearActividad("Analista", "Revisar", "USUARIO");
        esperarNotificacion("Elemento creado.");
        esperarTexto(subtituloItem("Revisar"), "Tarea de usuario · Analista");
        visible(textoEnLienzo("Revisar"));

        // Nombre obligatorio (validacion del formulario)...
        abrirFormularioNodo("+ Actividad");
        guardarFormularioPanel();
        visible(By.xpath("//span[contains(@class,'field-error') and normalize-space()='El nombre es obligatorio.']"));

        // ...y unico dentro del proceso (regla del backend)
        escribir(By.id("act-nombre"), "Revisar");
        guardarFormularioPanel();
        assertEquals("Ya existe una actividad con ese nombre en el proceso", errorDelPanel());

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Actividad 'Revisar' (USUARIO) creada en la lane 'Analista'"));
    }

    @Test
    @DisplayName("HU-09: se cambian nombre, tipo y lane de una actividad, sus arcos siguen conectados y un lector no puede editar")
    void editarActividad_conservaArcos() {
        prepararDiagrama();
        crearActividad("Analista", "Revisar", "TAREA");
        crearActividad("Supervisor", "Aprobar", "TAREA");
        crearArco("Revisar", "Aprobar");

        editarNodo("Revisar");
        esperarValor(By.id("act-nombre"), "Revisar");
        vaciar(By.id("act-nombre"));
        escribir(By.id("act-nombre"), "Validar");
        seleccionar(By.id("act-tipo"), "MANUAL");
        seleccionarTexto(By.id("act-lane"), "Supervisor");
        guardarFormularioPanel();
        esperarNotificacion("Elemento actualizado.");

        // Nueva lane y tipo; el dibujo y el arco se actualizan con el nombre nuevo
        esperarTexto(subtituloItem("Validar"), "Tarea manual · Supervisor");
        visible(textoEnLienzo("Validar"));
        irAPestana("Conexiones");
        visible(itemPanel(tituloArco("Validar", "Aprobar")));
        assertEquals(1, conexionesEnLienzo("Arco_"));

        verHistorialProceso(proceso);
        visible(entradaHistorial("EDITAR", "Actividad 'Validar' modificada"));

        // Un lector ve el diagrama pero no tiene opciones de edicion
        volverAProcesos();
        Invitado lector = invitarUsuario("LECTOR");
        cerrarSesion();
        iniciarSesion(lector.email(), lector.password());
        esperarPantallaProcesos();
        abrirEditor(proceso);
        visible(itemPanel("Validar"));
        assertFalse(existe(By.cssSelector(".paleta")));
        assertFalse(existe(botonPanel("+ Actividad")));
        assertFalse(existe(accionItem("Validar", "Editar")));
        assertFalse(existe(accionItem("Validar", "Eliminar")));
    }

    @Test
    @DisplayName("HU-10: el administrador elimina una actividad con confirmacion, se borran sus arcos y avisa lo que queda desconectado")
    void eliminarActividad_borraSusArcos() {
        prepararDiagrama();
        crearActividad("Analista", "Recibir", "TAREA");
        crearActividad("Analista", "Revisar", "TAREA");
        crearActividad("Supervisor", "Archivar", "TAREA");
        crearArco("Recibir", "Revisar");
        crearArco("Revisar", "Archivar");

        // Pide confirmacion: si se cancela, la actividad sigue ahi
        irAPestana("Elementos");
        clic(accionItem("Revisar", "Eliminar"));
        assertEquals("Eliminar actividad", visible(By.id("confirmacion-titulo")).getText().trim());
        cancelarDialogo();
        assertTrue(existe(itemPanel("Revisar")));

        String aviso = eliminarNodo("Revisar");
        assertTrue(aviso.contains("Arcos eliminados: 2."), aviso);
        assertTrue(aviso.contains("'Recibir' no tiene camino de salida"), aviso);

        irAPestana("Conexiones");
        visible(By.xpath("//aside//li[contains(@class,'vacio-item') and normalize-space()='Sin arcos.']"));
        esperarConexionesEnLienzo("Arco_", 0);

        verHistorialProceso(proceso);
        visible(entradaHistorial("ELIMINAR", "'Revisar' eliminado"));
        visible(entradaHistorial("ELIMINAR", "Arco eliminado junto con 'Revisar'"));

        // Un editor puede editar actividades, pero eliminarlas es solo del administrador
        volverAProcesos();
        Invitado editor = invitarUsuario("EDITOR");
        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        esperarPantallaProcesos();
        abrirEditor(proceso);
        assertTrue(visible(accionItem("Recibir", "Editar")).isDisplayed());
        assertFalse(existe(accionItem("Recibir", "Eliminar")));
    }
}
