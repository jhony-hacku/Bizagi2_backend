package desarrollo.web.Bizagi2.dto.diagrama;

import java.util.List;

import desarrollo.web.Bizagi2.dto.nodo.EventoResponse;
import desarrollo.web.Bizagi2.dto.nodo.GatewayResponse;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaPoolResponse {
    private Integer id;
    private String nombre;
    private TipoParticipante tipoParticipante;
    private List<DiagramaLaneResponse> lanes;
    private List<DiagramaActividadResponse> actividades;
    private List<GatewayResponse> gateways;
    private List<EventoResponse> eventos;
    private List<DiagramaArcoResponse> arcos;
}
