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
        return emitir(usuario, UUID.randomUUID().toString());
    }

    // Rotacion: cada refresh token se usa UNA sola vez. Al usarlo se revoca y se entrega otro de la misma familia.
    // Si llega uno ya usado, alguien lo copio (o el cliente fallo): se revoca toda la familia y hay que volver a loguearse.
    // noRollbackFor: la revocacion de la familia debe quedar guardada aunque se lance la excepcion.
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
        RefreshToken guardado = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new SesionInvalidaException("Sesion invalida o vencida: inicia sesion de nuevo"));

        // Ya usado o revocado => reutilizacion
        if (guardado.isRevocado() || refreshTokenRepository.revocarSiVigente(jti) == 0) {
            refreshTokenRepository.revocarFamilia(familiaId);
            throw new SesionInvalidaException("Sesion invalida: el token ya fue usado. Inicia sesion de nuevo");
        }

        Usuario usuario = usuarioRepository.findById(Integer.valueOf(claims.getSubject())).orElse(null);
        Integer version = claims.get("v", Integer.class);
        if (usuario == null || !usuario.isActivo() || version == null || version != usuario.getVersionSesion()) {
            refreshTokenRepository.revocarFamilia(familiaId);
            throw new SesionInvalidaException("Sesion invalida o vencida: inicia sesion de nuevo");
        }

        return new Rotacion(usuario, emitir(usuario, familiaId));
    }

    // Logout: se borran todos los refresh tokens del usuario (todas sus sesiones)
    @Transactional
    public void cerrarSesiones(Integer usuarioId) {
        refreshTokenRepository.eliminarDeUsuario(usuarioId);
    }

    // Limpieza periodica: los vencidos ya no sirven ni para detectar reutilizacion
    @Transactional
    public int limpiarVencidos() {
        return refreshTokenRepository.eliminarVencidosAntesDe(Instant.now());
    }

    private String emitir(Usuario usuario, String familiaId) {
        JwtService.RefreshEmitido emitido = jwtService.generarRefreshToken(usuario, familiaId);
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
