package desarrollo.web.Bizagi2.entities.domain.repository;

import desarrollo.web.Bizagi2.entities.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    User save(User user);

    boolean existsByEmail(String email);
}
