package desarrollo.web.bizagi2.e2e;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

// Base de las pruebas E2E: abre un Chrome nuevo para cada prueba y ofrece los pasos comunes.
// La direccion de la aplicacion se pasa con -De2e.baseUrl=https://... (por defecto, el front local).
abstract class BaseE2ETest {

    protected static final String BASE_URL = quitarBarraFinal(System.getProperty("e2e.baseUrl", "http://localhost:4200"));
    protected static final String PASSWORD = "ClaveE2E12345";
    protected static final By BOTON_ENVIAR = By.cssSelector("button[type='submit']");
    protected static final By ERROR_FORMULARIO = By.cssSelector("p.form-error");

    // Staging corre en hardware modesto: se da tiempo de sobra a cada espera
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(40);

    protected WebDriver driver;
    protected WebDriverWait espera;

    // Guarda la pantalla final de cada prueba (pase o falle) y arma el reporte en target/evidencias
    @RegisterExtension
    final Evidencias evidencias = new Evidencias(() -> driver);

    // Datos de una empresa recien registrada, para iniciar sesion despues (el NIT sirve para compartirle procesos)
    protected record Cuenta(String empresa, String email, String password, String nit) {
    }

    // Datos de un usuario invitado a la empresa (HU-02)
    protected record Invitado(String nombre, String email, String password) {
    }

    @BeforeEach
    void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
            opciones.addArguments("--headless=new");
        }
        opciones.addArguments("--window-size=1366,900", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(opciones);
        espera = new WebDriverWait(driver, ESPERA_MAXIMA);
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------- Pasos basicos ----------

    protected void abrir(String ruta) {
        driver.get(BASE_URL + ruta);
    }

    protected WebElement visible(By localizador) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }

    protected void escribir(By localizador, String texto) {
        WebElement campo = visible(localizador);
        campo.clear();
        campo.sendKeys(texto);
    }

    protected void clic(By localizador) {
        try {
            espera.until(ExpectedConditions.elementToBeClickable(localizador)).click();
        } catch (ElementClickInterceptedException tapado) {
            // Las notificaciones salen abajo a la derecha y pueden tapar botones (por ejemplo, el Guardar del
            // panel del editor). Se cierran y se vuelve a intentar.
            cerrarNotificaciones();
            espera.until(ExpectedConditions.elementToBeClickable(localizador)).click();
        }
    }

    // Cierra todas las notificaciones visibles con su boton "x" y espera a que desaparezcan
    protected void cerrarNotificaciones() {
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll(\"section[aria-label='Notificaciones'] .toast button.cerrar\").forEach(b => b.click());");
        esperarQueDesaparezca(By.cssSelector("section[aria-label='Notificaciones'] .toast"));
    }

    // Espera a que el elemento tenga exactamente ese texto (util cuando primero muestra "Cargando...")
    protected void esperarTexto(By localizador, String texto) {
        espera.until(ExpectedConditions.textToBe(localizador, texto));
    }

    // Borra el contenido de un campo tecleando, para que Angular se entere del cambio
    protected void vaciar(By localizador) {
        visible(localizador).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
    }

    protected void seleccionar(By localizador, String valor) {
        new Select(visible(localizador)).selectByValue(valor);
    }

    // Elige una opcion por el texto que ve el usuario. Sirve para las listas cuyas opciones son ids
    // (Angular ngValue) y espera a que la opcion exista, porque muchas se llenan tras consultar al backend.
    protected void seleccionarTexto(By localizador, String texto) {
        espera.until(d -> {
            for (WebElement opcion : d.findElement(localizador).findElements(By.tagName("option"))) {
                if (opcion.getText().trim().equals(texto)) {
                    return true;
                }
            }
            return false;
        });
        new Select(visible(localizador)).selectByVisibleText(texto);
    }

    // Textos de las opciones de una lista desplegable, tal como los ve el usuario
    protected List<String> opciones(By localizador) {
        List<String> textos = new ArrayList<>();
        for (WebElement opcion : visible(localizador).findElements(By.tagName("option"))) {
            textos.add(opcion.getText().trim());
        }
        return textos;
    }

    // Espera una notificacion emergente que contenga ese texto y devuelve su texto completo
    protected String esperarNotificacion(String fragmento) {
        return visible(By.xpath("//section[@aria-label='Notificaciones']//div[contains(@class,'toast')]/p[contains(normalize-space(),"
                + literal(fragmento) + ")]")).getText().trim();
    }

    // Espera a que un campo ya tenga cargado ese valor (los formularios de edicion se llenan tras consultar al backend)
    protected void esperarValor(By localizador, String valor) {
        espera.until(ExpectedConditions.textToBePresentInElementValue(localizador, valor));
    }

    protected void esperarQueDesaparezca(By localizador) {
        espera.until(ExpectedConditions.invisibilityOfElementLocated(localizador));
    }

    // Consulta inmediata, sin esperar: sirve para comprobar que algo NO esta en la pantalla ya cargada
    protected boolean existe(By localizador) {
        return !driver.findElements(localizador).isEmpty();
    }

    protected boolean estaEn(String ruta) {
        return driver.getCurrentUrl().contains(ruta);
    }

    // ---------- Pasos de la aplicacion ----------

    // La lista de procesos es la pantalla de inicio de un usuario autenticado
    protected void esperarPantallaProcesos() {
        espera.until(ExpectedConditions.urlContains("/procesos"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Procesos"));
    }

    protected void esperarPantallaLogin() {
        espera.until(ExpectedConditions.urlContains("/login"));
        visible(By.id("email"));
    }

    // HU-01: registra una empresa nueva con datos unicos y queda con la sesion iniciada.
    // Staging corre en hardware modesto: si el primer envio no lleva a la lista de procesos
    // (peticion lenta, o un formulario que Angular aun no tenia listo y que no envia nada), se repite
    // una vez con datos nuevos. Si vuelve a fallar, el error cuenta que mostraba la pantalla.
    protected Cuenta registrarEmpresa() {
        Cuenta cuenta = enviarRegistro();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.urlContains("/procesos"));
        } catch (TimeoutException primerIntento) {
            String primerMotivo = describirPantalla();
            // El primer registro pudo terminar justo despues de los 15 s (backend recien desplegado y en frio).
            // En ese caso la sesion ya quedo iniciada y la app manda de /register-company a /procesos.
            abrir("/register-company");
            espera.until(d -> estaEn("/procesos") || !d.findElements(By.id("nombreEmpresa")).isEmpty());
            if (estaEn("/procesos")) {
                esperarPantallaProcesos();
                return cuenta;
            }
            cuenta = enviarRegistro();
            try {
                esperarPantallaProcesos();
            } catch (TimeoutException segundoIntento) {
                throw new AssertionError("No se pudo registrar la empresa en dos intentos. Primero: "
                        + primerMotivo + ". Segundo: " + describirPantalla(), segundoIntento);
            }
            return cuenta;
        }
        esperarPantallaProcesos();
        return cuenta;
    }

    // Direccion actual y mensajes de error visibles (del formulario o de sus campos), para diagnosticar un fallo
    private String describirPantalla() {
        List<String> errores = new ArrayList<>();
        for (WebElement error : driver.findElements(By.cssSelector("p.form-error, span.field-error"))) {
            errores.add(error.getText().trim());
        }
        return "url=" + driver.getCurrentUrl()
                + (errores.isEmpty() ? ", sin mensajes de error en pantalla" : ", errores en pantalla=" + errores);
    }

    private Cuenta enviarRegistro() {
        String id = identificadorUnico();
        Cuenta cuenta = new Cuenta("Empresa E2E " + id, "admin." + id + "@example.com", PASSWORD, "E2E-" + id);

        abrir("/register-company");
        escribir(By.id("nombreEmpresa"), cuenta.empresa());
        escribir(By.id("nit"), cuenta.nit());
        escribir(By.id("emailContacto"), "contacto." + id + "@example.com");
        escribir(By.id("nombre"), "Admin E2E");
        escribir(By.id("email"), cuenta.email());
        escribir(By.id("password"), cuenta.password());
        escribir(By.id("confirmPassword"), cuenta.password());
        clic(BOTON_ENVIAR);
        return cuenta;
    }

    protected void iniciarSesion(String email, String password) {
        escribir(By.id("email"), email);
        escribir(By.id("password"), password);
        clic(BOTON_ENVIAR);
    }

    // HU-03: abre el menu del perfil y cierra la sesion
    protected void cerrarSesion() {
        clic(By.cssSelector("button.boton-perfil"));
        clic(By.cssSelector("button.salir"));
        esperarPantallaLogin();
    }

    // ---------- Procesos (HU-04 a HU-07) ----------

    protected static By filaProceso(String nombre) {
        return By.xpath(xpathFilaProceso(nombre));
    }

    // Un elemento dentro de la fila de un proceso, por ejemplo el enlace "Editar" o la etiqueta de estado
    protected static By enFilaProceso(String nombre, String xpathRelativo) {
        return By.xpath(xpathFilaProceso(nombre) + xpathRelativo);
    }

    protected static By estadoProceso(String nombre) {
        return enFilaProceso(nombre, "//span[contains(@class,'badge')]");
    }

    protected static By accionProceso(String nombre, String texto) {
        return enFilaProceso(nombre, "//*[self::a or self::button][normalize-space()='" + texto + "']");
    }

    // HU-04: crea un proceso desde la lista y espera a verlo en ella
    protected void crearProceso(String nombre, String categoria) {
        if (!driver.getCurrentUrl().endsWith("/procesos")) {
            abrir("/procesos");
            esperarPantallaProcesos();
        }
        clic(By.linkText("Nuevo proceso"));
        escribir(By.id("nombre"), nombre);
        escribir(By.id("descripcion"), "Proceso creado por una prueba E2E");
        escribir(By.id("categoria"), categoria);
        clic(BOTON_ENVIAR);

        esperarPantallaProcesos();
        visible(filaProceso(nombre));
    }

    // HU-07: abre el historial del proceso y espera a que cargue
    protected void abrirHistorial(String nombre) {
        clic(accionProceso(nombre, "Historial"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Historial \u00b7 " + nombre));
    }

    // Una entrada del historial con esa accion (CREAR, EDITAR, ELIMINAR...)
    protected static By entradaHistorial(String accion) {
        return By.xpath("//article[.//span[contains(@class,'accion') and normalize-space()='" + accion + "']]");
    }

    // Una entrada del historial con esa accion y cuyo detalle contiene el texto
    protected static By entradaHistorial(String accion, String detalle) {
        return By.xpath("//article[.//span[contains(@class,'accion') and normalize-space()='" + accion + "']"
                + " and .//p[contains(normalize-space()," + literal(detalle) + ")]]");
    }

    // Va a la lista de procesos y abre el historial de ese proceso
    protected void verHistorialProceso(String nombre) {
        abrir("/procesos");
        esperarPantallaProcesos();
        abrirHistorial(nombre);
    }

    protected void volverAProcesos() {
        clic(By.linkText("Procesos"));
        esperarPantallaProcesos();
    }

    // ---------- Dialogo de confirmacion ----------

    protected static final By DIALOGO = By.cssSelector("[role='alertdialog']");

    protected void confirmarDialogo() {
        clic(By.cssSelector("[role='alertdialog'] button.confirmar"));
        esperarQueDesaparezca(DIALOGO);
    }

    protected void cancelarDialogo() {
        clic(By.cssSelector("[role='alertdialog'] button.cancelar"));
        esperarQueDesaparezca(DIALOGO);
    }

    // ---------- Usuarios de la empresa (HU-02) ----------

    protected static By filaUsuario(String email) {
        return By.xpath(xpathFilaUsuario(email));
    }

    protected static By enFilaUsuario(String email, String xpathRelativo) {
        return By.xpath(xpathFilaUsuario(email) + xpathRelativo);
    }

    protected void abrirUsuarios() {
        clic(By.linkText("Usuarios"));
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Usuarios"));
    }

    // HU-02: el administrador invita a un usuario con un rol de acceso (ADMINISTRADOR, EDITOR o LECTOR)
    protected Invitado invitarUsuario(String rolAcceso) {
        String id = identificadorUnico();
        Invitado invitado = new Invitado("Usuario E2E " + id, "usuario." + id + "@example.com", PASSWORD);

        abrirUsuarios();
        clic(By.xpath("//button[normalize-space()='Invitar usuario']"));
        escribir(By.id("nombre"), invitado.nombre());
        escribir(By.id("email"), invitado.email());
        seleccionar(By.id("rolAcceso"), rolAcceso);
        escribir(By.id("password"), invitado.password());
        clic(BOTON_ENVIAR);

        visible(filaUsuario(invitado.email()));
        return invitado;
    }

    // ---------- Roles de proceso (HU-17 a HU-20) ----------

    protected static By filaRol(String nombre) {
        return By.xpath(xpathFilaProceso(nombre));
    }

    protected static By enFilaRol(String nombre, String xpathRelativo) {
        return By.xpath(xpathFilaProceso(nombre) + xpathRelativo);
    }

    protected static By accionRol(String nombre, String texto) {
        return enFilaRol(nombre, "//button[normalize-space()='" + texto + "']");
    }

    // Se abre por direccion: asi tambien funciona desde el editor, que no tiene el menu principal
    protected void abrirRoles() {
        abrir("/roles");
        espera.until(ExpectedConditions.textToBe(By.cssSelector("main h1"), "Roles de proceso"));
    }

    // HU-17: el administrador crea un rol de proceso y espera a verlo en la lista
    protected void crearRol(String nombre, String descripcion) {
        abrirRoles();
        clic(By.xpath("//button[normalize-space()='Nuevo rol']"));
        escribir(By.id("nombre"), nombre);
        escribir(By.id("descripcion"), descripcion);
        clic(BOTON_ENVIAR);
        visible(filaRol(nombre));
    }

    private static String xpathFilaProceso(String nombre) {
        return "//tr[td/strong[normalize-space()='" + nombre + "']]";
    }

    private static String xpathFilaUsuario(String email) {
        return "//tr[td/span[contains(@class,'sub') and normalize-space()='" + email + "']]";
    }

    // Texto entre comillas para XPath (admite textos con comillas simples)
    protected static String literal(String texto) {
        if (!texto.contains("'")) {
            return "'" + texto + "'";
        }
        return "concat('" + texto.replace("'", "', \"'\", '") + "')";
    }

    protected static String identificadorUnico() {
        return Long.toString(System.currentTimeMillis(), 36) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
