package desarrollo.web.Bizagi2.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.repository.RefreshTokenRepository;
import desarrollo.web.Bizagi2.service.AuthService;
import desarrollo.web.Bizagi2.service.UsuarioService;

/** Transacciones y repositorios reales, sin mocks. Puede ejecutarse tambien sobre PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
class SesionPersistenciaTest {
    @Autowired AuthService auth;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RefreshTokenRepository refreshRepository;
    @Autowired JwtService jwt;

    @Test
    void loginBloqueaUsuarioYEmiteTokensConLaVersionActual() {
        var sesion = registrar();
        var nueva = auth.login((String) sesion.cuerpo().get("email"), "Password123!");
        assertThat(nueva.cuerpo().get("usuarioId")).isEqualTo(sesion.cuerpo().get("usuarioId"));
        assertThat(jwt.leerRefreshClaims(nueva.refreshToken()).get("v", Integer.class)).isZero();
    }

    @Test
    void cambiarPasswordRevocaAccessYRefresh() {
        var sesion = registrar();
        Usuario usuario = usuario(sesion);
        int version = usuario.getVersionSesion();
        Usuario cambios = new Usuario();
        cambios.setPassword("OtraPassword123!");
        usuarios.actualizar(usuario.getId(), usuario.getEmpresa().getId(), cambios);
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(version + 1);
        assertThat(jwt.leerAccessClaims((String) sesion.cuerpo().get("token")).get("v", Integer.class))
                .isNotEqualTo(usuario(sesion).getVersionSesion());
        assertInvalido(sesion.refreshToken());
        assertThat(refreshRepository.findByJti(jwt.leerRefreshClaims(sesion.refreshToken()).getId())).isEmpty();
    }

    @Test
    void reutilizacionConfirmaRevocacionAunqueRespondaError() {
        var sesion = registrar();
        var renovada = auth.refrescar(sesion.refreshToken());
        assertInvalido(sesion.refreshToken());
        assertInvalido(renovada.refreshToken());
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
    }

    @Test
    void rotacionConservaVencimientoAbsoluto() {
        var sesion = registrar();
        var renovada = auth.refrescar(sesion.refreshToken());
        assertThat(jwt.leerRefreshClaims(renovada.refreshToken()).getExpiration())
                .isEqualTo(jwt.leerRefreshClaims(sesion.refreshToken()).getExpiration());
        assertThat(jwt.leerRefreshClaims(renovada.refreshToken()).getId())
                .isNotEqualTo(jwt.leerRefreshClaims(sesion.refreshToken()).getId());
    }

    @Test
    void logoutConRefreshEsIdempotente() {
        var sesion = registrar();
        auth.logout(null, sesion.refreshToken());
        auth.logout(null, sesion.refreshToken());
        auth.logout(null, "basura");
        auth.logout(null, null);
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
        assertInvalido(sesion.refreshToken());
    }

    @Test
    void dosRotacionesDelMismoTokenNoDejanDescendientesVigentes() throws Exception {
        var sesion = registrar();
        var resultados = paralelo(() -> auth.refrescar(sesion.refreshToken()),
                () -> auth.refrescar(sesion.refreshToken()));
        assertThat(resultados.stream().filter(AuthService.Sesion.class::isInstance)).hasSize(1);
        assertThat(resultados.stream().filter(SesionInvalidaException.class::isInstance)).hasSize(1);
        for (Object resultado : resultados) {
            if (resultado instanceof AuthService.Sesion nueva) assertInvalido(nueva.refreshToken());
        }
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
    }

    @Test
    void replayDeAntecesorCompitiendoConRotacionNoDejaSesionVigente() throws Exception {
        for (int intento = 0; intento < 5; intento++) {
            var sesion = registrar();
            var renovada = auth.refrescar(sesion.refreshToken());
            var resultados = paralelo(() -> auth.refrescar(sesion.refreshToken()),
                    () -> auth.refrescar(renovada.refreshToken()));
            for (Object resultado : resultados) {
                if (resultado instanceof AuthService.Sesion nueva) assertInvalido(nueva.refreshToken());
            }
            assertInvalido(renovada.refreshToken());
            assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
        }
    }

    @Test
    void logoutYRefreshConcurrentesTerminanSinSesion() throws Exception {
        var sesion = registrar();
        Usuario principal = usuario(sesion);
        var resultados = paralelo(() -> auth.refrescar(sesion.refreshToken()), () -> {
            auth.logout(principal, null);
            return "logout";
        });
        for (Object resultado : resultados) {
            if (resultado instanceof AuthService.Sesion nueva) assertInvalido(nueva.refreshToken());
        }
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
        assertInvalido(sesion.refreshToken());
    }

    @Test
    void cierresConcurrentesNoPierdenIncrementos() throws Exception {
        var sesion = registrar();
        Usuario principal = usuario(sesion);
        paralelo(() -> { auth.logout(principal, null); return "a"; },
                () -> { auth.logout(principal, null); return "b"; });
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(2);
    }

    @Test
    void editarUsuarioNoRestauraVersionAnteriorAlLogout() throws Exception {
        var sesion = registrar();
        Usuario principal = usuario(sesion);
        Usuario cambios = new Usuario();
        cambios.setNombre("Nombre editado");
        paralelo(() -> usuarios.actualizar(principal.getId(), principal.getEmpresa().getId(), cambios),
                () -> { auth.logout(principal, null); return "logout"; });
        assertThat(usuario(sesion).getVersionSesion()).isEqualTo(1);
        assertThat(usuario(sesion).getNombre()).isEqualTo("Nombre editado");
    }

    private AuthService.Sesion registrar() {
        String id = UUID.randomUUID().toString();
        return auth.registrar(Map.of("nombreEmpresa", "JWT " + id, "nit", id,
                "emailContacto", "contacto@example.com", "nombre", "Prueba JWT",
                "email", id + "@example.com", "password", "Password123!"));
    }

    private Usuario usuario(AuthService.Sesion sesion) {
        return usuarioRepository.findById((Integer) sesion.cuerpo().get("usuarioId")).orElseThrow();
    }

    private void assertInvalido(String token) {
        assertThatThrownBy(() -> auth.refrescar(token)).isInstanceOf(SesionInvalidaException.class);
    }

    private List<Object> paralelo(Callable<?> primera, Callable<?> segunda) throws Exception {
        var barrera = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> ejecutar(barrera, primera));
            var b = pool.submit(() -> ejecutar(barrera, segunda));
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private Object ejecutar(CyclicBarrier barrera, Callable<?> operacion) throws Exception {
        barrera.await(10, TimeUnit.SECONDS);
        try {
            return operacion.call();
        } catch (SesionInvalidaException ex) {
            return ex;
        }
    }
}
