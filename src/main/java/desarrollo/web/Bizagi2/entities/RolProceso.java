package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

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

// HU-17: el rol de proceso es de la EMPRESA y se reutiliza en los lanes de cualquiera de sus procesos
@Entity
@Table(name = "roles_proceso")
@SQLRestriction("activo = true")
@Data
@NoArgsConstructor
public class RolProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String nombre;
    private String descripcion;

    // Eliminacion logica: el rol pasa a inactivo y las consultas normales ya no lo ven
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
