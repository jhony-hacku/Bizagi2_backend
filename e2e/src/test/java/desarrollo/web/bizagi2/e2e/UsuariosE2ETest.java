package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-02: Registro de usuario en empresa")
class UsuariosE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("El administrador invita a un editor, que puede entrar y crear procesos pero no gestionar usuarios")
    void invitarEditor_puedeEntrarYCrearProcesos() {
        registrarEmpresa();

        Invitado editor = invitarUsuario("EDITOR");
        esperarTexto(enFilaUsuario(editor.email(), "/td[2]"), "Editor");
        esperarTexto(enFilaUsuario(editor.email(), "//span[contains(@class,'badge')]"), "Activo");

        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        esperarPantallaProcesos();

        // El rol de acceso Editor puede escribir procesos...
        assertTrue(visible(By.linkText("Nuevo proceso")).isDisplayed());
        // ...pero la gestion de usuarios es solo del administrador
        assertFalse(existe(By.linkText("Usuarios")));
    }

    @Test
    @DisplayName("Cambiar el rol y desactivar a un usuario conserva sus procesos y su historial, y le impide entrar")
    void cambiarRolYDesactivar_conservaProcesosEHistorial() {
        Cuenta admin = registrarEmpresa();
        Invitado editor = invitarUsuario("EDITOR");

        // El editor crea un proceso: pertenece a la empresa, no a el
        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        esperarPantallaProcesos();
        String proceso = "Proceso del editor " + identificadorUnico();
        crearProceso(proceso, "Operaciones");

        // El administrador le cambia el rol...
        cerrarSesion();
        iniciarSesion(admin.email(), admin.password());
        esperarPantallaProcesos();
        abrirUsuarios();
        clic(enFilaUsuario(editor.email(), "//button[normalize-space()='Editar']"));
        esperarValor(By.id("email"), editor.email());
        seleccionar(By.id("rolAcceso"), "LECTOR");
        clic(BOTON_ENVIAR);
        esperarTexto(enFilaUsuario(editor.email(), "/td[2]"), "Solo lectura");

        // ...y lo desactiva, con confirmacion
        clic(enFilaUsuario(editor.email(), "//button[normalize-space()='Desactivar']"));
        assertEquals("Desactivar usuario", visible(By.id("confirmacion-titulo")).getText().trim());
        confirmarDialogo();
        esperarTexto(enFilaUsuario(editor.email(), "//span[contains(@class,'badge')]"), "Inactivo");

        // Su proceso sigue disponible y el historial conserva quien lo creo
        volverAProcesos();
        assertTrue(visible(filaProceso(proceso)).isDisplayed());
        abrirHistorial(proceso);
        assertTrue(visible(entradaHistorial("CREAR")).getText().contains(editor.nombre()));

        // Un usuario desactivado ya no puede iniciar sesion
        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        assertEquals("Correo o contraseña incorrectos.", visible(ERROR_FORMULARIO).getText().trim());
        assertTrue(estaEn("/login"));
    }
}
