package desarrollo.web.bizagi2.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@DisplayName("E2E HU-25 a HU-28: Mensajes entre pools")
class MensajesE2ETest extends BaseEditorE2ETest {

    private static final String CONTENIDO = "idPedido: texto\nmonto: número";

    private String proceso;
    private String poolVentas;

    // Proceso con dos pools de la empresa (ventas y bodega), cada uno con una lane
    private void prepararDosPools() {
        registrarEmpresa();
        crearRol("Vendedor", "Vende");
        crearRol("Bodeguero", "Despacha");
        proceso = "Proceso mensajes " + identificadorUnico();
        crearProceso(proceso, "Logistica");
        abrirEditor(proceso);
        poolVentas = poolEmpresa();
        agregarLane(poolVentas, "Vendedor", null);
        crearPool("Bodega", "EMPRESA_PROPIETARIA");
        agregarLane("Bodega", "Bodeguero", null);
    }

    // Abre el formulario de mensaje y elige quien lo envia y a que pool llega
    private void llenarMensaje(String nombre, String origen, String poolDestino) {
        irAPestana("Conexiones");
        clic(botonPanel("+ Mensaje"));
        escribir(By.id("msg-nombre"), nombre);
        seleccionarNodo(By.id("msg-origen"), origen);
        seleccionarNodo(By.id("msg-destino"), poolDestino);
    }

    @Test
    @DisplayName("HU-25: se lanza un mensaje documentado hacia otro pool y el editor avisa mientras no tenga receptor")
    void enviarMensajeEntrePools() {
        prepararDosPools();
        crearEvento(poolVentas, "Enviar", "MENSAJE_LANZAMIENTO", CONTENIDO, "idPedido", false);
        esperarTexto(subtituloItem("Enviar"), "Lanzamiento de mensaje");

        // El pool destino no puede ser el mismo del que sale el mensaje
        llenarMensaje("Pedido", "Enviar", "Bodega");
        List<String> destinos = opciones(By.id("msg-destino"));
        assertEquals(List.of("Bodega (Empresa propietaria)"), destinos);
        escribir(By.id("msg-contenido"), CONTENIDO);
        guardarFormularioPanel();
        visible(itemPanel("Pedido"));
        assertTrue(visible(subtituloItem("Pedido")).getText().contains("Enviar → Bodega"));
        esperarConexionesEnLienzo("Mensaje_", 1);

        // Sin un Message Catch con el mismo nombre en el pool destino, queda sin receptor
        esperarAdvertencia("El mensaje 'Pedido' queda sin receptor en el pool 'Bodega'");
        crearEvento("Bodega", "Pedido", "MENSAJE_RECEPCION_INTERMEDIO", CONTENIDO, "idPedido", false);
        assertFalse(advertenciasActuales().stream().anyMatch(a -> a.contains("queda sin receptor")));

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Mensaje 'Pedido' creado de 'Enviar' al pool 'Bodega'"));
    }

    @Test
    @DisplayName("HU-27: un Message Catch de inicio o intermedio exige un mensaje lanzado u origen externo, y el de inicio no admite arcos entrantes")
    void recibirMensaje() {
        prepararDosPools();

        // Intermedio sin mensaje lanzado con su nombre: advertencia
        crearEvento("Bodega", "Pago", "MENSAJE_RECEPCION_INTERMEDIO", "valor: número", "idPago", false);
        esperarTexto(subtituloItem("Pago"), "Recepción de mensaje (intermedio)");
        esperarAdvertencia("No existe un mensaje lanzado con el nombre 'Pago' ni se marco el evento como de origen externo");

        // De inicio y de origen externo: no necesita un mensaje lanzado dentro del proceso
        crearActividad("Bodega", "Bodeguero", "Preparar", "TAREA");
        crearEvento("Bodega", "Alta", "MENSAJE_RECEPCION_INICIO", "idCliente: texto", "idCliente", true);
        esperarTexto(subtituloItem("Alta"), "Recepción de mensaje (inicio)");
        assertFalse(advertenciasActuales().stream().anyMatch(a -> a.contains("nombre 'Alta'")));

        // Un Message Catch de inicio arranca el proceso: no puede tener arcos entrantes
        abrirFormularioArco();
        seleccionarTexto(By.id("arco-pool"), "Bodega");
        llenarArco("Preparar", "Alta", null, null);
        guardarFormularioPanel();
        assertEquals("Un Message Catch de inicio no puede tener arcos entrantes", errorDelPanel());
        clic(botonPanel("Cancelar"));

        // Se edita y se elimina como los demas elementos, con confirmacion
        editarNodo("Pago");
        esperarValor(By.id("ev-nombre"), "Pago");
        clic(By.xpath("//aside//label[contains(@class,'check') and contains(normalize-space(),'origen externo')]/input"));
        guardarFormularioPanel();
        esperarNotificacion("Elemento actualizado.");
        visible(itemPanel("Pago"));
        assertFalse(advertenciasActuales().stream().anyMatch(a -> a.contains("nombre 'Pago'")));

        irAPestana("Elementos");
        clic(accionItem("Pago", "Eliminar"));
        assertEquals("Eliminar evento", visible(By.id("confirmacion-titulo")).getText().trim());
        confirmarDialogo();
        esperarQueDesaparezca(itemPanel("Pago"));

        verHistorialProceso(proceso);
        visible(entradaHistorial("ELIMINAR", "'Pago' eliminado"));
    }

    @Test
    @DisplayName("HU-26: se notifica a un sistema externo indicando el canal, el momento y que hacer si la notificacion falla")
    void notificarSistemaExterno() {
        registrarEmpresa();
        crearRol("Vendedor", "Vende");
        proceso = "Proceso notificaciones " + identificadorUnico();
        crearProceso(proceso, "Logistica");
        abrirEditor(proceso);
        poolVentas = poolEmpresa();
        agregarLane(poolVentas, "Vendedor", null);
        crearPool("Correo", "SISTEMA_EXTERNO");
        crearActividad("Vendedor", "Notificar", "ENVIO");
        crearActividad("Vendedor", "Corregir", "TAREA");

        llenarMensaje("Aviso", "Notificar", "Correo");
        escribir(By.id("msg-contenido"), "idPedido: texto");
        seleccionar(By.id("msg-tipo"), "CORREO");
        escribir(By.id("msg-momento"), "Al despachar el pedido");
        seleccionar(By.id("msg-fallo"), "DERIVAR_A_MANEJO_ERROR");
        seleccionarTexto(By.id("msg-error"), "Corregir");
        guardarFormularioPanel();
        visible(itemPanel("Aviso"));
        visible(By.xpath("//aside//li[div/strong[normalize-space()='Aviso']]//span[contains(@class,'sub') and normalize-space()='Destino: Correo electrónico']"));
        assertTrue(visible(subtituloItem("Aviso")).getText().contains("Notificar → Correo"));

        // Editar: si falla, ahora termina el proceso
        clic(accionItem("Aviso", "Editar"));
        esperarValor(By.id("msg-nombre"), "Aviso");
        assertEquals("Al despachar el pedido", visible(By.id("msg-momento")).getAttribute("value"));
        seleccionar(By.id("msg-fallo"), "FINALIZAR_PROCESO");
        assertFalse(existe(By.id("msg-error")));
        guardarFormularioPanel();
        esperarNotificacion("Cambios guardados.");

        // Eliminar, con confirmacion
        irAPestana("Conexiones");
        clic(accionItem("Aviso", "Eliminar"));
        assertEquals("Eliminar mensaje", visible(By.id("confirmacion-titulo")).getText().trim());
        confirmarDialogo();
        esperarNotificacion("Mensaje eliminado.");
        esperarQueDesaparezca(itemPanel("Aviso"));

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Mensaje 'Aviso' creado de 'Notificar' al pool 'Correo'"));
        visible(entradaHistorial("ELIMINAR"));
    }

    @Test
    @DisplayName("HU-28: el mensaje declara su clave de correlacion, que debe coincidir con la del receptor y no repetirse con el mismo nombre")
    void correlacionarMensajes() {
        prepararDosPools();
        crearEvento(poolVentas, "Enviar", "MENSAJE_LANZAMIENTO", CONTENIDO, "idPedido", false);
        llenarMensaje("Pedido", "Enviar", "Bodega");
        guardarFormularioPanel();
        visible(itemPanel("Pedido"));
        crearEvento("Bodega", "Pedido", "MENSAJE_RECEPCION_INTERMEDIO", CONTENIDO, null, false);

        // Sin clave: el editor lo advierte en el mensaje y en el Message Catch intermedio
        esperarAdvertencia("El mensaje 'Pedido' no tiene clave de correlacion");
        esperarAdvertencia("El Message Catch intermedio 'Pedido' no tiene clave de correlacion");

        // Se declara la clave y que hacer si no hay un caso en espera
        irAPestana("Conexiones");
        clic(accionItem("Pedido", "Correlación"));
        escribir(By.id("cor-criterio"), "idPedido");
        seleccionar(By.id("cor-sin-caso"), "INICIAR_CASO_NUEVO");
        guardarFormularioPanel();
        visible(By.xpath("//aside//li[div/strong[normalize-space()='Pedido']]//span[contains(@class,'sub') and normalize-space()='Correlación: idPedido']"));

        // La clave del receptor debe ser la misma
        editarNodo("Pedido");
        esperarValor(By.id("ev-nombre"), "Pedido");
        escribir(By.id("ev-clave"), "numeroPedido");
        guardarFormularioPanel();
        esperarNotificacion("Elemento actualizado.");
        esperarAdvertencia("La clave de correlacion de 'Pedido' (numeroPedido) no coincide con la del mensaje lanzado (idPedido)");

        editarNodo("Pedido");
        esperarValor(By.id("ev-clave"), "numeroPedido");
        vaciar(By.id("ev-clave"));
        escribir(By.id("ev-clave"), "idPedido");
        // La notificacion del guardado anterior puede seguir en pantalla: se espera a que la validacion cambie
        guardarFormularioPanel();
        visible(itemPanel("Pedido"));
        irAPestana("Validación");
        esperarQueDesaparezca(advertencia("no coincide con la del mensaje lanzado"));
        List<String> advertencias = advertenciasActuales();
        assertFalse(advertencias.stream().anyMatch(a -> a.contains("no coincide") || a.contains("no tiene clave")),
                advertencias.toString());

        // Otro mensaje con el mismo nombre y la misma clave seria ambiguo
        crearEvento(poolVentas, "Reenviar", "MENSAJE_LANZAMIENTO", CONTENIDO, "idPedido", false);
        llenarMensaje("Pedido", "Reenviar", "Bodega");
        guardarFormularioPanel();
        // El mensaje nuevo es el "Pedido" que aun no muestra correlacion (la lista no garantiza el orden)
        String sinCorrelacion = "//aside//ul[contains(@class,'items')]/li[div/strong[normalize-space()='Pedido']"
                + " and not(.//span[starts-with(normalize-space(),'Correlación:')])]";
        visible(By.xpath(sinCorrelacion));
        cerrarNotificaciones();
        clic(By.xpath(sinCorrelacion + "//button[normalize-space()='Correlación']"));
        // Formulario de una correlacion nueva: el criterio llega vacio
        assertEquals("", visible(By.id("cor-criterio")).getAttribute("value"));
        escribir(By.id("cor-criterio"), "idPedido");
        guardarFormularioPanel();
        // Los dos mensajes "Pedido" quedan con la misma clave
        By conClave = By.xpath("//aside//ul[contains(@class,'items')]/li[div/strong[normalize-space()='Pedido']]"
                + "//span[normalize-space()='Correlación: idPedido']");
        try {
            espera.until(d -> d.findElements(conClave).size() == 2);
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError("La segunda correlacion no se guardo. Panel: "
                    + driver.findElement(By.cssSelector("aside .contenido")).getText(), e);
        }
        esperarAdvertencia("El mensaje 'Pedido' es ambiguo: otro mensaje comparte su nombre y su clave de correlacion");

        verHistorialProceso(proceso);
        visible(entradaHistorial("CREAR", "Clave de correlacion 'idPedido' definida para el mensaje 'Pedido'"));
    }
}
