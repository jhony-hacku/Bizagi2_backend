package desarrollo.web.Bizagi2.service;

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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceTest - Pruebas basadas en HU-02 y HU-03")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("HU-02 CA1, CA2 & CA3: Crear usuario en la empresa con rol de acceso y contraseña cifrada")
    void crear_usuarioValido_exito() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario datos = new Usuario();
        datos.setNombre("Ana Lopez");
        datos.setEmail("ana@empresa.com");
        datos.setPassword("Password123");
        datos.setRolAcceso(RolAcceso.EDITOR);

        when(usuarioRepository.existsByEmail("ana@empresa.com")).thenReturn(false);
        when(empresaService.buscarPorId(1)).thenReturn(empresa);
        when(passwordEncoder.encode("Password123")).thenReturn("hash123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(10);
            return u;
        });

        Usuario resultado = usuarioService.crear(1, datos);

        assertThat(resultado.getId()).isEqualTo(10);
        assertThat(resultado.getEmpresa().getId()).isEqualTo(1);
        assertThat(resultado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(resultado.getPassword()).isEqualTo("hash123");
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("HU-02 CA1: Email duplicado lanza ConflictoDominioException")
    void crear_emailDuplicado_lanzaConflicto() {
        Usuario datos = new Usuario();
        datos.setNombre("Ana");
        datos.setEmail("ana@empresa.com");
        datos.setPassword("Password123");
        datos.setRolAcceso(RolAcceso.LECTOR);

        when(usuarioRepository.existsByEmail("ana@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(1, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El email ya esta registrado");
    }

    @Test
    @DisplayName("HU-02 CA2: Contraseña menor a 8 caracteres lanza ReglaNegocioException")
    void crear_passwordCorta_lanzaReglaNegocio() {
        Usuario datos = new Usuario();
        datos.setNombre("Ana");
        datos.setEmail("ana@empresa.com");
        datos.setPassword("12345");
        datos.setRolAcceso(RolAcceso.LECTOR);

        assertThatThrownBy(() -> usuarioService.crear(1, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 8 caracteres");
    }

    @Test
    @DisplayName("HU-02 CA4: Actualizar nombre, email y rol del usuario")
    void actualizar_datosValidos_actualizaCorrectamente() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario usuario = new Usuario();
        usuario.setId(5);
        usuario.setNombre("Viejo");
        usuario.setEmail("viejo@empresa.com");
        usuario.setRolAcceso(RolAcceso.LECTOR);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findById(5)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario cambios = new Usuario();
        cambios.setNombre("Nuevo Nombre");
        cambios.setRolAcceso(RolAcceso.EDITOR);

        Usuario resultado = usuarioService.actualizar(5, 1, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Nuevo Nombre");
        assertThat(resultado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
    }

    @Test
    @DisplayName("HU-02 CA4 & CA5: Desactivación lógica de usuario preserva su registro e incrementa versionSesion")
    void desactivar_usuarioNormal_desactivaLogicamente() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario usuario = new Usuario();
        usuario.setId(5);
        usuario.setActivo(true);
        usuario.setVersionSesion(0);
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findById(5)).thenReturn(Optional.of(usuario));

        usuarioService.desactivar(5, 1);

        assertThat(usuario.isActivo()).isFalse();
        assertThat(usuario.getVersionSesion()).isEqualTo(1);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("HU-02 CA5: Bloqueo de desactivación si es el único administrador activo de la empresa")
    void desactivar_ultimoAdministrador_lanzaConflicto() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setActivo(true);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresa);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmpresaId(1)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> usuarioService.desactivar(1, 1))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("La empresa debe conservar al menos un administrador activo");
    }

    @Test
    @DisplayName("Reactivar usuario previamente desactivado")
    void activar_usuarioInactivo_activaCorrectamente() {
        Empresa empresa = new Empresa();
        empresa.setId(1);

        Usuario usuario = new Usuario();
        usuario.setId(8);
        usuario.setActivo(false);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findById(8)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario activado = usuarioService.activar(8, 1);

        assertThat(activado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("HU-03 CA1 & CA2: Autenticación exitosa verificando contraseña cifrada")
    void autenticar_credencialesValidas_retornaUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@empresa.com");
        usuario.setPassword("encoded_hash");
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail("user@empresa.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "encoded_hash")).thenReturn(true);

        Usuario resultado = usuarioService.autenticar("user@empresa.com", "Password123");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("user@empresa.com");
    }

    @Test
    @DisplayName("HU-03 CA4: Credenciales inválidas arroja mensaje genérico sin revelar datos")
    void autenticar_passwordInvalida_lanzaBadCredentials() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@empresa.com");
        usuario.setPassword("encoded_hash");
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail("user@empresa.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPass", "encoded_hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.autenticar("user@empresa.com", "WrongPass"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("HU-03 CA5: Cerrar sesión incrementa la versión de sesión")
    void cerrarSesion_incrementaVersionSesion() {
        Usuario usuario = new Usuario();
        usuario.setId(3);
        usuario.setVersionSesion(5);

        when(usuarioRepository.findById(3)).thenReturn(Optional.of(usuario));

        usuarioService.cerrarSesion(3);

        assertThat(usuario.getVersionSesion()).isEqualTo(6);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Listar usuarios de una empresa")
    void listar_retornaUsuariosDeEmpresa() {
        when(usuarioRepository.findByEmpresaId(1)).thenReturn(List.of(new Usuario(), new Usuario()));

        List<Usuario> lista = usuarioService.listar(1);

        assertThat(lista).hasSize(2);
    }
}
