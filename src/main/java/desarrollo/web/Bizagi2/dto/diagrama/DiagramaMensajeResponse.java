package desarrollo.web.Bizagi2.dto.diagrama;

import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaMensajeResponse {
    private Integer id;
    private String nombre;
    private Integer origenId;
    private Integer origenPoolId;
    private Integer destinoPoolId;
    private String contenido;
    private TipoDestino tipoDestino;
    private String momento;
    private AccionFallo accionFallo;
    private Integer actividadErrorId;
    private DiagramaCorrelacionResponse correlacion;
}
