package desarrollo.web.Bizagi2.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import desarrollo.web.Bizagi2.entities.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    // Atomico: devuelve 1 solo para la primera peticion que usa el token. Si dos peticiones
    // llegan a la vez con el mismo refresh token, la segunda recibe 0 y se trata como reutilizacion.
    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken r set r.revocado = true where r.jti = :jti and r.revocado = false")
    int revocarSiVigente(@Param("jti") String jti);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken r set r.revocado = true where r.familiaId = :familiaId and r.revocado = false")
    int revocarFamilia(@Param("familiaId") String familiaId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken r where r.usuario.id = :usuarioId")
    int eliminarDeUsuario(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("delete from RefreshToken r where r.expiraEn < :limite")
    int eliminarVencidosAntesDe(@Param("limite") Instant limite);
}
