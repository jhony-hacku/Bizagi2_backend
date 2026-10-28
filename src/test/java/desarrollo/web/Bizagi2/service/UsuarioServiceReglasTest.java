package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - HU-02: modificar usuarios de la empresa")
class UsuarioServiceReglasTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpresaService empresaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario editor;

    @BeforeEach
    void setUp() {
        editor = DatosDto.usuario();
        editor.setId(2);
        editor.setRolAcceso(RolAcceso.EDITOR);
        editor.setEmail("luis@acme.com");
    }

    private Usuario cambios(String nombre, String email, String password, RolAcceso rol) {
        Usuario datos = new Usuario();
        datos.setNombre(nombre);
        datos.setEmail(email);
        datos.setPassword(password);
        datos.setRolAcceso(rol);
        return datos;
    }

    private void usuarioExistente(Usuario usuario) {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("buscarPorId: inexistente o de otra empresa responde no encontrado")
    void buscarPorIdNoEncontrado() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(editor));

        assertThatThrownBy(() -> usuarioService.buscarPorId(99, EMPRESA_ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> usuarioService.buscarPorId(2, 8)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambia nombre, email (en minusculas), contrasena y rol")
    void actualizarTodo() {
        usuarioExistente(editor);
        when(usuarioRepository.existsByEmail("luis.m@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("$hash");

        Usuario editado = usuarioService.actualizar(2, EMPRESA_ID,
                cambios(" Luis M ", " Luis.M@acme.com ", "NuevaClave123", RolAcceso.LECTOR));

        assertThat(editado.getNombre()).isEqualTo("Luis M");
        assertThat(editado.getEmail()).isEqualTo("luis.m@acme.com");
        assertThat(editado.getPassword()).isEqualTo("$hash");
        assertThat(editado.getRolAcceso()).isEqualTo(RolAcceso.LECTOR);
    }

    @Test
    @DisplayName("Los campos vacios no se modifican")
    void actualizarSinCambios() {
        usuarioExistente(editor);

        Usuario editado = usuarioService.actualizar(2, EMPRESA_ID, cambios(" ", "", null, RolAcceso.EDITOR));

        assertThat(editado.getNombre()).isEqualTo("Ana Perez");
        assertThat(editado.getEmail()).isEqualTo("luis@acme.com");
    }

    @Test
    @DisplayName("No se puede usar el email de otro usuario")
    void emailDuplicado() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(editor));
        when(usuarioRepository.existsByEmail("ana@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(2, EMPRESA_ID, cambios(null, "ana@acme.com", null, null)))
                .isInstanceOf(ConflictoDominioException.class);
    }

    @Test
    @DisplayName("El unico administrador activo no puede dejar de serlo")
    void ultimoAdministrador() {
        Usuario admin = DatosDto.usuario();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(admin, editor));

        assertThatThrownBy(() -> usuarioService.actualizar(1, EMPRESA_ID, cambios(null, null, null, RolAcceso.EDITOR)))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("administrador");
    }

    @Test
    @DisplayName("Un administrador puede cambiar de rol si queda otro administrador activo")
    void administradorConReemplazo() {
        Usuario admin = DatosDto.usuario();
        Usuario otroAdmin = DatosDto.usuario();
        otroAdmin.setId(3);
        usuarioExistente(admin);
        when(usuarioRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(admin, otroAdmin));

        Usuario editado = usuarioService.actualizar(1, EMPRESA_ID, cambios(null, null, null, RolAcceso.EDITOR));

        assertThat(editado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
    }
}
