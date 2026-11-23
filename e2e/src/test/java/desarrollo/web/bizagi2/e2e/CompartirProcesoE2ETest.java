package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

@DisplayName("E2E HU-23: Compartir procesos con otras empresas")
class CompartirProcesoE2ETest extends BaseEditorE2ETest {

    private static final By SIN_COMPARTIR = By.xpath("//td[contains(@class,'vacio') and "
            + "normalize-space()='Este proceso no se comparte con ninguna empresa.']");

    @Test
    @DisplayName("El administrador comparte un proceso en solo lectura con otra empresa por su NIT y luego deja de compartirlo")
    void compartirYDejarDeCompartir() {
        // La empresa invitada existe antes
        Cuenta invitada = registrarEmpresa();
        cerrarSesion();

        Cuenta propietaria = registrarEmpresa();
        crearRol("Analista", "Revisa");
        String proceso = "Proceso compartido " + identificadorUnico();
        crearProceso(proceso, "Ventas");
        abrirEditor(proceso);
        agregarLane(poolEmpresa(), "Analista", null);
        crearActividad("Analista", "Revisar", "TAREA");

        // Por defecto no se comparte con nadie
        abrir("/procesos");
        esperarPantallaProcesos();
        abrirCompartir(proceso);
        visible(SIN_COMPARTIR);

        // Un NIT que no existe se rechaza
        escribir(By.id("nit"), "NO-EXISTE-" + identificadorUnico());
        clic(BOTON_ENVIAR);
        esperarNotificacion("No existe una empresa con ese NIT");

        escribir(By.id("nit"), invitada.nit());
        clic(BOTON_ENVIAR);
        esperarNotificacion("Proceso compartido con " + invitada.empresa() + ".");
        visible(filaInvitada(invitada));

        verHistorialProceso(proceso);
        visible(entradaHistorial("COMPARTIR", "Proceso compartido en solo lectura con la empresa '" + invitada.empresa() + "'"));

        // La empresa invitada lo consulta sin poder editar el diagrama
        volverAProcesos();
        cerrarSesion();
        iniciarSesion(invitada.email(), invitada.password());
        esperarPantallaProcesos();
        assertFalse(existe(filaProceso(proceso)));
        abrirCompartidos();
        visible(filaProceso(proceso));
        esperarTexto(enFilaProceso(proceso, "/td[2]"), propietaria.empresa());
        clic(enFilaProceso(proceso, "//a[normalize-space()='Ver diagrama']"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".editor-page h1"), proceso));
        visible(By.xpath("//span[contains(@class,'badge') and normalize-space()='Compartido · solo lectura']"));
        visible(itemPanel("Revisar"));
        assertFalse(existe(By.cssSelector(".paleta")));
        assertFalse(existe(botonPanel("+ Actividad")));
        assertFalse(existe(accionItem("Revisar", "Editar")));

        // La propietaria deja de compartirlo y la invitada ya no lo ve
        abrir("/procesos");
        esperarPantallaProcesos();
        cerrarSesion();
        iniciarSesion(propietaria.email(), propietaria.password());
        esperarPantallaProcesos();
        abrirCompartir(proceso);
        clic(By.xpath(xpathFilaInvitada(invitada) + "//button[normalize-space()='Dejar de compartir']"));
        confirmarDialogo();
        esperarNotificacion("Ya no se comparte con " + invitada.empresa() + ".");
        visible(SIN_COMPARTIR);

        verHistorialProceso(proceso);
        visible(entradaHistorial("DEJAR_DE_COMPARTIR"));

        volverAProcesos();
        cerrarSesion();
        iniciarSesion(invitada.email(), invitada.password());
        esperarPantallaProcesos();
        abrirCompartidos();
        visible(By.xpath("//td[contains(@class,'vacio') and contains(normalize-space(),'Ninguna empresa ha compartido procesos contigo')]"));
        assertTrue(estaEn("/procesos/compartidos"));
    }

    private void abrirCompartir(String proceso) {
        clic(accionProceso(proceso, "Compartir"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Compartir · " + proceso));
    }

    private void abrirCompartidos() {
        clic(By.linkText("Compartidos"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Compartidos conmigo"));
    }

    private static String xpathFilaInvitada(Cuenta invitada) {
        return "//tr[td[normalize-space()=" + literal(invitada.nit()) + "]]";
    }

    private static By filaInvitada(Cuenta invitada) {
        return By.xpath(xpathFilaInvitada(invitada));
    }
}
