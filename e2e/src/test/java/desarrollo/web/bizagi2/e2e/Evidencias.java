package desarrollo.web.bizagi2.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

// Extension de JUnit que deja evidencia de cada prueba, pase o falle:
//   target/evidencias/capturas/<Clase>-<metodo>.png  pantalla final de la prueba
//   target/evidencias/index.html                     reporte para abrir en el navegador
//   target/evidencias/resumen.md                     tabla para el resumen del pipeline
// Se ejecuta antes de @AfterEach, es decir, con el navegador todavia abierto.
class Evidencias implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Path CARPETA = Path.of("target", "evidencias");
    private static final Path CAPTURAS = CARPETA.resolve("capturas");
    private static final ExtensionContext.Namespace ESPACIO = ExtensionContext.Namespace.create(Evidencias.class);
    private static final String INICIO = "inicio";

    // Todas las clases de prueba corren en la misma JVM: la lista acumula los resultados de la corrida
    private static final List<Resultado> RESULTADOS = new ArrayList<>();
    private static final String FECHA = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    private record Resultado(String historia, String prueba, boolean ok, long milisegundos, String captura,
            String url, String error) {
    }

    private final Supplier<WebDriver> navegador;

    Evidencias(Supplier<WebDriver> navegador) {
        this.navegador = navegador;
    }

    @Override
    public void beforeTestExecution(ExtensionContext contexto) {
        contexto.getStore(ESPACIO).put(INICIO, System.nanoTime());
    }

    @Override
    public void afterTestExecution(ExtensionContext contexto) {
        long inicio = contexto.getStore(ESPACIO).getOrDefault(INICIO, Long.class, System.nanoTime());
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        Optional<Throwable> fallo = contexto.getExecutionException();

        String archivo = contexto.getRequiredTestClass().getSimpleName() + "-"
                + contexto.getRequiredTestMethod().getName() + ".png";
        String captura = null;
        String url = "";
        WebDriver driver = navegador.get();
        if (driver instanceof TakesScreenshot camara) {
            try {
                Files.createDirectories(CAPTURAS);
                Files.write(CAPTURAS.resolve(archivo), camara.getScreenshotAs(OutputType.BYTES));
                captura = "capturas/" + archivo;
                url = driver.getCurrentUrl();
            } catch (IOException | WebDriverException ex) {
                System.err.println("No se pudo guardar la captura: " + ex.getMessage());
            }
        }

        String historia = contexto.getParent().map(ExtensionContext::getDisplayName)
                .orElse(contexto.getRequiredTestClass().getSimpleName());
        String error = fallo.map(Evidencias::describir).orElse("");
        if (fallo.isPresent()) {
            System.err.println("Prueba fallida. Captura: " + CARPETA.resolve(captura == null ? "" : captura)
                    + " (URL: " + url + ")");
        }

        synchronized (RESULTADOS) {
            RESULTADOS.add(new Resultado(historia, contexto.getDisplayName(), fallo.isEmpty(), milisegundos,
                    captura, url, error));
            escribirReportes();
        }
    }

    // ---------- Reportes ----------

    // Se reescriben tras cada prueba: asi quedan completos aunque la corrida se interrumpa
    private static void escribirReportes() {
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(CARPETA.resolve("index.html"), html(), StandardCharsets.UTF_8);
            Files.writeString(CARPETA.resolve("resumen.md"), markdown(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("No se pudo escribir el reporte de evidencias: " + ex.getMessage());
        }
    }

    private static Map<String, List<Resultado>> porHistoria() {
        Map<String, List<Resultado>> grupos = new TreeMap<>();
        for (Resultado r : RESULTADOS) {
            grupos.computeIfAbsent(r.historia(), k -> new ArrayList<>()).add(r);
        }
        return grupos;
    }

    private static long fallidas() {
        return RESULTADOS.stream().filter(r -> !r.ok()).count();
    }

    private static String markdown() {
        StringBuilder md = new StringBuilder();
        md.append("### Pruebas E2E con Selenium\n\n");
        md.append(RESULTADOS.size()).append(" pruebas, ").append(RESULTADOS.size() - fallidas())
                .append(" aprobadas, ").append(fallidas()).append(" fallidas\n\n");
        md.append("| Historia | Prueba | Resultado | Tiempo |\n|---|---|---|---|\n");
        porHistoria().forEach((historia, pruebas) -> {
            for (Resultado r : pruebas) {
                md.append("| ").append(celda(historia)).append(" | ").append(celda(r.prueba())).append(" | ")
                        .append(r.ok() ? "Aprobada" : "FALLIDA").append(" | ").append(segundos(r)).append(" |\n");
            }
        });
        return md.toString();
    }

    private static String html() {
        long fallidas = fallidas();
        StringBuilder cuerpo = new StringBuilder();
        porHistoria().forEach((historia, pruebas) -> {
            cuerpo.append("<section><h2>").append(esc(historia)).append("</h2><div class=\"pruebas\">\n");
            for (Resultado r : pruebas) {
                cuerpo.append("<article class=\"").append(r.ok() ? "ok" : "fallo").append("\">");
                if (r.captura() != null) {
                    cuerpo.append("<a class=\"captura\" href=\"").append(esc(r.captura()))
                            .append("\" target=\"_blank\"><img loading=\"lazy\" src=\"").append(esc(r.captura()))
                            .append("\" alt=\"Pantalla final de la prueba\"></a>");
                }
                cuerpo.append("<div class=\"datos\"><span class=\"estado\">")
                        .append(r.ok() ? "Aprobada" : "Fallida").append("</span><span class=\"tiempo\">")
                        .append(segundos(r)).append("</span><h3>").append(esc(r.prueba())).append("</h3>");
                if (!r.url().isEmpty()) {
                    cuerpo.append("<p class=\"url\">").append(esc(r.url())).append("</p>");
                }
                if (!r.error().isEmpty()) {
                    cuerpo.append("<pre>").append(esc(r.error())).append("</pre>");
                }
                cuerpo.append("</div></article>\n");
            }
            cuerpo.append("</div></section>\n");
        });

        return PLANTILLA
                .replace("{{RESUMEN}}", RESULTADOS.size() + " pruebas · " + (RESULTADOS.size() - fallidas)
                        + " aprobadas · " + fallidas + " fallidas")
                .replace("{{CLASE_RESUMEN}}", fallidas == 0 ? "ok" : "fallo")
                .replace("{{FECHA}}", esc(FECHA))
                .replace("{{APP}}", esc(System.getProperty("e2e.baseUrl", "http://localhost:4200")))
                .replace("{{CUERPO}}", cuerpo.toString());
    }

    private static String describir(Throwable fallo) {
        String mensaje = fallo.getMessage() == null ? "" : fallo.getMessage().strip();
        // El mensaje de Selenium trae muchas lineas de diagnostico: basta con la primera
        int salto = mensaje.indexOf('\n');
        if (salto > 0) {
            mensaje = mensaje.substring(0, salto);
        }
        if (mensaje.length() > 400) {
            mensaje = mensaje.substring(0, 400) + "...";
        }
        return fallo.getClass().getSimpleName() + (mensaje.isEmpty() ? "" : ": " + mensaje);
    }

    private static String segundos(Resultado r) {
        return String.format(java.util.Locale.ROOT, "%.1f s", r.milisegundos() / 1000.0);
    }

    private static String esc(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String celda(String texto) {
        return texto.replace("|", "\\|");
    }

    private static final String PLANTILLA = """
            <!doctype html>
            <html lang="es">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Evidencias E2E - Bizagi2</title>
            <style>
              :root { --fondo:#f6f7f9; --tarjeta:#fff; --texto:#1c2330; --suave:#5d6777; --borde:#dfe3ea;
                      --ok:#1a7f4b; --ok-fondo:#e6f4ec; --fallo:#b3261e; --fallo-fondo:#fbe9e7; }
              @media (prefers-color-scheme: dark) {
                :root { --fondo:#14181f; --tarjeta:#1d232d; --texto:#e7ebf1; --suave:#9aa5b5; --borde:#2f3846;
                        --ok:#5fd08f; --ok-fondo:#17352a; --fallo:#ff8a80; --fallo-fondo:#3d1f1d; }
              }
              * { box-sizing: border-box; }
              body { margin:0; padding:24px 16px 48px; background:var(--fondo); color:var(--texto);
                     font:15px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
              main { max-width:1100px; margin:0 auto; }
              h1 { margin:0 0 4px; font-size:26px; }
              .meta { margin:0 0 16px; color:var(--suave); overflow-wrap:anywhere; }
              .resumen { display:inline-block; margin:0 0 8px; padding:6px 14px; border-radius:999px; font-weight:600; }
              .resumen.ok { background:var(--ok-fondo); color:var(--ok); }
              .resumen.fallo { background:var(--fallo-fondo); color:var(--fallo); }
              h2 { margin:32px 0 12px; font-size:18px; border-bottom:1px solid var(--borde); padding-bottom:6px; }
              .pruebas { display:grid; gap:16px; grid-template-columns:repeat(auto-fill, minmax(300px, 1fr)); }
              article { background:var(--tarjeta); border:1px solid var(--borde); border-radius:10px; overflow:hidden;
                        border-top:4px solid var(--ok); }
              article.fallo { border-top-color:var(--fallo); }
              .captura { display:block; background:var(--fondo); border-bottom:1px solid var(--borde); }
              .captura img { display:block; width:100%; height:auto; }
              .datos { padding:12px 14px 14px; }
              .estado { font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:.04em; color:var(--ok); }
              article.fallo .estado { color:var(--fallo); }
              .tiempo { float:right; font-size:13px; color:var(--suave); }
              h3 { margin:6px 0 0; font-size:15px; font-weight:600; }
              .url { margin:6px 0 0; font-size:12px; color:var(--suave); overflow-wrap:anywhere; }
              pre { margin:10px 0 0; padding:8px 10px; border-radius:6px; background:var(--fallo-fondo); color:var(--fallo);
                    font-size:12px; white-space:pre-wrap; overflow-wrap:anywhere; }
            </style>
            </head>
            <body>
            <main>
            <h1>Evidencias de pruebas E2E</h1>
            <p class="meta">Bizagi2 · Selenium · {{FECHA}} · {{APP}}</p>
            <p class="resumen {{CLASE_RESUMEN}}">{{RESUMEN}}</p>
            {{CUERPO}}
            </main>
            </body>
            </html>
            """;
}
