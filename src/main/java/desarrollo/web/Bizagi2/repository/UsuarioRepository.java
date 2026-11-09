package desarrollo.web.Bizagi2.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import desarrollo.web.Bizagi2.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findByEmail(String email);

    // Todas las escrituras del usuario y de sus sesiones toman este mismo bloqueo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Integer id);

    boolean existsByEmail(String email);

    List<Usuario> findByEmpresaId(Integer empresaId);
}
