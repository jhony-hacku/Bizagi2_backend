package desarrollo.web.Bizagi2.application.usecase;

import desarrollo.web.Bizagi2.application.dto.RegisterRequest;
import desarrollo.web.Bizagi2.application.dto.RegisterResponse;
import desarrollo.web.Bizagi2.application.exception.EmailAlreadyExistsException;
import desarrollo.web.Bizagi2.domain.model.User;
import desarrollo.web.Bizagi2.domain.model.UserRole;
import desarrollo.web.Bizagi2.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("MySecurePassword123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MySecurePassword123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(
                new User(1L, "john", "john@example.com", "hashed-password", UserRole.USER, LocalDateTime.now())
        );

        RegisterResponse response = registerUserUseCase.execute(request);

        assertEquals(1L, response.getUserId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
        assertNotEquals(request.getPassword(), "hashed-password");
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("MySecurePassword123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> registerUserUseCase.execute(request));
    }
}
