package desarrollo.web.Bizagi2.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import desarrollo.web.Bizagi2.dto.auth.AuthResponse;
import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.AuthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - HU-01 registro y HU-03 inicio/cierre de sesion")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("POST /api/auth/register responde 201 con el token")
    void registrar() {
        RegistroRequest datos = new RegistroRequest();
        AuthResponse token = new AuthResponse();
        when(authService.registrar(datos)).thenReturn(token);

        ResponseEntity<AuthResponse> respuesta = controller.registrar(datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(token);
    }

    @Test
    @DisplayName("POST /api/auth/login responde 200 con el token")
    void login() {
        LoginRequest datos = new LoginRequest();
        AuthResponse token = new AuthResponse();
        when(authService.login(datos)).thenReturn(token);

        ResponseEntity<AuthResponse> respuesta = controller.login(datos);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(token);
    }

    @Test
    @DisplayName("POST /api/auth/logout responde 204 e invalida la sesion")
    void logout() {
        Usuario usuario = UsuarioDePrueba.administrador();

        ResponseEntity<Void> respuesta = controller.logout(usuario);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout(usuario);
    }
}
