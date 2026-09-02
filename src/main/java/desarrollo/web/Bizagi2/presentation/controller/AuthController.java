package desarrollo.web.Bizagi2.presentation.controller;

import desarrollo.web.Bizagi2.application.dto.LoginRequest;
import desarrollo.web.Bizagi2.application.dto.LoginResponse;
import desarrollo.web.Bizagi2.application.dto.RegisterRequest;
import desarrollo.web.Bizagi2.application.dto.RegisterResponse;
import desarrollo.web.Bizagi2.application.usecase.LoginUseCase;
import desarrollo.web.Bizagi2.application.usecase.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
    }

    @Operation(
            summary = "Register new user",
            description = "Creates a new user with USER role and securely hashes the password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "409", description = "Email already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerUserUseCase.execute(request);
    }

    @Operation(
            summary = "Login user",
            description = "Validates credentials and returns a JWT token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    value = "{\"email\":\"john@example.com\",\"password\":\"MySecurePassword123\"}"
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }
}
