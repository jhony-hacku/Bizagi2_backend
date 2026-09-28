package desarrollo.web.Bizagi2.entities;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Trazabilidad: una fila por cada cambio (de un proceso, de cualquier elemento de su diagrama o de un rol)
@Entity
@Table(name = "historial")
@Data
@NoArgsConstructor
public class Historial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Proceso al que pertenece el cambio. Vacio en los cambios de roles, que son de la empresa.
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "proceso_id")
    private Proceso proceso;

    @JsonIgnoreProperties("empresa")
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionHistorial accion;

    // Que se cambio: PROCESO, POOL, LANE, ACTIVIDAD, GATEWAY, EVENTO, ARCO, MENSAJE, CORRELACION, ROL
    @Column(nullable = false, length = 30)
    private String entidad;

    private Integer entidadId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 2000)
    private String detalle;
}
