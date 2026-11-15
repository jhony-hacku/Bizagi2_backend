package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioCreateRequest;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioResponse;
import desarrollo.web.Bizagi2.dto.usuario.UsuarioUpdateRequest;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - conversion entre DTOs y entidades")
class UsuarioServiceDtoTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpresaService empresaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private UsuarioService servicio;

    private UsuarioService usuarioService;
    private final Usuario usuario = DatosDto.usuario();

    @BeforeEach
    void setUp() {
        usuarioService = spy(servicio);
    }

    @Test
    @DisplayName("aResponse incluye la empresa y no expone la contrasena")
    void aResponse() {
        UsuarioResponse dto = usuarioService.aResponse(usuario);

        assertThat(dto.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(dto.getEmail()).isEqualTo("ana@acme.com");
        assertThat(dto.getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
    }

    @Test
    @DisplayName("listarDto, buscarPorIdDto y activarDto devuelven DTOs")
    void consultas() {
        doReturn(List.of(usuario)).when(usuarioService).listar(EMPRESA_ID);
        doReturn(usuario).when(usuarioService).buscarPorId(1, EMPRESA_ID);
        doReturn(usuario).when(usuarioService).activar(1, EMPRESA_ID);

        assertThat(usuarioService.listarDto(EMPRESA_ID)).extracting(UsuarioResponse::getId).containsExactly(1);
        assertThat(usuarioService.buscarPorIdDto(1, EMPRESA_ID).getNombre()).isEqualTo("Ana Perez");
        assertThat(usuarioService.activarDto(1, EMPRESA_ID).isActivo()).isTrue();
    }

    @Test
    @DisplayName("crear con UsuarioCreateRequest pasa los datos del nuevo usuario")
    void crear() {
        doReturn(usuario).when(usuarioService).crear(eq(EMPRESA_ID), any(Usuario.class));

        usuarioService.crear(EMPRESA_ID, new UsuarioCreateRequest("Luis", "luis@acme.com", "Clave12345", RolAcceso.LECTOR));

        ArgumentCaptor<Usuario> enviado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).crear(eq(EMPRESA_ID), enviado.capture());
        assertThat(enviado.getValue().getEmail()).isEqualTo("luis@acme.com");
        assertThat(enviado.getValue().getRolAcceso()).isEqualTo(RolAcceso.LECTOR);
    }

    @Test
    @DisplayName("actualizar con UsuarioUpdateRequest devuelve el usuario modificado")
    void actualizar() {
        doReturn(usuario).when(usuarioService).actualizar(eq(1), eq(EMPRESA_ID), any(Usuario.class));

        UsuarioResponse dto = usuarioService.actualizar(1, EMPRESA_ID, new UsuarioUpdateRequest("Ana", null, null, null));

        assertThat(dto.getId()).isEqualTo(1);
    }
}
