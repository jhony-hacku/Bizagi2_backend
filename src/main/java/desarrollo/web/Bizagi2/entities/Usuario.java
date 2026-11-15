package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Usuario {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // Se guarda cifrada con BCrypt. Ningun DTO de respuesta la incluye
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolAcceso rolAcceso;

    // Un usuario desactivado no puede entrar, pero su historial y sus procesos se conservan
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

    // Va dentro del token. Al cerrar sesion se incrementa y los tokens anteriores dejan de servir
    @Column(name = "version_sesion", nullable = false)
    @ColumnDefault("0")
    private int versionSesion = 0;

    // Control de concurrencia optimista: si dos transacciones modifican la misma fila,
    // la segunda falla con OptimisticLockException en lugar de sobrescribir los cambios
    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private long version;
}
