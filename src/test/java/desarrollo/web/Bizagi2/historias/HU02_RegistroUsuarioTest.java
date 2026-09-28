package desarrollo.web.Bizagi2.historias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.service.EmpresaService;
import desarrollo.web.Bizagi2.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-02: Registro de usuario en empresa")
class HU02_RegistroUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("CA1 & CA2 & CA3: Crear usuario asignado a una sola empresa con rol de acceso y contraseña cifrada")
    void ca1_ca2_ca3_crearUsuario_exito() {
        Empresa empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa Uno");

        Usuario datos = new Usuario();
        datos.setNombre("Carlos Gomez");
        datos.setEmail("carlos@empresa.com");
        datos.setPassword("Password123");
        datos.setRolAcceso(RolAcceso.EDITOR);

        when(usuarioRepository.existsByEmail("carlos@empresa.com")).thenReturn(false);
        when(empresaService.buscarPorId(1)).thenReturn(empresa);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(20);
            return u;
        });

        Usuario resultado = usuarioService.crear(1, datos);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(20);
        assertThat(resultado.getEmpresa().getId()).isEqualTo(1);
        assertThat(resultado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(resultado.getPassword()).isEqualTo("encoded_pass");
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("CA1: Email duplicado en el sistema lanza ConflictoDominioException")
    void ca1_crearUsuario_emailDuplicado_lanzaConflicto() {
        Usuario datos = new Usuario();
        datos.setNombre("Carlos");
        datos.setEmail("carlos@empresa.com");
        datos.setPassword("Password123");
        datos.setRolAcceso(RolAcceso.LECTOR);

        when(usuarioRepository.existsByEmail("carlos@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(1, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El email ya esta registrado");
    }

    @Test
    @DisplayName("CA2: Validación de contraseña corta (< 8 caracteres)")
    void ca2_crearUsuario_passwordCorta_lanzaReglaNegocio() {
        Usuario datos = new Usuario();
        datos.setNombre("Carlos");
        datos.setEmail("carlos@empresa.com");
        datos.setPassword("12345");
        datos.setRolAcceso(RolAcceso.LECTOR);

        assertThatThrownBy(() -> usuarioService.crear(1, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 8 caracteres");
    }

    @Test
    @DisplayName("CA4: Se puede cambiar el rol de un usuario dentro de la empresa")
    void ca4_actualizarRolUsuario_exito() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(20);
        usuarioExistente.setNombre("Carlos");
        usuarioExistente.setEmail("carlos@empresa.com");
        usuarioExistente.setRolAcceso(RolAcceso.LECTOR);
        usuarioExistente.setEmpresa(empresa);

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario cambios = new Usuario();
        cambios.setRolAcceso(RolAcceso.EDITOR);

        Usuario resultado = usuarioService.actualizar(20, 1, cambios);

        assertThat(resultado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        verify(usuarioRepository).save(usuarioExistente);
    }

    @Test
    @DisplayName("CA4 & CA5: Desactivación lógica de usuario (activo=false, versionSesion incrementada)")
    void ca4_ca5_desactivarUsuario_noBorraFisicamente() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario editor = new Usuario();
        editor.setId(20);
        editor.setActivo(true);
        editor.setVersionSesion(0);
        editor.setRolAcceso(RolAcceso.EDITOR);
        editor.setEmpresa(empresa);

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(editor));

        usuarioService.desactivar(20, 1);

        assertThat(editor.isActivo()).isFalse();
        assertThat(editor.getVersionSesion()).isEqualTo(1);
        verify(usuarioRepository).save(editor);
    }

    @Test
    @DisplayName("CA5: No se puede desactivar al único administrador activo de la empresa")
    void ca5_desactivarUnicoAdministrador_lanzaConflicto() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario admin = new Usuario();
        admin.setId(10);
        admin.setActivo(true);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresa);

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmpresaId(1)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> usuarioService.desactivar(10, 1))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("La empresa debe conservar al menos un administrador activo");
    }

    @Test
    @DisplayName("CA3: Buscar usuario de otra empresa lanza RecursoNoEncontradoException (aislamiento)")
    void ca3_buscarPorId_otraEmpresa_lanzaNoEncontrado() {
        Empresa empresa1 = new Empresa();
        empresa1.setId(1);

        Usuario usuario = new Usuario();
        usuario.setId(30);
        usuario.setEmpresa(empresa1);

        when(usuarioRepository.findById(30)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.buscarPorId(30, 999))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
