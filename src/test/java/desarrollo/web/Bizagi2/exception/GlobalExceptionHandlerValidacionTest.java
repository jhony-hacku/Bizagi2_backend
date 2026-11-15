package desarrollo.web.Bizagi2.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import desarrollo.web.Bizagi2.dto.ErrorResponse;

@DisplayName("GlobalExceptionHandler - errores de @Valid en los DTOs")
class GlobalExceptionHandlerValidacionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Responde 400 con el primer mensaje de cada campo invalido")
    void validacion() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "datos");
        resultado.addError(new FieldError("datos", "email", "El email no es valido"));
        resultado.addError(new FieldError("datos", "email", "El email es obligatorio"));
        resultado.addError(new FieldError("datos", "nombre", "El nombre es obligatorio"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(resultado);

        ResponseEntity<ErrorResponse> respuesta = handler.validacion(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getErrores())
                .containsEntry("email", "El email no es valido")
                .containsEntry("nombre", "El nombre es obligatorio")
                .hasSize(2);
        assertThat(respuesta.getBody().getMessage()).isEqualTo("La solicitud tiene campos invalidos");
    }
}
