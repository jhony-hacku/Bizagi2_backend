package desarrollo.web.Bizagi2.application.dto;

import desarrollo.web.Bizagi2.domain.model.UserRole;

public class LoginResponse {
    private final String token;
    private final Long userId;
    private final String username;
    private final UserRole role;

    public LoginResponse(String token, Long userId, String username, UserRole role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }
}
