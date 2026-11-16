package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.dto.auth.AuthResponse;
import desarrollo.web.Bizagi2.dto.auth.LoginRequest;
import desarrollo.web.Bizagi2.dto.auth.RegistroRequest;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
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

    @Mock
    private RefreshTokenService refreshTokenService;

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
        when(jwtService.generarAccessToken(admin)).thenReturn("token.jwt.valido");
        when(refreshTokenService.iniciarFamilia(admin)).thenReturn("refresh.registro");

        RegistroRequest datos = new RegistroRequest();
        datos.setNombreEmpresa("Tech SA");
        datos.setNit("900111222");
        datos.setEmailContacto("info@tech.com");
        datos.setNombre("Carlos Admin");
        datos.setEmail("admin@tech.com");
        datos.setPassword("Secure12345");

        AuthService.Sesion sesion = authService.registrar(datos);
        AuthResponse respuesta = sesion.cuerpo();

        assertThat(sesion.refreshToken()).isEqualTo("refresh.registro");
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getToken()).isEqualTo("token.jwt.valido");
        assertThat(respuesta.getUsuarioId()).isEqualTo(10);
        assertThat(respuesta.getEmpresaId()).isEqualTo(1);
        assertThat(respuesta.getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
    }

    @Test
    @DisplayName("HU-01 CA5: Registrar con campos faltantes lanza ReglaNegocioException")
    void registrar_camposFaltantes_lanzaReglaNegocio() {
        RegistroRequest datos = new RegistroRequest();
        datos.setNombreEmpresa("Tech SA");
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
        when(jwtService.generarAccessToken(usuario)).thenReturn("token.login.ok");
        when(jwtService.getAccessExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.iniciarFamilia(usuario)).thenReturn("refresh.login.ok");

        AuthService.Sesion sesion = authService.login(new LoginRequest("carlos@tech.com", "Password123"));
        AuthResponse respuesta = sesion.cuerpo();

        assertThat(sesion.refreshToken()).isEqualTo("refresh.login.ok");
        assertThat(respuesta.getTokenType()).isEqualTo("Bearer");
        assertThat(respuesta.getExpiresIn()).isEqualTo(900L);
        assertThat(respuesta.getToken()).isEqualTo("token.login.ok");
        assertThat(respuesta.getEmpresaId()).isEqualTo(1);
        assertThat(respuesta.getEmpresa()).isEqualTo("Tech SA");
        assertThat(respuesta.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
    }

    @Test
    @DisplayName("HU-03 CA5: Logout delega cierre de sesión a UsuarioService")
    void logout_usuario_cierraSesion() {
        Usuario usuario = new Usuario();
        usuario.setId(10);

        authService.logout(usuario, null);

        verify(usuarioService).cerrarSesion(10);
        verify(refreshTokenService).cerrarSesiones(10);
    }

    @Test
    @DisplayName("Refrescar: entrega access nuevo y el refresh rotado, sin poner el refresh en el cuerpo")
    void refrescar_rotaYEntregaTokensNuevos() {
        Empresa empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Tech SA");
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmpresa(empresa);
        usuario.setRolAcceso(RolAcceso.EDITOR);

        when(refreshTokenService.rotar("refresh.viejo"))
                .thenReturn(new RefreshTokenService.Rotacion(usuario, "refresh.nuevo"));
        when(jwtService.generarAccessToken(usuario)).thenReturn("access.nuevo");

        AuthService.Sesion sesion = authService.refrescar("refresh.viejo");

        assertThat(sesion.refreshToken()).isEqualTo("refresh.nuevo");
        assertThat(sesion.cuerpo().getToken()).isEqualTo("access.nuevo");
    }

    @Test
    @DisplayName("Refrescar con un refresh token invalido propaga SesionInvalidaException")
    void refrescar_tokenInvalido_propagaExcepcion() {
        when(refreshTokenService.rotar("malo")).thenThrow(new SesionInvalidaException("Sesion invalida"));

        assertThatThrownBy(() -> authService.refrescar("malo")).isInstanceOf(SesionInvalidaException.class);
    }
}
