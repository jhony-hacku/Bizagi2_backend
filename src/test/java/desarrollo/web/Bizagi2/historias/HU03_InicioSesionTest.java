package desarrollo.web.Bizagi2.historias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.security.JwtService;
import desarrollo.web.Bizagi2.service.AuthService;
import desarrollo.web.Bizagi2.service.EmpresaService;
import desarrollo.web.Bizagi2.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-03: Inicio de sesión")
class HU03_InicioSesionTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("CA1 & CA2: Autenticación exitosa validando email y contraseña cifrada con PasswordEncoder")
    void ca1_ca2_autenticar_exitoso() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("test@empresa.com");
        usuario.setPassword("$2a$10$encodedHashHere");
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail("test@empresa.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$encodedHashHere")).thenReturn(true);

        Usuario resultado = usuarioService.autenticar("test@empresa.com", "Password123");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("test@empresa.com");
        verify(passwordEncoder).matches("Password123", "$2a$10$encodedHashHere");
    }

    @Test
    @DisplayName("CA3: Login devuelve token JWT y asocia explícitamente el usuario con su empresa")
    void ca3_login_retornaTokenYDatosEmpresa() {
        AuthService authService = new AuthService(empresaService, usuarioService, jwtService);

        Empresa empresa = new Empresa();
        empresa.setId(5);
        empresa.setNombre("Empresa Logistica");

        Usuario usuario = new Usuario();
        usuario.setId(12);
        usuario.setNombre("Laura");
        usuario.setEmail("laura@logistica.com");
        usuario.setPassword("$2a$10$encoded");
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findByEmail("laura@logistica.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveSecreta1", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("jwt.token.valido");

        Map<String, Object> respuesta = authService.login("laura@logistica.com", "ClaveSecreta1");

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.get("token")).isEqualTo("jwt.token.valido");
        assertThat(respuesta.get("usuarioId")).isEqualTo(12);
        assertThat(respuesta.get("empresaId")).isEqualTo(5);
        assertThat(respuesta.get("empresa")).isEqualTo("Empresa Logistica");
        assertThat(respuesta.get("rolAcceso")).isEqualTo(RolAcceso.EDITOR);
    }

    @Test
    @DisplayName("CA4: Intento fallido con correo no registrado muestra mensaje genérico sin revelar si el correo existe")
    void ca4_autenticar_correoNoExiste_lanzaMensajeGenerico() {
        when(usuarioRepository.findByEmail("inexistente@empresa.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.autenticar("inexistente@empresa.com", "CualquierPass"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("CA4: Intento fallido con contraseña incorrecta muestra el mismo mensaje genérico")
    void ca4_autenticar_passwordIncorrecta_lanzaMensajeGenerico() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@empresa.com");
        usuario.setPassword("$2a$10$encoded");
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail("test@empresa.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("PassEquivocada", "$2a$10$encoded")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.autenticar("test@empresa.com", "PassEquivocada"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("CA4: Usuario inactivo no puede autenticarse y recibe mensaje genérico")
    void ca4_autenticar_usuarioInactivo_lanzaMensajeGenerico() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@empresa.com");
        usuario.setPassword("$2a$10$encoded");
        usuario.setActivo(false);

        when(usuarioRepository.findByEmail("test@empresa.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.autenticar("test@empresa.com", "Password123"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("CA5: Cerrar sesión incrementa la versión de sesión invalidando tokens previos")
    void ca5_cerrarSesion_incrementaVersionSesion() {
        Usuario usuario = new Usuario();
        usuario.setId(7);
        usuario.setVersionSesion(2);

        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        usuarioService.cerrarSesion(7);

        assertThat(usuario.getVersionSesion()).isEqualTo(3);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("CA1: Validación de campos obligatorios en login")
    void ca1_autenticar_camposVacios_lanzaReglaNegocio() {
        assertThatThrownBy(() -> usuarioService.autenticar("", "password"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("email");

        assertThatThrownBy(() -> usuarioService.autenticar("email@test.com", null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("password");
    }
}
