package desarrollo.web.bizagi2.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

// Extension de JUnit: cuando una prueba falla, guarda una captura del navegador en target/screenshots.
// Se ejecuta antes de @AfterEach, es decir, con el navegador todavia abierto.
class CapturaAlFallar implements AfterTestExecutionCallback {

    private static final Path CARPETA = Path.of("target", "screenshots");

    private final Supplier<WebDriver> navegador;

    CapturaAlFallar(Supplier<WebDriver> navegador) {
        this.navegador = navegador;
    }

    @Override
    public void afterTestExecution(ExtensionContext contexto) {
        WebDriver driver = navegador.get();
        if (contexto.getExecutionException().isEmpty() || !(driver instanceof TakesScreenshot camara)) {
            return;
        }
        String nombre = contexto.getRequiredTestClass().getSimpleName() + "-"
                + contexto.getRequiredTestMethod().getName() + ".png";
        try {
            Files.createDirectories(CARPETA);
            Files.write(CARPETA.resolve(nombre), camara.getScreenshotAs(OutputType.BYTES));
            System.err.println("Captura guardada: " + CARPETA.resolve(nombre) + " (URL: " + driver.getCurrentUrl() + ")");
        } catch (IOException | WebDriverException ex) {
            System.err.println("No se pudo guardar la captura: " + ex.getMessage());
        }
    }
}
