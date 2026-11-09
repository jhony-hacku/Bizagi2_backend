package desarrollo.web.Bizagi2.exception;

// El refresh token no sirve (falta, vencio, fue revocado o reutilizado). Se responde 401 y hay que volver a hacer login.
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
