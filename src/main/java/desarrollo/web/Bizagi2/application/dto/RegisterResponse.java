package desarrollo.web.Bizagi2.application.dto;

import desarrollo.web.Bizagi2.domain.model.UserRole;

public class RegisterResponse {
    private final Long userId;
    private final String username;
    private final String email;
    private final UserRole role;

    public RegisterResponse(Long userId, String username, String email, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
