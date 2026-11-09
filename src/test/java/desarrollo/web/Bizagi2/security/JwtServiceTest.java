package desarrollo.web.Bizagi2.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET_VALIDO = "clave-secreta-super-larga-y-segura-de-mas-de-32-bytes";
    private static final long ACCESS_MS = 900_000L; // 15 minutos
    private static final long REFRESH_MS = 604_800_000L; // 7 dias

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_VALIDO, ACCESS_MS, REFRESH_MS);
    }

    private Usuario usuario(int id, RolAcceso rol, int version) {
        Empresa empresa = new Empresa();
        empresa.setId(10);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("admin@empresa.com");
        usuario.setRolAcceso(rol);
        usuario.setEmpresa(empresa);
        usuario.setVersionSesion(version);
        return usuario;
    }

    @Test
    @DisplayName("Constructor lanza excepcion si secret es nulo o menor a 32 caracteres")
    void testConstructorValidacionSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, ACCESS_MS, REFRESH_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("corto", ACCESS_MS, REFRESH_MS));
    }

    @Test
    @DisplayName("Constructor exige que el refresh dure mas que el access")
    void testConstructorValidacionDuraciones() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET_VALIDO, ACCESS_MS, ACCESS_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET_VALIDO, 0, REFRESH_MS));
    }

    @Test
    @DisplayName("Access token: generar y leer claims correctamente")
    void testGenerarYLeerAccessToken() {
        String token = jwtService.generarAccessToken(usuario(42, RolAcceso.ADMINISTRADOR, 1));
        assertNotNull(token);

        Claims claims = jwtService.leerAccessClaims(token);
        assertEquals("42", claims.getSubject());
        assertEquals("access", claims.get("tipo"));
        assertEquals("admin@empresa.com", claims.get("email"));
        assertEquals("ADMINISTRADOR", claims.get("rol"));
        assertEquals(10, claims.get("empresaId", Integer.class));
        assertEquals(1, claims.get("v", Integer.class));
        assertEquals(ACCESS_MS, claims.getExpiration().getTime() - claims.getIssuedAt().getTime(), 1000);
    }

    @Test
    @DisplayName("Refresh token: lleva jti, familia y version, y dura mucho mas que el access")
    void testGenerarYLeerRefreshToken() {
        JwtService.RefreshEmitido emitido = jwtService.generarRefreshToken(usuario(42, RolAcceso.EDITOR, 3), "fam-1");

        Claims claims = jwtService.leerRefreshClaims(emitido.token());
        assertEquals("42", claims.getSubject());
        assertEquals("refresh", claims.get("tipo"));
        assertEquals(emitido.jti(), claims.getId());
        assertEquals("fam-1", claims.get("fam", String.class));
        assertEquals(3, claims.get("v", Integer.class));
        // El refresh no lleva datos de perfil: solo lo necesario para renovar
        assertNull(claims.get("email"));
        assertNull(claims.get("rol"));
        assertTrue(claims.getExpiration().getTime() - claims.getIssuedAt().getTime() > ACCESS_MS * 10);
    }

    @Test
    @DisplayName("Cada refresh token tiene un jti distinto")
    void testJtiUnico() {
        Usuario usuario = usuario(1, RolAcceso.LECTOR, 0);
        assertNotEquals(jwtService.generarRefreshToken(usuario, "f").jti(),
                jwtService.generarRefreshToken(usuario, "f").jti());
    }

    @Test
    @DisplayName("Un refresh token no sirve como access token")
    void testRefreshNoSirveComoAccess() {
        String refresh = jwtService.generarRefreshToken(usuario(1, RolAcceso.LECTOR, 0), "f").token();
        assertThrows(JwtException.class, () -> jwtService.leerAccessClaims(refresh));
    }

    @Test
    @DisplayName("Un access token no sirve como refresh token")
    void testAccessNoSirveComoRefresh() {
        String access = jwtService.generarAccessToken(usuario(1, RolAcceso.LECTOR, 0));
        assertThrows(JwtException.class, () -> jwtService.leerRefreshClaims(access));
    }

    @Test
    @DisplayName("Leer claims falla con token invalido, alterado o firmado con otra clave")
    void testTokenInvalido() {
        assertThrows(JwtException.class, () -> jwtService.leerAccessClaims("token.totalmente.falso"));

        JwtService otro = new JwtService("otra-clave-secreta-distinta-de-mas-de-32-bytes!", ACCESS_MS, REFRESH_MS);
        String ajeno = otro.generarAccessToken(usuario(1, RolAcceso.LECTOR, 0));
        assertThrows(JwtException.class, () -> jwtService.leerAccessClaims(ajeno));
    }

    @Test
    @DisplayName("Leer claims falla con token expirado")
    void testTokenExpirado() throws InterruptedException {
        JwtService corto = new JwtService(SECRET_VALIDO, 1L, 2L); // 1 ms y 2 ms

        String access = corto.generarAccessToken(usuario(1, RolAcceso.LECTOR, 1));
        String refresh = corto.generarRefreshToken(usuario(1, RolAcceso.LECTOR, 1), "f").token();
        Thread.sleep(30);

        assertThrows(JwtException.class, () -> corto.leerAccessClaims(access));
        assertThrows(JwtException.class, () -> corto.leerRefreshClaims(refresh));
    }
}
