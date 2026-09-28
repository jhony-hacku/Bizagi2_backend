package desarrollo.web.Bizagi2.exception;

// Se traduce a HTTP 400: datos invalidos o que rompen la coherencia del diagrama
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
