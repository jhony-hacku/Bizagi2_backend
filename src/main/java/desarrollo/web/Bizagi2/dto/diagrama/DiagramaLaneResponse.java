package desarrollo.web.Bizagi2.dto.diagrama;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Los datos del rol se omiten (null) cuando el proceso es compartido
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagramaLaneResponse {
    private Integer id;
    private String nombre;
    private int orden;
    private Integer rolProcesoId;
    private String rolProceso;
}
