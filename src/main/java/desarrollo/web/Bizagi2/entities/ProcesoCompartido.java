package desarrollo.web.Bizagi2.entities;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// HU-23: un proceso compartido en SOLO LECTURA con otra empresa (la invitada)
@Entity
@Table(name = "procesos_compartidos")
@Data
@NoArgsConstructor
public class ProcesoCompartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private Instant fecha;

    // Dejar de compartir es logico: se conserva el registro
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
