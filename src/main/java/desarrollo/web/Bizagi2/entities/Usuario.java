package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
import lombok.ToString;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // WRITE_ONLY: se acepta al recibir el JSON pero nunca se devuelve al cliente
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String password;

    @Enumerated(EnumType.STRING)
    private RolAcceso rolAcceso;

    // Un usuario desactivado no puede entrar, pero su historial y sus procesos se conservan
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

    // Va dentro del token. Al cerrar sesion se incrementa y los tokens anteriores dejan de servir
    @JsonIgnore
    @Column(name = "version_sesion", nullable = false)
    @ColumnDefault("0")
    private int versionSesion = 0;
}
