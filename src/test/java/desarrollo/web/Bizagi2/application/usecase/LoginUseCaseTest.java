package desarrollo.web.Bizagi2.application.usecase;

import desarrollo.web.Bizagi2.application.dto.LoginRequest;
import desarrollo.web.Bizagi2.application.dto.LoginResponse;
import desarrollo.web.Bizagi2.application.exception.InvalidCredentialsException;
import desarrollo.web.Bizagi2.domain.model.User;
import desarrollo.web.Bizagi2.domain.model.UserRole;
import desarrollo.web.Bizagi2.domain.repository.UserRepository;
import desarrollo.web.Bizagi2.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("MySecurePassword123");

        User user = new User(1L, "john", "john@example.com", "hashed-password", UserRole.USER, LocalDateTime.now());

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("MySecurePassword123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = loginUseCase.execute(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("john", response.getUsername());
        assertEquals(UserRole.USER, response.getRole());
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("MySecurePassword123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(request));
    }

    @Test
    void shouldFailWhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("WrongPassword123");

        User user = new User(1L, "john", "john@example.com", "hashed-password", UserRole.USER, LocalDateTime.now());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(request));
    }
}
