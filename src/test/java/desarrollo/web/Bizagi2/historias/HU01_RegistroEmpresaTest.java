package desarrollo.web.Bizagi2.historias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;
import desarrollo.web.Bizagi2.security.JwtService;
import desarrollo.web.Bizagi2.service.AuthService;
import desarrollo.web.Bizagi2.service.EmpresaService;
import desarrollo.web.Bizagi2.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-01: Registro de empresa")
class HU01_RegistroEmpresaTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(empresaService, usuarioService, jwtService);
    }

    @Test
    @DisplayName("CA1 & CA4: Se solicita nombre de empresa, NIT y correo de contacto, y se crea la entidad independiente")
    void ca1_ca4_crearEmpresa_conDatosValidos_exito() {
        when(empresaRepository.existsByNit("900123456-1")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa e = invocation.getArgument(0);
            e.setId(1);
            return e;
        });

        Empresa resultado = empresaService.crear("Mi Empresa S.A.S.", "900123456-1", "contacto@miempresa.com");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Mi Empresa S.A.S.");
        assertThat(resultado.getNit()).isEqualTo("900123456-1");
        assertThat(resultado.getEmailContacto()).isEqualTo("contacto@miempresa.com");

        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("CA2: El NIT es único en el sistema - Si ya existe, lanza ConflictoDominioException")
    void ca2_crearEmpresa_nitDuplicado_lanzaConflicto() {
        when(empresaRepository.existsByNit("900123456-1")).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear("Otra Empresa", "900123456-1", "otra@empresa.com"))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe una empresa registrada con ese NIT");
    }

    @Test
    @DisplayName("CA3: En el registro empresarial se genera un usuario administrador inicial")
    void ca3_registroCompleto_creaUsuarioAdministradorInicial() {
        when(empresaRepository.existsByNit("900999888-2")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa e = invocation.getArgument(0);
            e.setId(10);
            return e;
        });

        Empresa empresaCreada = new Empresa();
        empresaCreada.setId(10);
        empresaCreada.setNombre("Tech Corp");

        Usuario adminGuardado = new Usuario();
        adminGuardado.setId(100);
        adminGuardado.setNombre("Juan Perez");
        adminGuardado.setEmail("admin@techcorp.com");
        adminGuardado.setRolAcceso(RolAcceso.ADMINISTRADOR);
        adminGuardado.setEmpresa(empresaCreada);

        when(usuarioService.crear(eq(10), any(Usuario.class))).thenReturn(adminGuardado);
        when(jwtService.generarToken(adminGuardado)).thenReturn("mocked-jwt-token");

        Map<String, String> datos = new HashMap<>();
        datos.put("nombreEmpresa", "Tech Corp");
        datos.put("nit", "900999888-2");
        datos.put("emailContacto", "info@techcorp.com");
        datos.put("nombre", "Juan Perez");
        datos.put("email", "admin@techcorp.com");
        datos.put("password", "SecurePass123");

        Map<String, Object> respuesta = authService.registrar(datos);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.get("token")).isEqualTo("mocked-jwt-token");
        assertThat(respuesta.get("usuarioId")).isEqualTo(100);
        assertThat(respuesta.get("rolAcceso")).isEqualTo(RolAcceso.ADMINISTRADOR);
        assertThat(respuesta.get("empresaId")).isEqualTo(10);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).crear(eq(10), captor.capture());
        assertThat(captor.getValue().getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@techcorp.com");
    }

    @Test
    @DisplayName("CA5: Validación de campos obligatorios vacíos o nulos")
    void ca5_crearEmpresa_camposObligatoriosFaltantes_lanzaExcepcion() {
        assertThatThrownBy(() -> empresaService.crear(null, "900123456-1", "contacto@empresa.com"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombreEmpresa");

        assertThatThrownBy(() -> empresaService.crear("Empresa", "", "contacto@empresa.com"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nit");

        assertThatThrownBy(() -> empresaService.crear("Empresa", "900123456-1", "   "))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("emailContacto");
    }

    @Test
    @DisplayName("CA5: Validación de formato de email de contacto inválido")
    void ca5_crearEmpresa_emailInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> empresaService.crear("Empresa", "900123456-1", "correo-invalido"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El formato del email no es valido");
    }
}
