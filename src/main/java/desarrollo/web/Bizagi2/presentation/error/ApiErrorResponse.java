package desarrollo.web.Bizagi2.presentation.error;

public class ApiErrorResponse {
    private final int status;
    private final String message;
    private final String timestamp;

    public ApiErrorResponse(int status, String message, String timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
