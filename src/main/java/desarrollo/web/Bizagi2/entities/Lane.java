package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU-22: la lane pertenece a un pool y esta asociada a un rol de proceso de la empresa.
// Las actividades de la lane heredan ese rol como responsable.
@Entity
@Table(name = "lanes")
@SQLRestriction("activo = true")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Lane {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso;

    @Column(nullable = false)
    private String nombre;

    // Posicion de la lane dentro del pool (de arriba hacia abajo)
    @Column(nullable = false)
    @ColumnDefault("0")
    private int orden;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

    // Control de concurrencia optimista: si dos transacciones modifican la misma fila,
    // la segunda falla con OptimisticLockException en lugar de sobrescribir los cambios
    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private long version;
}
