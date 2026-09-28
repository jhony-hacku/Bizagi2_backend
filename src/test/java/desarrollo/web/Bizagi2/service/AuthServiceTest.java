package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.security.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceTest - Pruebas basadas en HU-01 y HU-03")
class AuthServiceTest {

    @Mock
    private EmpresaService empresaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("HU-01 CA3 & CA4: Registrar empresa y administrador inicial devuelve token y metadatos")
    void registrar_datosCompletos_exito() {
        Empresa empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Tech SA");

        Usuario admin = new Usuario();
        admin.setId(10);
        admin.setNombre("Carlos Admin");
        admin.setEmail("admin@tech.com");
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        admin.setEmpresa(empresa);

        when(empresaService.crear("Tech SA", "900111222", "info@tech.com")).thenReturn(empresa);
        when(usuarioService.crear(eq(1), any(Usuario.class))).thenReturn(admin);
        when(jwtService.generarToken(admin)).thenReturn("token.jwt.valido");

        Map<String, String> datos = new HashMap<>();
        datos.put("nombreEmpresa", "Tech SA");
        datos.put("nit", "900111222");
        datos.put("emailContacto", "info@tech.com");
        datos.put("nombre", "Carlos Admin");
        datos.put("email", "admin@tech.com");
        datos.put("password", "Secure12345");

        Map<String, Object> respuesta = authService.registrar(datos);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.get("token")).isEqualTo("token.jwt.valido");
        assertThat(respuesta.get("usuarioId")).isEqualTo(10);
        assertThat(respuesta.get("empresaId")).isEqualTo(1);
        assertThat(respuesta.get("rolAcceso")).isEqualTo(RolAcceso.ADMINISTRADOR);
    }

    @Test
    @DisplayName("HU-01 CA5: Registrar con campos faltantes lanza ReglaNegocioException")
    void registrar_camposFaltantes_lanzaReglaNegocio() {
        Map<String, String> datos = new HashMap<>();
        datos.put("nombreEmpresa", "Tech SA");
        // nit falta

        assertThatThrownBy(() -> authService.registrar(datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nit");
    }

    @Test
    @DisplayName("HU-03 CA3: Login exitoso devuelve token y datos de empresa asociada")
    void login_credencialesValidas_retornaTokenYDatos() {
        Empresa empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Tech SA");

        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setNombre("Carlos");
        usuario.setEmail("carlos@tech.com");
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);

        when(usuarioService.autenticar("carlos@tech.com", "Password123")).thenReturn(usuario);
        when(jwtService.generarToken(usuario)).thenReturn("token.login.ok");

        Map<String, Object> respuesta = authService.login("carlos@tech.com", "Password123");

        assertThat(respuesta.get("token")).isEqualTo("token.login.ok");
        assertThat(respuesta.get("empresaId")).isEqualTo(1);
        assertThat(respuesta.get("empresa")).isEqualTo("Tech SA");
        assertThat(respuesta.get("rolAcceso")).isEqualTo(RolAcceso.EDITOR);
    }

    @Test
    @DisplayName("HU-03 CA5: Logout delega cierre de sesión a UsuarioService")
    void logout_usuario_cierraSesion() {
        Usuario usuario = new Usuario();
        usuario.setId(10);

        authService.logout(usuario);

        verify(usuarioService).cerrarSesion(10);
    }
}
