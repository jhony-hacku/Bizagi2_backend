package desarrollo.web.Bizagi2.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.RefreshToken;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.SesionInvalidaException;
import desarrollo.web.Bizagi2.repository.RefreshTokenRepository;
import desarrollo.web.Bizagi2.repository.UsuarioRepository;
import desarrollo.web.Bizagi2.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

// Ciclo de vida del refresh token: emitir al hacer login, rotar al renovar y revocar al cerrar sesion.
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public record Rotacion(Usuario usuario, String refreshToken) {
    }

    // Login / registro: abre una familia nueva de tokens
    @Transactional
    public String iniciarFamilia(Usuario usuario) {
        // Login mantiene este bloqueo desde la comprobacion de la contrasena.
        usuarioRepository.buscarParaActualizar(usuario.getId())
                .orElseThrow(() -> new SesionInvalidaException("Usuario no encontrado"));
        return guardar(usuario, UUID.randomUUID().toString(), null);
    }

    // Rotacion: cada refresh token se usa UNA sola vez. Al usarlo se revoca y se entrega otro de la misma familia.
    // Si llega uno ya usado, alguien lo copio (o el cliente fallo): se cierran todas las sesiones del usuario y hay que volver a loguearse.
    // noRollbackFor: la revocacion debe quedar guardada aunque se lance la excepcion.
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Rotacion rotar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SesionInvalidaException("Sesion invalida: falta el refresh token");
        }

        Claims claims;
        try {
            claims = jwtService.leerRefreshClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new SesionInvalidaException("Sesion invalida o vencida: inicia sesion de nuevo");
        }

        String jti = claims.getId();
        String familiaId = claims.get("fam", String.class);
        Usuario usuario = usuarioRepository.buscarParaActualizar(Integer.valueOf(claims.getSubject()))
                .orElseThrow(() -> new SesionInvalidaException("Sesion invalida o vencida"));
        RefreshToken guardado = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new SesionInvalidaException("Sesion invalida o vencida: inicia sesion de nuevo"));

        if (!corresponde(guardado, claims) || !usuario.isActivo()
                || claims.get("v", Integer.class) != usuario.getVersionSesion()) {
            throw new SesionInvalidaException("Sesion invalida o vencida: inicia sesion de nuevo");
        }

        // Ya usado o revocado => reutilizacion
        if (guardado.isRevocado() || refreshTokenRepository.revocarSiVigente(jti) == 0) {
            // Ante una posible copia se cierran tambien los access ya emitidos.
            usuario.setVersionSesion(usuario.getVersionSesion() + 1);
            usuarioRepository.save(usuario);
            refreshTokenRepository.eliminarDeUsuario(usuario.getId());
            throw new SesionInvalidaException("Sesion invalida: el token ya fue usado. Inicia sesion de nuevo");
        }

        return new Rotacion(usuario, guardar(usuario, familiaId, claims.getExpiration().toInstant()));
    }

    // Logout: se borran todos los refresh tokens del usuario (todas sus sesiones)
    @Transactional
    public void cerrarSesiones(Integer usuarioId) {
        usuarioRepository.buscarParaActualizar(usuarioId);
        refreshTokenRepository.eliminarDeUsuario(usuarioId);
    }

    // Sin un access vigente, un refresh registrado permite cerrar todas las sesiones.
    // Un token ausente, vencido o ya revocado hace que logout sea un no-op idempotente.
    @Transactional
    public void cerrarPorRefresh(String token) {
        Claims claims;
        try {
            claims = jwtService.leerRefreshClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }
        Usuario usuario = usuarioRepository.buscarParaActualizar(Integer.valueOf(claims.getSubject())).orElse(null);
        RefreshToken registro = refreshTokenRepository.findByJti(claims.getId()).orElse(null);
        if (usuario != null && usuario.isActivo() && registro != null && !registro.isRevocado()
                && corresponde(registro, claims) && claims.get("v", Integer.class) == usuario.getVersionSesion()) {
            usuario.setVersionSesion(usuario.getVersionSesion() + 1);
            usuarioRepository.save(usuario);
            refreshTokenRepository.eliminarDeUsuario(usuario.getId());
        }
    }

    private boolean corresponde(RefreshToken registro, Claims claims) {
        return registro.getUsuario().getId().toString().equals(claims.getSubject())
                && registro.getFamiliaId().equals(claims.get("fam", String.class))
                && registro.getExpiraEn().isAfter(Instant.now())
                && registro.getExpiraEn().getEpochSecond() == claims.getExpiration().toInstant().getEpochSecond();
    }

    // Limpieza periodica: los vencidos ya no sirven ni para detectar reutilizacion
    @Transactional
    public int limpiarVencidos() {
        return refreshTokenRepository.eliminarVencidosAntesDe(Instant.now());
    }

    private String guardar(Usuario usuario, String familiaId, Instant expiraEn) {
        JwtService.RefreshEmitido emitido = expiraEn == null
                ? jwtService.generarRefreshToken(usuario, familiaId)
                : jwtService.generarRefreshToken(usuario, familiaId, expiraEn);
        RefreshToken registro = new RefreshToken();
        registro.setJti(emitido.jti());
        registro.setFamiliaId(familiaId);
        registro.setUsuario(usuario);
        registro.setCreadoEn(emitido.emitidoEn());
        registro.setExpiraEn(emitido.expiraEn());
        registro.setRevocado(false);
        refreshTokenRepository.save(registro);
        return emitido.token();
    }
}
