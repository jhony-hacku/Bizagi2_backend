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

// HU-22: la lane pertenece a un pool y esta asociada a un rol de proceso de la empresa.
// Las actividades de la lane heredan ese rol como responsable.
@Entity
@Table(name = "lanes")
@SQLRestriction("activo = true")
@Data
@NoArgsConstructor
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso;

    private String nombre;

    // Posicion de la lane dentro del pool (de arriba hacia abajo)
    @Column(nullable = false)
    @ColumnDefault("0")
    private int orden;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
