package desarrollo.web.bizagi2.e2e;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

// Pasos del editor de diagramas (lote 3: HU-08 a HU-16 y HU-21 a HU-28). Todo se hace desde el panel
// lateral (formularios con listas y botones), que es mas estable en Selenium que arrastrar sobre el lienzo;
// el lienzo solo se consulta para comprobar que el diagrama se dibujo.
abstract class BaseEditorE2ETest extends BaseE2ETest {

    private static final String PANEL = "//aside[contains(@class,'panel')]";
    private static final String LIENZO = "//div[contains(@class,'editor-canvas')]";

    // ---------- Abrir el editor y moverse por el panel ----------

    // Desde la lista de procesos abre el diagrama y espera a que el panel termine de cargar
    protected void abrirEditor(String proceso) {
        clic(accionProceso(proceso, "Diagrama"));
        espera.until(ExpectedConditions.urlContains("/editor"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".editor-page h1"), proceso));
        esperarQueDesaparezca(By.cssSelector("aside .contenido > p.vacio"));
    }

    // Vuelve a la lista de procesos y abre otra vez el editor (por ejemplo, despues de cambiar de usuario)
    protected void reabrirEditor(String proceso) {
        abrir("/procesos");
        esperarPantallaProcesos();
        abrirEditor(proceso);
    }

    // Pestanas del panel: Elementos, Conexiones, Estructura, Validación
    protected void irAPestana(String nombre) {
        clic(By.xpath("//nav[contains(@class,'pestanas')]/button[starts-with(normalize-space()," + literal(nombre) + ")]"));
    }

    // Boton del panel por su texto, por ejemplo "+ Actividad" o "+ Lane"
    protected static By botonPanel(String texto) {
        return By.xpath(PANEL + "//button[normalize-space()=" + literal(texto) + "]");
    }

    // Un elemento de las listas del panel (actividades, gateways, eventos, arcos o mensajes), por su titulo
    protected static By itemPanel(String titulo) {
        return By.xpath(PANEL + "//ul[contains(@class,'items')]/li[div/strong[normalize-space()=" + literal(titulo) + "]]");
    }

    protected static By subtituloItem(String titulo) {
        return By.xpath(PANEL + "//ul[contains(@class,'items')]/li[div/strong[normalize-space()=" + literal(titulo) + "]]"
                + "//span[contains(@class,'sub')]");
    }

    protected static By accionItem(String titulo, String accion) {
        return By.xpath(PANEL + "//ul[contains(@class,'items')]/li[div/strong[normalize-space()=" + literal(titulo) + "]]"
                + "//button[normalize-space()=" + literal(accion) + "]");
    }

    protected static String tituloArco(String origen, String destino) {
        return origen + " → " + destino;
    }

    // Error que muestra el formulario abierto en el panel (por ejemplo, una regla del backend)
    protected String errorDelPanel() {
        return visible(By.xpath(PANEL + "//p[contains(@class,'form-error')]")).getText().trim();
    }

    protected void guardarFormularioPanel() {
        clic(By.xpath(PANEL + "//button[@type='submit']"));
    }

    // ---------- Estructura: pools y lanes (HU-21, HU-22, HU-24) ----------

    protected static String xpathPool(String nombre) {
        return PANEL + "//div[contains(@class,'pool-card')][div[contains(@class,'pool-cab')]//strong[normalize-space()="
                + literal(nombre) + "]]";
    }

    protected static By lane(String pool, String lane) {
        return By.xpath(xpathPool(pool) + "//ul/li[div/strong[normalize-space()=" + literal(lane) + "]]");
    }

    protected static By accionLane(String pool, String lane, String accion) {
        return By.xpath(xpathPool(pool) + "//ul/li[div/strong[normalize-space()=" + literal(lane) + "]]"
                + "//button[normalize-space()=" + literal(accion) + " or @aria-label=" + literal(accion) + "]");
    }

    // Nombre del pool de la empresa, que se crea con el proceso (HU-21)
    protected String poolEmpresa() {
        irAPestana("Estructura");
        return visible(By.xpath(PANEL + "//div[contains(@class,'pool-card')][1]/div[contains(@class,'pool-cab')]//strong"))
                .getText().trim();
    }

    // Tipo de participante: EMPRESA_PROPIETARIA, CLIENTE, PROVEEDOR o SISTEMA_EXTERNO
    protected void crearPool(String nombre, String tipoParticipante) {
        irAPestana("Estructura");
        clic(botonPanel("+ Pool"));
        escribir(By.id("pool-nombre"), nombre);
        seleccionar(By.id("pool-tipo"), tipoParticipante);
        guardarFormularioPanel();
        visible(By.xpath(xpathPool(nombre)));
    }

    // Agrega una lane con un rol de proceso; sin nombre, la lane toma el nombre del rol
    protected void agregarLane(String pool, String rol, String nombre) {
        irAPestana("Estructura");
        clic(By.xpath(xpathPool(pool) + "//button[normalize-space()='+ Lane']"));
        seleccionarTexto(By.id("lane-rol"), rol);
        if (nombre != null) {
            escribir(By.id("lane-nombre"), nombre);
        }
        guardarFormularioPanel();
        visible(lane(pool, nombre != null ? nombre : rol));
    }

    // ---------- Elementos: actividades, gateways y eventos (HU-08 a HU-10, HU-14 a HU-16, HU-25 y HU-27) ----------

    // Tipo: TAREA, USUARIO, MANUAL, SERVICIO o ENVIO. Se crea en el primer pool de la empresa.
    protected void crearActividad(String laneNombre, String nombre, String tipo) {
        crearActividad(null, laneNombre, nombre, tipo);
    }

    protected void crearActividad(String pool, String laneNombre, String nombre, String tipo) {
        abrirFormularioNodo("+ Actividad");
        if (pool != null) {
            seleccionarTexto(By.id("act-pool"), pool);
        }
        seleccionarTexto(By.id("act-lane"), laneNombre);
        escribir(By.id("act-nombre"), nombre);
        seleccionar(By.id("act-tipo"), tipo);
        guardarFormularioPanel();
        visible(itemPanel(nombre));
    }

    // Tipo: EXCLUSIVA, PARALELA o INCLUSIVA
    protected void crearGateway(String nombre, String tipo) {
        abrirFormularioNodo("+ Gateway");
        escribir(By.id("gw-nombre"), nombre);
        seleccionar(By.id("gw-tipo"), tipo);
        guardarFormularioPanel();
        visible(itemPanel(nombre));
    }

    // Tipo: INICIO, FIN, MENSAJE_LANZAMIENTO, MENSAJE_RECEPCION_INICIO o MENSAJE_RECEPCION_INTERMEDIO.
    // contenido y clave solo aplican a los eventos de mensaje; origenExterno, a los de recepcion.
    protected void crearEvento(String pool, String nombre, String tipo, String contenido, String clave, boolean origenExterno) {
        abrirFormularioNodo("+ Evento");
        if (pool != null) {
            seleccionarTexto(By.id("ev-pool"), pool);
        }
        seleccionar(By.id("ev-tipo"), tipo);
        escribir(By.id("ev-nombre"), nombre);
        if (contenido != null) {
            escribir(By.id("ev-contenido"), contenido);
        }
        if (clave != null) {
            escribir(By.id("ev-clave"), clave);
        }
        if (origenExterno) {
            clic(By.xpath(PANEL + "//label[contains(@class,'check') and contains(normalize-space(),'origen externo')]/input"));
        }
        guardarFormularioPanel();
        visible(itemPanel(nombre));
    }

    protected void crearEvento(String nombre, String tipo) {
        crearEvento(null, nombre, tipo, null, null, false);
    }

    protected void abrirFormularioNodo(String boton) {
        irAPestana("Elementos");
        clic(botonPanel(boton));
    }

    protected void editarNodo(String nombre) {
        irAPestana("Elementos");
        clic(accionItem(nombre, "Editar"));
    }

    // Elimina un elemento desde la lista del panel, confirmando, y devuelve el aviso del resultado
    protected String eliminarNodo(String nombre) {
        irAPestana("Elementos");
        clic(accionItem(nombre, "Eliminar"));
        confirmarDialogo();
        String aviso = esperarNotificacion("Elemento eliminado.");
        esperarQueDesaparezca(itemPanel(nombre));
        return aviso;
    }

    // ---------- Conexiones: arcos, mensajes y correlacion (HU-11 a HU-13, HU-25, HU-26 y HU-28) ----------

    protected void abrirFormularioArco() {
        irAPestana("Conexiones");
        clic(botonPanel("+ Arco"));
    }

    // Llena el formulario de arco abierto; condicion solo aplica si el origen es un gateway exclusivo o inclusivo
    protected void llenarArco(String origen, String destino, String etiqueta, String condicion) {
        seleccionarNodo(By.id("arco-origen"), origen);
        seleccionarNodo(By.id("arco-destino"), destino);
        if (etiqueta != null) {
            escribir(By.id("arco-etiqueta"), etiqueta);
        }
        if (condicion != null) {
            escribir(By.id("arco-condicion"), condicion);
        }
    }

    protected void crearArco(String origen, String destino, String etiqueta, String condicion) {
        abrirFormularioArco();
        llenarArco(origen, destino, etiqueta, condicion);
        guardarFormularioPanel();
        visible(itemPanel(tituloArco(origen, destino)));
    }

    protected void crearArco(String origen, String destino) {
        crearArco(origen, destino, null, null);
    }

    // Las opciones de origen y destino se ven como "Nombre (Actividad)", "Nombre (Gateway)"...
    protected void seleccionarNodo(By lista, String nombre) {
        espera.until(d -> opcionNodo(d.findElement(lista), nombre) != null);
        new Select(visible(lista)).selectByVisibleText(opcionNodo(driver.findElement(lista), nombre));
    }

    private static String opcionNodo(WebElement lista, String nombre) {
        for (WebElement opcion : lista.findElements(By.tagName("option"))) {
            String texto = opcion.getText().trim();
            if (texto.startsWith(nombre + " (")) {
                return texto;
            }
        }
        return null;
    }

    // ---------- Validacion del diagrama ----------

    protected static final By ADVERTENCIAS = By.xpath(PANEL + "//ul[contains(@class,'items')]/li//span[contains(@class,'sub')]");

    protected static By advertencia(String fragmento) {
        return By.xpath(PANEL + "//ul[contains(@class,'items')]/li//span[contains(@class,'sub') and contains(normalize-space(),"
                + literal(fragmento) + ")]");
    }

    // Abre la pestana de validacion y espera una observacion que contenga ese texto
    protected String esperarAdvertencia(String fragmento) {
        irAPestana("Validación");
        return visible(advertencia(fragmento)).getText().trim();
    }

    // Observaciones actuales. El diagrama y su validacion se recargan juntos despues de cada cambio, asi
    // que basta con haber visto el cambio en otra pestana para que esta lista ya este al dia.
    protected List<String> advertenciasActuales() {
        irAPestana("Validación");
        visible(By.xpath(PANEL + "//h3[normalize-space()='Validación del diagrama']"));
        return driver.findElements(ADVERTENCIAS).stream().map(e -> e.getText().trim()).toList();
    }

    // ---------- Lienzo ----------

    // Texto dibujado en el lienzo (usar nombres de una sola palabra: el lienzo parte los textos largos en lineas)
    protected static By textoEnLienzo(String texto) {
        return By.xpath(LIENZO + "//*[local-name()='text' and normalize-space()=" + literal(texto) + "]");
    }

    // Arcos (Arco_) o flujos de mensaje (Mensaje_) dibujados en el lienzo
    protected int conexionesEnLienzo(String prefijo) {
        return driver.findElements(By.cssSelector(".editor-canvas g.djs-connection[data-element-id^='" + prefijo + "']")).size();
    }

    protected void esperarConexionesEnLienzo(String prefijo, int cantidad) {
        espera.until(d -> conexionesEnLienzo(prefijo) == cantidad);
    }
}
