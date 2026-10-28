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
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import desarrollo.web.Bizagi2.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

// Todas las excepciones se traducen aqui a un JSON uniforme (ErrorResponse): { status, message, timestamp }.
// Los errores de @Valid agregan "errores": { campo: mensaje }
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoDominioException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoDominioException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> credencialesInvalidas(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> sinPermiso(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "No tienes permisos para realizar esta accion");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Valor invalido para el parametro '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no permitido para esta ruta");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaNoExiste(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "La ruta solicitada no existe");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos", ex);
        return error(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de datos");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        // El detalle tecnico solo se registra en el servidor, nunca se envia al cliente
        log.error("Error interno", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud");
    }

    // @Valid sobre un DTO fallo: se responde 400 con el mensaje de cada campo invalido
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ErrorResponse cuerpo = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "La solicitud tiene campos invalidos", Instant.now().toString(), errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String mensaje) {
        ErrorResponse cuerpo = new ErrorResponse(status.value(), mensaje, Instant.now().toString(), null);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
