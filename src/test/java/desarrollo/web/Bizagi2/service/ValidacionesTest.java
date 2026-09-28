package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import desarrollo.web.Bizagi2.exception.ReglaNegocioException;

@DisplayName("ValidacionesTest - Pruebas de utilidades de validación")
class ValidacionesTest {

    @Test
    @DisplayName("requerido(String) valida presencia de texto")
    void requerido_string_validaCorrectamente() {
        assertThatCode(() -> Validaciones.requerido("Texto válido", "campo"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Validaciones.requerido(null, "campo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El campo 'campo' es obligatorio");

        assertThatThrownBy(() -> Validaciones.requerido("   ", "campo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El campo 'campo' es obligatorio");
    }

    @Test
    @DisplayName("requerido(Object) valida presencia de objeto")
    void requerido_object_validaCorrectamente() {
        assertThatCode(() -> Validaciones.requerido(new Object(), "objeto"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Validaciones.requerido(null, "objeto"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El campo 'objeto' es obligatorio");
    }

    @Test
    @DisplayName("emailValido valida formato de correo electrónico")
    void emailValido_validaFormato() {
        assertThatCode(() -> Validaciones.emailValido("usuario@empresa.com"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Validaciones.emailValido("invalido"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El formato del email no es valido");

        assertThatThrownBy(() -> Validaciones.emailValido("sinarroba.com"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("passwordValido valida longitud mínima de 8 caracteres")
    void passwordValido_validaLongitud() {
        assertThatCode(() -> Validaciones.passwordValido("12345678"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Validaciones.passwordValido("1234567"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 8 caracteres");
    }
}
