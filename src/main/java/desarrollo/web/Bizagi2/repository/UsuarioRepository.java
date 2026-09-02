package desarrollo.web.Bizagi2.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
}
