package desarrollo.web.Bizagi2.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

// Las mismas reglas que dispara @Valid en el controlador, probadas sin levantar Spring
@DisplayName("Validaciones declarativas de los DTOs")
class DtoValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    private static <T> Set<String> camposInvalidos(T dto) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(dto);
        return violaciones.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("Registro valido no tiene errores")
    void registroValido() {
        RegistroRequest ok = new RegistroRequest("Acme SAS", "900123456", "contacto@acme.com",
                "Ana Perez", "ana@acme.com", "Clave12345");
        assertThat(camposInvalidos(ok)).isEmpty();
    }

    @Test
    @DisplayName("Registro vacio reporta todos los campos obligatorios")
    void registroVacio() {
        assertThat(camposInvalidos(new RegistroRequest()))
                .containsExactlyInAnyOrder("nombreEmpresa", "nit", "emailContacto", "nombre", "email", "password");
    }

    @Test
    @DisplayName("Email con formato invalido se rechaza")
    void emailInvalido() {
        assertThat(camposInvalidos(new LoginRequest("no-es-un-correo", "x"))).containsExactly("email");
    }

    @Test
    @DisplayName("Crear usuario sin rol se rechaza")
    void usuarioSinRol() {
        assertThat(camposInvalidos(new UsuarioCreateRequest("Luis", "luis@acme.com", "Clave12345", null)))
                .containsExactly("rolAcceso");
    }
}
