package desarrollo.web.Bizagi2.dto.mensaje;

import desarrollo.web.Bizagi2.entities.AccionFallo;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {
    private Integer id;
    private Integer procesoId;
    private String nombre;
    private Integer origenId;
    private String origenNombre;
    private Integer destinoPoolId;
    private String destinoPoolNombre;
    private String contenido;
    private TipoDestino tipoDestino;
    private String momento;
    private AccionFallo accionFallo;
    private Integer actividadErrorId;
    private String actividadErrorNombre;
}
