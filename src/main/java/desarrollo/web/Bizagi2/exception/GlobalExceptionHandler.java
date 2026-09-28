package desarrollo.web.Bizagi2.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

// Todas las excepciones se traducen aqui a un JSON uniforme: { status, message, timestamp }
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoDominioException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoDominioException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credencialesInvalidas(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> sinPermiso(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "No tienes permisos para realizar esta accion");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Valor invalido para el parametro '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no permitido para esta ruta");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaNoExiste(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "La ruta solicitada no existe");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos", ex);
        return error(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de datos");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        // El detalle tecnico solo se registra en el servidor, nunca se envia al cliente
        log.error("Error interno", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("message", mensaje);
        cuerpo.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(cuerpo);
    }
}
