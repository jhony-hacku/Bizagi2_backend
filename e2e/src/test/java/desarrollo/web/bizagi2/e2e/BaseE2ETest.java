package desarrollo.web.bizagi2.e2e;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
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

    // Si la prueba falla, guarda una captura de pantalla en target/screenshots antes de cerrar el navegador
    @RegisterExtension
    final CapturaAlFallar capturaAlFallar = new CapturaAlFallar(() -> driver);

    // Datos de una empresa recien registrada, para iniciar sesion despues
    protected record Cuenta(String empresa, String email, String password) {
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
        espera.until(ExpectedConditions.elementToBeClickable(localizador)).click();
    }

    // Espera a que el elemento tenga exactamente ese texto (util cuando primero muestra "Cargando...")
    protected void esperarTexto(By localizador, String texto) {
        espera.until(ExpectedConditions.textToBe(localizador, texto));
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

    // HU-01: registra una empresa nueva con datos unicos y queda con la sesion iniciada
    protected Cuenta registrarEmpresa() {
        String id = identificadorUnico();
        Cuenta cuenta = new Cuenta("Empresa E2E " + id, "admin." + id + "@example.com", PASSWORD);

        abrir("/register-company");
        escribir(By.id("nombreEmpresa"), cuenta.empresa());
        escribir(By.id("nit"), "E2E-" + id);
        escribir(By.id("emailContacto"), "contacto." + id + "@example.com");
        escribir(By.id("nombre"), "Admin E2E");
        escribir(By.id("email"), cuenta.email());
        escribir(By.id("password"), cuenta.password());
        escribir(By.id("confirmPassword"), cuenta.password());
        clic(BOTON_ENVIAR);

        esperarPantallaProcesos();
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

    protected static String identificadorUnico() {
        return Long.toString(System.currentTimeMillis(), 36) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
