package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ACTIVIDAD")
@Getter
@Setter
@NoArgsConstructor
public class Actividad extends NodoFlujo {

    // Con SINGLE_TABLE estas columnas tambien existen para las filas de otros nodos,
    // por eso no pueden ser nullable = false. La obligatoriedad se valida en NodoFlujoService.
    @Enumerated(EnumType.STRING)
    private TipoActividad tipo;

    // El responsable de la actividad es el rol de esta lane (HU-22)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id")
    private Lane lane;

    private String descripcion;
}
