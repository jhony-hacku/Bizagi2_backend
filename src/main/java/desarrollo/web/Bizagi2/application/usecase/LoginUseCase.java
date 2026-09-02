package desarrollo.web.Bizagi2.application.usecase;

import desarrollo.web.Bizagi2.application.dto.LoginRequest;
import desarrollo.web.Bizagi2.application.dto.LoginResponse;
import desarrollo.web.Bizagi2.application.exception.InvalidCredentialsException;
import desarrollo.web.Bizagi2.domain.model.User;
import desarrollo.web.Bizagi2.domain.repository.UserRepository;
import desarrollo.web.Bizagi2.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
