package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RefreshToken;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.repository.RefreshTokenRepository;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.security.JwtService;
import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService - rotacion y deteccion de reutilizacion")
class RefreshTokenServiceTest {

    private static final String SECRET = "clave-secreta-super-larga-y-segura-de-mas-de-32-bytes";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtService jwtService;
    private RefreshTokenService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        service = new RefreshTokenService(refreshTokenRepository, usuarioRepository, jwtService);

        Empresa empresa = new Empresa();
        empresa.setId(1);
        usuario = new Usuario();
        usuario.setId(7);
        usuario.setEmail("ana@acme.com");
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);
        usuario.setActivo(true);
        usuario.setVersionSesion(2);
    }

    private RefreshToken registro(String jti, String familia, boolean revocado) {
        RefreshToken r = new RefreshToken();
        r.setJti(jti);
        r.setFamiliaId(familia);
        r.setUsuario(usuario);
        r.setRevocado(revocado);
        return r;
    }

    @Test
    @DisplayName("iniciarFamilia guarda el jti del token emitido y devuelve un refresh valido")
    void iniciarFamilia_guardaRegistro() {
        String token = service.iniciarFamilia(usuario);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken guardado = captor.getValue();

        Claims claims = jwtService.leerRefreshClaims(token);
        assertThat(guardado.getJti()).isEqualTo(claims.getId());
        assertThat(guardado.getFamiliaId()).isEqualTo(claims.get("fam", String.class));
        assertThat(guardado.getUsuario()).isSameAs(usuario);
        assertThat(guardado.isRevocado()).isFalse();
        assertThat(guardado.getExpiraEn()).isAfter(guardado.getCreadoEn());
    }

    @Test
    @DisplayName("rotar: revoca el token usado y emite otro de la misma familia")
    void rotar_exito() {
        String viejo = jwtService.generarRefreshToken(usuario, "fam-A").token();
        String jtiViejo = jwtService.leerRefreshClaims(viejo).getId();
        when(refreshTokenRepository.findByJti(jtiViejo)).thenReturn(Optional.of(registro(jtiViejo, "fam-A", false)));
        when(refreshTokenRepository.revocarSiVigente(jtiViejo)).thenReturn(1);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        RefreshTokenService.Rotacion rotacion = service.rotar(viejo);

        assertThat(rotacion.usuario()).isSameAs(usuario);
        assertThat(rotacion.refreshToken()).isNotEqualTo(viejo);
        Claims nuevo = jwtService.leerRefreshClaims(rotacion.refreshToken());
        assertThat(nuevo.get("fam", String.class)).isEqualTo("fam-A");
        assertThat(nuevo.getId()).isNotEqualTo(jtiViejo);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).revocarFamilia(anyString());
    }

    @Test
    @DisplayName("rotar: reutilizar un token ya usado revoca toda la familia")
    void rotar_tokenYaUsado_revocaFamilia() {
        String token = jwtService.generarRefreshToken(usuario, "fam-B").token();
        String jti = jwtService.leerRefreshClaims(token).getId();
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(registro(jti, "fam-B", true)));

        assertThatThrownBy(() -> service.rotar(token)).isInstanceOf(SesionInvalidaException.class);

        verify(refreshTokenRepository).revocarFamilia("fam-B");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotar: si dos peticiones usan el mismo token a la vez, la perdedora se trata como reutilizacion")
    void rotar_carreraConcurrente_revocaFamilia() {
        String token = jwtService.generarRefreshToken(usuario, "fam-C").token();
        String jti = jwtService.leerRefreshClaims(token).getId();
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(registro(jti, "fam-C", false)));
        when(refreshTokenRepository.revocarSiVigente(jti)).thenReturn(0);

        assertThatThrownBy(() -> service.rotar(token)).isInstanceOf(SesionInvalidaException.class);

        verify(refreshTokenRepository).revocarFamilia("fam-C");
    }

    @Test
    @DisplayName("rotar: rechaza token ausente, basura, access token o jti desconocido")
    void rotar_tokenInvalido() {
        assertThatThrownBy(() -> service.rotar(null)).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> service.rotar("  ")).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> service.rotar("no.es.jwt")).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> service.rotar(jwtService.generarAccessToken(usuario)))
                .isInstanceOf(SesionInvalidaException.class);

        String desconocido = jwtService.generarRefreshToken(usuario, "fam-D").token();
        when(refreshTokenRepository.findByJti(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotar(desconocido)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("rotar: usuario desactivado o con la sesion cerrada (version distinta) no puede renovar")
    void rotar_usuarioInactivoOVersionCambiada() {
        String token = jwtService.generarRefreshToken(usuario, "fam-E").token();
        String jti = jwtService.leerRefreshClaims(token).getId();
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(registro(jti, "fam-E", false)));
        when(refreshTokenRepository.revocarSiVigente(jti)).thenReturn(1);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        usuario.setVersionSesion(3); // hizo logout despues de emitir el token
        assertThatThrownBy(() -> service.rotar(token)).isInstanceOf(SesionInvalidaException.class);
        verify(refreshTokenRepository).revocarFamilia("fam-E");

        usuario.setVersionSesion(2);
        usuario.setActivo(false);
        assertThatThrownBy(() -> service.rotar(token)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("cerrarSesiones elimina todos los refresh tokens del usuario")
    void cerrarSesiones_eliminaDelUsuario() {
        service.cerrarSesiones(7);
        verify(refreshTokenRepository).eliminarDeUsuario(7);
    }

    @Test
    @DisplayName("limpiarVencidos borra los registros con expiracion pasada")
    void limpiarVencidos_delegaEnRepositorio() {
        when(refreshTokenRepository.eliminarVencidosAntesDe(any())).thenReturn(4);
        assertThat(service.limpiarVencidos()).isEqualTo(4);
    }
}
