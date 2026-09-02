package desarrollo.web.Bizagi2.application.usecase;

import desarrollo.web.Bizagi2.application.dto.RegisterRequest;
import desarrollo.web.Bizagi2.application.dto.RegisterResponse;
import desarrollo.web.Bizagi2.application.exception.EmailAlreadyExistsException;
import desarrollo.web.Bizagi2.domain.model.User;
import desarrollo.web.Bizagi2.domain.model.UserRole;
import desarrollo.web.Bizagi2.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse execute(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(
                null,
                request.getUsername(),
                request.getEmail(),
                passwordHash,
                UserRole.USER,
                LocalDateTime.now()
        );
        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
}
