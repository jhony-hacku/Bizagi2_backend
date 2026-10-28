package desarrollo.web.Bizagi2.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Cuerpo uniforme de error (equivale al ErrorDto del material del curso).
// "errores" solo aparece en fallos de validacion: campo -> mensaje
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String message;
    private String timestamp;
    private Map<String, String> errores;
}
