package desarrollo.web.Bizagi2.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import desarrollo.web.Bizagi2.dto.ErrorResponse;
import desarrollo.web.Bizagi2.entities.Proceso;

@DisplayName("GlobalExceptionHandler - concurrencia optimista (@Version)")
class GlobalExceptionHandlerConcurrenciaTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Una modificacion concurrente responde 409 sin exponer detalles internos")
    void modificacionConcurrente() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Proceso.class, 10);

        ResponseEntity<ErrorResponse> respuesta = handler.modificacionConcurrente(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().getMessage()).contains("modificado por otro usuario");
    }
}
