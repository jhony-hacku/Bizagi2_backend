package desarrollo.web.Bizagi2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Registro de cada refresh token emitido. Solo se guarda su id (jti), no el token: el token va firmado,
// asi que quien lea esta tabla no puede fabricar uno valido.
// Todos los tokens que nacen de un mismo login comparten "familiaId": si se reutiliza uno ya rotado
// (señal de robo) se cierran todas las sesiones del usuario.
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_familia", columnList = "familia_id"),
        @Index(name = "idx_refresh_usuario", columnList = "usuario_id"),
        @Index(name = "idx_refresh_expira", columnList = "expira_en")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "familia_id", nullable = false, length = 36)
    private String familiaId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    // true cuando ya se uso (rotado), se cerro la sesion o se detecto reutilizacion
    @Column(nullable = false)
    private boolean revocado;
}
