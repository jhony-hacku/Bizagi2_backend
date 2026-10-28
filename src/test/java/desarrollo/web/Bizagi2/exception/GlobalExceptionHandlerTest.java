package desarrollo.web.Bizagi2.exception;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import desarrollo.web.Bizagi2.dto.ErrorResponse;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("RecursoNoEncontradoException -> 404 NOT_FOUND")
    void testRecursoNoEncontrado() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Usuario no encontrado");
        ResponseEntity<ErrorResponse> response = handler.noEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Usuario no encontrado", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("ConflictoDominioException -> 409 CONFLICT")
    void testConflictoDominio() {
        ConflictoDominioException ex = new ConflictoDominioException("El correo ya existe");
        ResponseEntity<ErrorResponse> response = handler.conflicto(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("El correo ya existe", response.getBody().getMessage());
    }

    @Test
    @DisplayName("ReglaNegocioException -> 400 BAD_REQUEST")
    void testReglaNegocio() {
        ReglaNegocioException ex = new ReglaNegocioException("La actividad debe tener un nombre unico");
        ResponseEntity<ErrorResponse> response = handler.reglaNegocio(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("La actividad debe tener un nombre unico", response.getBody().getMessage());
    }

    @Test
    @DisplayName("BadCredentialsException -> 401 UNAUTHORIZED")
    void testCredencialesInvalidas() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = handler.credencialesInvalidas(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Credenciales invalidas", response.getBody().getMessage());
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN")
    void testSinPermiso() {
        AccessDeniedException ex = new AccessDeniedException("Acceso denegado");
        ResponseEntity<ErrorResponse> response = handler.sinPermiso(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Acceso denegado", response.getBody().getMessage());

        // Sin mensaje especificado
        AccessDeniedException exSinMensaje = new AccessDeniedException(null);
        ResponseEntity<ErrorResponse> response2 = handler.sinPermiso(exSinMensaje);
        assertEquals("No tienes permisos para realizar esta accion", response2.getBody().getMessage());
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 BAD_REQUEST")
    void testJsonInvalido() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);
        ResponseEntity<ErrorResponse> response = handler.jsonInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("El cuerpo de la solicitud no es un JSON valido", response.getBody().getMessage());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException -> 400 BAD_REQUEST")
    void testParametroInvalido() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);
        ResponseEntity<ErrorResponse> response = handler.parametroInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Valor invalido para el parametro 'id'", response.getBody().getMessage());
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException -> 405 METHOD_NOT_ALLOWED")
    void testMetodoNoPermitido() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ErrorResponse> response = handler.metodoNoPermitido(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().getStatus());
        assertEquals("Metodo HTTP no permitido para esta ruta", response.getBody().getMessage());
    }

    @Test
    @DisplayName("NoResourceFoundException -> 404 NOT_FOUND")
    void testRutaNoExiste() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/ruta/inexistente", "/ruta/inexistente");
        ResponseEntity<ErrorResponse> response = handler.rutaNoExiste(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("La ruta solicitada no existe", response.getBody().getMessage());
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 CONFLICT")
    void testIntegridad() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");
        ResponseEntity<ErrorResponse> response = handler.integridad(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("La operacion viola una restriccion de integridad de datos", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Exception generica -> 500 INTERNAL_SERVER_ERROR")
    void testInesperado() {
        Exception ex = new RuntimeException("Error fatal no controlado");
        ResponseEntity<ErrorResponse> response = handler.inesperado(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("No fue posible procesar la solicitud", response.getBody().getMessage());
    }
}
