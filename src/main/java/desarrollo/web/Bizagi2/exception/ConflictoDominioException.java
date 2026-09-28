package desarrollo.web.Bizagi2.exception;

// Se traduce a HTTP 409: duplicados o elementos que no se pueden eliminar porque estan en uso
public class ConflictoDominioException extends RuntimeException {

    public ConflictoDominioException(String mensaje) {
        super(mensaje);
    }
}
