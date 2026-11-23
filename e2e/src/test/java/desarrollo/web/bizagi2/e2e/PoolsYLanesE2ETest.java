package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

@DisplayName("E2E HU-21, HU-22 y HU-24: Pools, lanes y sus permisos")
class PoolsYLanesE2ETest extends BaseEditorE2ETest {

    @Test
    @DisplayName("HU-21: el proceso nace con el pool de la empresa y se agregan pools externos, que son caja negra")
    void configurarPools() {
        Cuenta cuenta = registrarEmpresa();
        String proceso = "Proceso pools " + identificadorUnico();
        crearProceso(proceso, "Ventas");
        abrirEditor(proceso);

        // El pool de la empresa se crea con el proceso y lleva su nombre
        assertEquals(cuenta.empresa(), poolEmpresa());
        esperarTexto(By.xpath(xpathPool(cuenta.empresa()) + "//span[contains(@class,'sub')]"), "Empresa propietaria");

        // Un cliente es un participante externo: se dibuja sin lanes ni elementos internos
        crearPool("Cliente", "CLIENTE");
        esperarNotificacion("Cambios guardados.");
        esperarTexto(By.xpath(xpathPool("Cliente") + "//span[contains(@class,'sub')]"), "Cliente");
        assertFalse(existe(By.xpath(xpathPool("Cliente") + "//button[normalize-space()='+ Lane']")));
        assertTrue(existe(By.xpath(xpathPool(cuenta.empresa()) + "//button[normalize-space()='+ Lane']")));

        abrirFormularioNodo("+ Actividad");
        List<String> pools = opciones(By.id("act-pool"));
        assertTrue(pools.contains(cuenta.empresa()), pools.toString());
        assertFalse(pools.contains("Cliente"), pools.toString());
        clic(botonPanel("Cancelar"));

        // Varios pools en el mismo diagrama; se pueden renombrar
        crearPool("Sistema de pagos", "SISTEMA_EXTERNO");
        irAPestana("Estructura");
        clic(By.xpath(xpathPool("Cliente") + "//button[normalize-space()='Editar']"));
        esperarValor(By.id("pool-nombre"), "Cliente");
        vaciar(By.id("pool-nombre"));
        escribir(By.id("pool-nombre"), "Cliente mayorista");
        guardarFormularioPanel();
        visible(By.xpath(xpathPool("Cliente mayorista")));
        visible(By.xpath(xpathPool("Sistema de pagos")));

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Pool 'Cliente' (CLIENTE) creado"));
        visible(entradaHistorial("EDITAR", "Pool 'Cliente mayorista'"));
    }

    @Test
    @DisplayName("HU-22: las lanes se crean con un rol, se renombran, se reordenan y no se eliminan si tienen actividades")
    void gestionarLanes() {
        registrarEmpresa();
        crearRol("Analista", "Revisa");
        crearRol("Supervisor", "Aprueba");
        String proceso = "Proceso lanes " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        String pool = poolEmpresa();

        // Cada lane se asocia a un rol de proceso; el nombre es opcional
        agregarLane(pool, "Analista", "Mesa de ayuda");
        esperarTexto(By.xpath(xpathPool(pool) + "//li[div/strong[normalize-space()='Mesa de ayuda']]//span[contains(@class,'sub')]"),
                "Rol: Analista");
        agregarLane(pool, "Supervisor", null);

        // Renombrar
        clic(accionLane(pool, "Mesa de ayuda", "Editar"));
        esperarValor(By.id("lane-nombre"), "Mesa de ayuda");
        vaciar(By.id("lane-nombre"));
        escribir(By.id("lane-nombre"), "Soporte");
        guardarFormularioPanel();
        visible(lane(pool, "Soporte"));

        // Reordenar: la primera lane baja
        esperarTexto(By.xpath(xpathPool(pool) + "//ul/li[1]/div/strong"), "Soporte");
        clic(accionLane(pool, "Soporte", "Bajar lane"));
        esperarTexto(By.xpath(xpathPool(pool) + "//ul/li[1]/div/strong"), "Supervisor");

        // Cada actividad pertenece a una lane; una lane con actividades no se puede eliminar
        crearActividad("Soporte", "Atender", "TAREA");
        esperarTexto(subtituloItem("Atender"), "Tarea · Soporte");
        irAPestana("Estructura");
        clic(accionLane(pool, "Soporte", "Eliminar"));
        confirmarDialogo();
        esperarNotificacion("La lane tiene 1 actividad(es): reasignalas a otra lane primero");
        assertTrue(existe(lane(pool, "Soporte")));

        // Al mover la actividad a otra lane, ya se puede eliminar
        editarNodo("Atender");
        esperarValor(By.id("act-nombre"), "Atender");
        seleccionarTexto(By.id("act-lane"), "Supervisor");
        guardarFormularioPanel();
        esperarTexto(subtituloItem("Atender"), "Tarea · Supervisor");
        irAPestana("Estructura");
        clic(accionLane(pool, "Soporte", "Eliminar"));
        confirmarDialogo();
        esperarNotificacion("Lane eliminada.");
        esperarQueDesaparezca(lane(pool, "Soporte"));

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Lane 'Mesa de ayuda' creada con el rol 'Analista'"));
        visible(entradaHistorial("ELIMINAR", "Lane 'Soporte' eliminada"));
    }

    @Test
    @DisplayName("HU-24: si la empresa lo restringe, un editor no modifica pools ni lanes, y un lector solo consulta")
    void permisosSobrePoolsYLanes() {
        Cuenta cuenta = registrarEmpresa();
        crearRol("Analista", "Revisa");
        String proceso = "Proceso permisos " + identificadorUnico();
        crearProceso(proceso, "Operaciones");
        abrirEditor(proceso);
        String pool = poolEmpresa();
        agregarLane(pool, "Analista", null);

        // Solo los roles de proceso de la empresa se ofrecen para las lanes
        clic(By.xpath(xpathPool(pool) + "//button[normalize-space()='+ Lane']"));
        seleccionarTexto(By.id("lane-rol"), "Analista");
        assertEquals(List.of("Selecciona un rol…", "Analista"), opciones(By.id("lane-rol")));
        clic(botonPanel("Cancelar"));

        abrir("/procesos");
        esperarPantallaProcesos();
        Invitado editor = invitarUsuario("EDITOR");
        Invitado lector = invitarUsuario("LECTOR");

        // El administrador decide que los editores no modifican la estructura
        clic(By.linkText("Empresa"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Empresa"));
        By permiso = By.xpath("//label[contains(@class,'check') and contains(normalize-space(),'pools y lanes')]/input");
        // Se espera a que el formulario cargue los datos de la empresa antes de cambiarlo
        esperarValor(By.id("nit"), cuenta.nit());
        assertTrue(visible(permiso).isSelected());
        clic(permiso);
        clic(BOTON_ENVIAR);
        esperarNotificacion("Datos de la empresa actualizados.");

        // El editor sigue modelando actividades, pero no ve opciones sobre pools y lanes
        cerrarSesion();
        iniciarSesion(editor.email(), editor.password());
        esperarPantallaProcesos();
        abrirEditor(proceso);
        irAPestana("Elementos");
        visible(botonPanel("+ Actividad"));
        irAPestana("Estructura");
        visible(lane(pool, "Analista"));
        assertFalse(existe(botonPanel("+ Pool")));
        assertFalse(existe(botonPanel("+ Lane")));
        assertFalse(existe(accionLane(pool, "Analista", "Editar")));

        // El lector ve el pool y sus lanes sin poder modificar nada
        reabrirSesionComo(lector);
        abrirEditor(proceso);
        irAPestana("Estructura");
        visible(lane(pool, "Analista"));
        assertFalse(existe(botonPanel("+ Pool")));
        assertFalse(existe(botonPanel("+ Lane")));
        irAPestana("Elementos");
        assertFalse(existe(botonPanel("+ Actividad")));
    }

    // Cierra la sesion actual desde la lista de procesos e inicia con otro usuario
    private void reabrirSesionComo(Invitado invitado) {
        abrir("/procesos");
        esperarPantallaProcesos();
        cerrarSesion();
        iniciarSesion(invitado.email(), invitado.password());
        esperarPantallaProcesos();
    }
}
