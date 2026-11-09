package desarrollo.web.Bizagi2.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import desarrollo.web.Bizagi2.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Emite y valida los dos tokens de la sesion:
//  - ACCESS  (corto, minutos): va en "Authorization: Bearer" y autoriza cada peticion.
//  - REFRESH (largo, dias):    solo sirve para pedir un access nuevo en POST /api/auth/refresh.
// Los dos llevan el claim "tipo" para que uno nunca se pueda usar en lugar del otro.
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private static final String ISSUER = "bizagi2";
    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_FAMILIA = "fam";
    private static final String CLAIM_VERSION = "v";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final JwtParser parser;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        if (accessExpirationMs <= 0 || refreshExpirationMs <= accessExpirationMs) {
            throw new IllegalArgumentException(
                    "La duracion del refresh token debe ser mayor que la del access token");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.parser = Jwts.parser().verifyWith(secretKey).requireIssuer(ISSUER).build();
    }

    // Token corto: lleva los datos que el frontend necesita para pintar la sesion (rol, empresa).
    // El rol y la empresa que se aplican de verdad se leen de la BD en cada peticion (ver el filtro).
    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_TIPO, TIPO_ACCESS)
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRolAcceso().name())
                .claim("empresaId", usuario.getEmpresa().getId())
                .claim(CLAIM_VERSION, usuario.getVersionSesion())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(accessExpirationMs)))
                .signWith(secretKey)
                .compact();
    }

    // Token largo: minimo de datos (quien es, a que familia pertenece y su id unico "jti").
    // El jti se guarda en la BD para poder rotarlo y revocarlo.
    public RefreshEmitido generarRefreshToken(Usuario usuario, String familiaId) {
        return generarRefreshToken(usuario, familiaId, Instant.now().plusMillis(refreshExpirationMs));
    }

    // Las rotaciones conservan el vencimiento original: una familia dura como maximo siete dias.
    public RefreshEmitido generarRefreshToken(Usuario usuario, String familiaId, Instant expira) {
        Instant ahora = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(ISSUER)
                .id(jti)
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .claim(CLAIM_FAMILIA, familiaId)
                .claim(CLAIM_VERSION, usuario.getVersionSesion())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(secretKey)
                .compact();
        return new RefreshEmitido(token, jti, ahora, expira);
    }

    // Lanza JwtException si el token esta mal firmado, vencido o no es un access token
    public Claims leerAccessClaims(String token) {
        return leer(token, TIPO_ACCESS);
    }

    // Lanza JwtException si el token esta mal firmado, vencido o no es un refresh token
    public Claims leerRefreshClaims(String token) {
        return leer(token, TIPO_REFRESH);
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private Claims leer(String token, String tipoEsperado) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (!tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new JwtException("Tipo de token incorrecto");
        }
        try {
            if (claims.getExpiration() == null || claims.getIssuedAt() == null
                    || !claims.getExpiration().after(claims.getIssuedAt())
                    || claims.getIssuedAt().toInstant().isAfter(Instant.now().plusSeconds(30))
                    || Integer.parseInt(claims.getSubject()) <= 0
                    || claims.get(CLAIM_VERSION, Integer.class) == null
                    || claims.get(CLAIM_VERSION, Integer.class) < 0) {
                throw new JwtException("Claims de sesion incompletos o invalidos");
            }
            if (TIPO_REFRESH.equals(tipoEsperado)) {
                UUID.fromString(claims.getId());
                UUID.fromString(claims.get(CLAIM_FAMILIA, String.class));
            }
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new JwtException("Claims de sesion invalidos", ex);
        }
        return claims;
    }

    public record RefreshEmitido(String token, String jti, Instant emitidoEn, Instant expiraEn) {
    }
}
