package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-14 a HU-16: Gateways")
class GatewaysE2ETest extends BaseEditorE2ETest {

    private String proceso;

    // Empresa con un rol, un proceso con una lane y dos actividades a las que puede llevar una decision
    private void prepararDiagrama() {
        registrarEmpresa();
        crearRol("Analista", "Revisa");
        proceso = "Proceso gateways " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        agregarLane(poolEmpresa(), "Analista", null);
        crearActividad("Analista", "Aprobar", "TAREA");
        crearActividad("Analista", "Rechazar", "TAREA");
    }

    @Test
    @DisplayName("HU-14: se crean gateways de los tres tipos y el editor exige dos salidas con condicion en el exclusivo")
    void crearGateways_yValidarSusSalidas() {
        prepararDiagrama();

        crearGateway("Evaluar", "EXCLUSIVA");
        esperarNotificacion("Elemento creado.");
        crearGateway("Bifurcar", "PARALELA");
        crearGateway("Combinar", "INCLUSIVA");
        esperarTexto(subtituloItem("Evaluar"), "Gateway Exclusivo (X)");
        esperarTexto(subtituloItem("Bifurcar"), "Gateway Paralelo (+)");
        esperarTexto(subtituloItem("Combinar"), "Gateway Inclusivo (O)");
        visible(textoEnLienzo("Evaluar"));

        // Sin salidas: el gateway de divergencia necesita al menos dos
        esperarAdvertencia("El gateway 'Evaluar' debe tener al menos dos arcos salientes");

        // Con dos salidas, la que no tiene condicion se marca como error
        crearArco("Evaluar", "Aprobar", null, "monto <= 1000");
        crearArco("Evaluar", "Rechazar");
        String error = esperarAdvertencia("que sale del gateway 'Evaluar' debe tener una condicion");
        assertFalse(advertenciasActuales().contains("El gateway 'Evaluar' debe tener al menos dos arcos salientes"), error);

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Gateway 'Evaluar' (EXCLUSIVA) creado"));
    }

    @Test
    @DisplayName("HU-15: el editor avisa de condiciones repetidas y al pasar el gateway a paralelo se quitan las condiciones")
    void editarGateway_aParaleloQuitaCondiciones() {
        prepararDiagrama();
        crearGateway("Evaluar", "EXCLUSIVA");
        crearArco("Evaluar", "Aprobar", null, "aprobado");
        crearArco("Evaluar", "Rechazar", null, "aprobado");
        esperarAdvertencia("El gateway exclusivo 'Evaluar' tiene condiciones repetidas: no son mutuamente excluyentes");

        // Cambiar la condicion de una salida
        irAPestana("Conexiones");
        clic(accionItem(tituloArco("Evaluar", "Rechazar"), "Editar"));
        esperarValor(By.id("arco-condicion"), "aprobado");
        vaciar(By.id("arco-condicion"));
        escribir(By.id("arco-condicion"), "rechazado");
        guardarFormularioPanel();
        esperarTexto(subtituloItem(tituloArco("Evaluar", "Rechazar")), "rechazado");
        assertFalse(advertenciasActuales().stream().anyMatch(a -> a.contains("condiciones repetidas")));

        // Pasar a paralelo: se avisa antes de guardar y las condiciones desaparecen
        editarNodo("Evaluar");
        esperarValor(By.id("gw-nombre"), "Evaluar");
        seleccionar(By.id("gw-tipo"), "PARALELA");
        visible(By.xpath("//aside//p[contains(@class,'nota') and contains(normalize-space(),'se eliminan las condiciones')]"));
        guardarFormularioPanel();
        esperarNotificacion("Elemento actualizado.");
        esperarTexto(subtituloItem("Evaluar"), "Gateway Paralelo (+)");

        irAPestana("Conexiones");
        visible(itemPanel(tituloArco("Evaluar", "Aprobar")));
        assertFalse(existe(subtituloItem(tituloArco("Evaluar", "Aprobar"))));
        assertFalse(existe(subtituloItem(tituloArco("Evaluar", "Rechazar"))));

        verHistorialProceso(proceso);
        visible(entradaHistorial("EDITAR", "paso a PARALELA: se eliminaron las condiciones de sus arcos salientes"));
    }

    @Test
    @DisplayName("HU-16: el administrador elimina un gateway con confirmacion y se eliminan todos sus arcos")
    void eliminarGateway_borraSusArcos() {
        prepararDiagrama();
        crearActividad("Analista", "Recibir", "TAREA");
        crearGateway("Evaluar", "EXCLUSIVA");
        crearArco("Recibir", "Evaluar");
        crearArco("Evaluar", "Aprobar", null, "monto <= 1000");
        crearArco("Evaluar", "Rechazar", null, "monto > 1000");
        esperarConexionesEnLienzo("Arco_", 3);

        irAPestana("Elementos");
        clic(accionItem("Evaluar", "Eliminar"));
        assertEquals("Eliminar gateway", visible(By.id("confirmacion-titulo")).getText().trim());
        confirmarDialogo();
        String aviso = esperarNotificacion("Elemento eliminado.");
        assertTrue(aviso.contains("Arcos eliminados: 3."), aviso);
        assertTrue(aviso.contains("'Recibir' no tiene camino de salida"), aviso);
        esperarQueDesaparezca(itemPanel("Evaluar"));
        esperarConexionesEnLienzo("Arco_", 0);

        verHistorialProceso(proceso);
        visible(entradaHistorial("ELIMINAR", "'Evaluar' eliminado"));
    }
}
