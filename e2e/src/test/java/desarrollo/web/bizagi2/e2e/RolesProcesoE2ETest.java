package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-17 a HU-20: Roles de proceso")
class RolesProcesoE2ETest extends BaseEditorE2ETest {

    private static final By BUSQUEDA = By.cssSelector("input[aria-label='Buscar por nombre']");

    @Test
    @DisplayName("HU-17: el administrador crea un rol con nombre y descripcion, y el nombre no se puede repetir en la empresa")
    void crearRol_conNombreUnico() {
        registrarEmpresa();

        crearRol("Analista", "Revisa las solicitudes");
        esperarNotificacion("Rol creado.");
        assertTrue(visible(enFilaRol("Analista", "//span[contains(@class,'sub')]")).getText().contains("Revisa las solicitudes"));
        esperarTexto(enFilaRol("Analista", "//span[contains(@class,'badge')]"), "Sin uso · eliminable");

        // Mismo nombre (sin importar mayusculas): el backend lo rechaza
        clic(By.xpath("//button[normalize-space()='Nuevo rol']"));
        escribir(By.id("nombre"), "analista");
        clic(BOTON_ENVIAR);
        esperarNotificacion("Ya existe un rol con ese nombre en la empresa");
    }

    @Test
    @DisplayName("HU-17 y HU-18: el rol queda disponible para las lanes y, al renombrarlo, la lane muestra el nombre nuevo y el historial lo registra")
    void editarRol_actualizaLaLaneYElHistorial() {
        registrarEmpresa();
        crearRol("Supervisor", "Aprueba");
        String proceso = "Proceso con roles " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        String pool = poolEmpresa();
        agregarLane(pool, "Supervisor", null);

        abrirRoles();
        clic(accionRol("Supervisor", "Editar"));
        esperarValor(By.id("nombre"), "Supervisor");
        vaciar(By.id("nombre"));
        escribir(By.id("nombre"), "Coordinador");
        escribir(By.id("descripcion"), "Coordina y aprueba");
        clic(BOTON_ENVIAR);
        esperarNotificacion("Rol actualizado.");
        visible(filaRol("Coordinador"));

        // Historial del rol
        clic(accionRol("Coordinador", "Historial"));
        visible(By.xpath("//tr[contains(@class,'detalle')]//li[strong[normalize-space()='EDITAR']"
                + " and contains(., \"Rol 'Supervisor' modificado a 'Coordinador'\")]"));

        // El proceso sigue apuntando al mismo rol y lo muestra con el nombre nuevo
        abrir("/procesos");
        esperarPantallaProcesos();
        abrirEditor(proceso);
        irAPestana("Estructura");
        visible(By.xpath(xpathPool(pool) + "//li//span[contains(@class,'sub') and normalize-space()='Rol: Coordinador']"));
    }

    @Test
    @DisplayName("HU-19: un rol en uso no se puede eliminar (indica en que procesos); uno sin uso se elimina con confirmacion")
    void eliminarRol_soloSiNoEstaEnUso() {
        registrarEmpresa();
        crearRol("Auditor", "Revisa cumplimiento");
        crearRol("Temporal", "Sin uso");
        String proceso = "Proceso auditado " + identificadorUnico();
        crearProceso(proceso, "Calidad");
        abrirEditor(proceso);
        agregarLane(poolEmpresa(), "Auditor", null);

        abrirRoles();
        esperarTexto(enFilaRol("Auditor", "//td[2]//button"), "En 1 proceso");
        clic(accionRol("Auditor", "Eliminar"));
        String aviso = esperarNotificacion("está en uso en:");
        assertTrue(aviso.contains(proceso), aviso);
        assertFalse(existe(DIALOGO));
        assertTrue(existe(filaRol("Auditor")));

        // El rol sin uso pide confirmacion: si se cancela no pasa nada...
        clic(accionRol("Temporal", "Eliminar"));
        assertEquals("Eliminar rol", visible(By.id("confirmacion-titulo")).getText().trim());
        cancelarDialogo();
        assertTrue(existe(filaRol("Temporal")));

        // ...y si se confirma deja de aparecer en el catalogo
        clic(accionRol("Temporal", "Eliminar"));
        confirmarDialogo();
        esperarNotificacion("Rol eliminado.");
        esperarQueDesaparezca(filaRol("Temporal"));
    }

    @Test
    @DisplayName("HU-20: se consultan los roles con su uso, se buscan por nombre y un lector los ve sin poder modificarlos")
    void consultarRoles_buscarYVerUso() {
        registrarEmpresa();
        crearRol("Analista", "Analiza");
        crearRol("Gerente", "Decide");
        String proceso = "Proceso consultado " + identificadorUnico();
        crearProceso(proceso, "Ventas");
        abrirEditor(proceso);
        agregarLane(poolEmpresa(), "Gerente", null);

        abrirRoles();
        // Uso de cada rol: el que esta en una lane muestra en que procesos
        clic(enFilaRol("Gerente", "//td[2]//button"));
        visible(By.xpath("//tr[contains(@class,'detalle')]//li[normalize-space()=" + literal(proceso) + "]"));
        esperarTexto(enFilaRol("Analista", "//span[contains(@class,'badge')]"), "Sin uso · eliminable");

        // Busqueda por nombre
        escribir(BUSQUEDA, "geren");
        esperarQueDesaparezca(filaRol("Analista"));
        assertTrue(visible(filaRol("Gerente")).isDisplayed());

        // Un usuario de solo lectura ve el catalogo, pero no puede crear, editar ni eliminar
        Invitado lector = invitarUsuario("LECTOR");
        cerrarSesion();
        iniciarSesion(lector.email(), lector.password());
        esperarPantallaProcesos();
        abrirRoles();
        visible(filaRol("Analista"));
        visible(filaRol("Gerente"));
        assertTrue(existe(accionRol("Gerente", "Historial")));
        assertFalse(existe(By.xpath("//button[normalize-space()='Nuevo rol']")));
        assertFalse(existe(accionRol("Gerente", "Editar")));
        assertFalse(existe(accionRol("Gerente", "Eliminar")));
    }
}
