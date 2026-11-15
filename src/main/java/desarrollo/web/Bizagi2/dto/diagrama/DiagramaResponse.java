package desarrollo.web.Bizagi2.dto.diagrama;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-07: el diagrama completo del proceso en una sola respuesta
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaResponse {
    private DiagramaProcesoResponse proceso;
    private List<DiagramaPoolResponse> pools;
    private List<DiagramaMensajeResponse> mensajes;
}
