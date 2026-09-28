package desarrollo.web.Bizagi2.exception;

import java.util.Map;

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
        ResponseEntity<Map<String, Object>> response = handler.noEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Usuario no encontrado", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("ConflictoDominioException -> 409 CONFLICT")
    void testConflictoDominio() {
        ConflictoDominioException ex = new ConflictoDominioException("El correo ya existe");
        ResponseEntity<Map<String, Object>> response = handler.conflicto(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("El correo ya existe", response.getBody().get("message"));
    }

    @Test
    @DisplayName("ReglaNegocioException -> 400 BAD_REQUEST")
    void testReglaNegocio() {
        ReglaNegocioException ex = new ReglaNegocioException("La actividad debe tener un nombre unico");
        ResponseEntity<Map<String, Object>> response = handler.reglaNegocio(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("La actividad debe tener un nombre unico", response.getBody().get("message"));
    }

    @Test
    @DisplayName("BadCredentialsException -> 401 UNAUTHORIZED")
    void testCredencialesInvalidas() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Map<String, Object>> response = handler.credencialesInvalidas(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("Credenciales invalidas", response.getBody().get("message"));
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN")
    void testSinPermiso() {
        AccessDeniedException ex = new AccessDeniedException("Acceso denegado");
        ResponseEntity<Map<String, Object>> response = handler.sinPermiso(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Acceso denegado", response.getBody().get("message"));

        // Sin mensaje especificado
        AccessDeniedException exSinMensaje = new AccessDeniedException(null);
        ResponseEntity<Map<String, Object>> response2 = handler.sinPermiso(exSinMensaje);
        assertEquals("No tienes permisos para realizar esta accion", response2.getBody().get("message"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 BAD_REQUEST")
    void testJsonInvalido() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);
        ResponseEntity<Map<String, Object>> response = handler.jsonInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("El cuerpo de la solicitud no es un JSON valido", response.getBody().get("message"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException -> 400 BAD_REQUEST")
    void testParametroInvalido() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);
        ResponseEntity<Map<String, Object>> response = handler.parametroInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Valor invalido para el parametro 'id'", response.getBody().get("message"));
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException -> 405 METHOD_NOT_ALLOWED")
    void testMetodoNoPermitido() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<Map<String, Object>> response = handler.metodoNoPermitido(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().get("status"));
        assertEquals("Metodo HTTP no permitido para esta ruta", response.getBody().get("message"));
    }

    @Test
    @DisplayName("NoResourceFoundException -> 404 NOT_FOUND")
    void testRutaNoExiste() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/ruta/inexistente", "/ruta/inexistente");
        ResponseEntity<Map<String, Object>> response = handler.rutaNoExiste(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("La ruta solicitada no existe", response.getBody().get("message"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 CONFLICT")
    void testIntegridad() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");
        ResponseEntity<Map<String, Object>> response = handler.integridad(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("La operacion viola una restriccion de integridad de datos", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Exception generica -> 500 INTERNAL_SERVER_ERROR")
    void testInesperado() {
        Exception ex = new RuntimeException("Error fatal no controlado");
        ResponseEntity<Map<String, Object>> response = handler.inesperado(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("No fue posible procesar la solicitud", response.getBody().get("message"));
    }
}
