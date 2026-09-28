package desarrollo.web.Bizagi2.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final long EXPIRACION_MS = 3600000L; // 1 hora

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_VALIDO, EXPIRACION_MS);
    }

    @Test
    @DisplayName("Constructor lanza excepcion si secret es nulo o menor a 32 caracteres")
    void testConstructorValidacionSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, EXPIRACION_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("corto", EXPIRACION_MS));
    }

    @Test
    @DisplayName("Generar token y leer claims correctamente")
    void testGenerarYLeerToken() {
        Empresa empresa = new Empresa();
        empresa.setId(10);

        Usuario usuario = new Usuario();
        usuario.setId(42);
        usuario.setEmail("admin@empresa.com");
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setEmpresa(empresa);
        usuario.setVersionSesion(1);

        String token = jwtService.generarToken(usuario);
        assertNotNull(token);

        Claims claims = jwtService.leerClaims(token);
        assertEquals("42", claims.getSubject());
        assertEquals("admin@empresa.com", claims.get("email"));
        assertEquals("ADMINISTRADOR", claims.get("rol"));
        assertEquals(10, claims.get("empresaId", Integer.class));
        assertEquals(1, claims.get("v", Integer.class));
    }

    @Test
    @DisplayName("Leer claims falla con token invalido o alterado")
    void testTokenInvalido() {
        assertThrows(JwtException.class, () -> jwtService.leerClaims("token.totalmente.falso"));
    }

    @Test
    @DisplayName("Leer claims falla con token expirado")
    void testTokenExpirado() throws InterruptedException {
        JwtService jwtServiceCorto = new JwtService(SECRET_VALIDO, 1L); // 1 ms

        Empresa empresa = new Empresa();
        empresa.setId(1);
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("test@test.com");
        usuario.setRolAcceso(RolAcceso.LECTOR);
        usuario.setEmpresa(empresa);
        usuario.setVersionSesion(1);

        String token = jwtServiceCorto.generarToken(usuario);
        Thread.sleep(15);

        assertThrows(JwtException.class, () -> jwtServiceCorto.leerClaims(token));
    }
}
