package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaServiceTest - Pruebas basadas en HU-01 y HU-24")
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    @Test
    @DisplayName("HU-01 CA1 & CA4: Crear empresa con datos válidos registra la entidad independiente")
    void crear_datosValidos_exito() {
        when(empresaRepository.existsByNit("900123456-1")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa e = invocation.getArgument(0);
            e.setId(1);
            return e;
        });

        Empresa resultado = empresaService.crear("Mi Empresa", "900123456-1", "contacto@miempresa.com");

        assertThat(resultado.getId()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Mi Empresa");
        assertThat(resultado.getNit()).isEqualTo("900123456-1");
        assertThat(resultado.getEmailContacto()).isEqualTo("contacto@miempresa.com");
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("HU-01 CA2: Crear empresa con NIT existente lanza ConflictoDominioException")
    void crear_nitDuplicado_lanzaConflicto() {
        when(empresaRepository.existsByNit("900123456-1")).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear("Empresa 2", "900123456-1", "correo@empresa.com"))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe una empresa registrada con ese NIT");
    }

    @Test
    @DisplayName("HU-01 CA5: Crear empresa con campos obligatorios nulos o vacíos lanza ReglaNegocioException")
    void crear_camposFaltantes_lanzaReglaNegocio() {
        assertThatThrownBy(() -> empresaService.crear(null, "900", "test@test.com"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nombreEmpresa");

        assertThatThrownBy(() -> empresaService.crear("Nombre", "   ", "test@test.com"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nit");

        assertThatThrownBy(() -> empresaService.crear("Nombre", "900", ""))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("emailContacto");
    }

    @Test
    @DisplayName("HU-01 CA5: Crear empresa con email inválido lanza ReglaNegocioException")
    void crear_emailInvalido_lanzaReglaNegocio() {
        assertThatThrownBy(() -> empresaService.crear("Nombre", "900", "invalido"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("El formato del email no es valido");
    }

    @Test
    @DisplayName("Buscar empresa por ID existente retorna la entidad")
    void buscarPorId_existente_retornaEmpresa() {
        Empresa e = new Empresa();
        e.setId(5);
        e.setNombre("Empresa 5");

        when(empresaRepository.findById(5)).thenReturn(Optional.of(e));

        Empresa resultado = empresaService.buscarPorId(5);
        assertThat(resultado.getId()).isEqualTo(5);
        assertThat(resultado.getNombre()).isEqualTo("Empresa 5");
    }

    @Test
    @DisplayName("Buscar empresa por ID inexistente lanza RecursoNoEncontradoException")
    void buscarPorId_inexistente_lanzaRecursoNoEncontrado() {
        when(empresaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empresaService.buscarPorId(99))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Empresa no encontrada");
    }

    @Test
    @DisplayName("HU-24: Actualizar empresa y permiso de editorModificaEstructura")
    void actualizar_datosValidos_actualizaCorrectamente() {
        Empresa existente = new Empresa();
        existente.setId(1);
        existente.setNombre("Antiguo");
        existente.setNit("111");
        existente.setEmailContacto("old@test.com");
        existente.setEditorModificaEstructura(false);

        when(empresaRepository.findById(1)).thenReturn(Optional.of(existente));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        Empresa nuevosDatos = new Empresa();
        nuevosDatos.setNombre("Nuevo Nombre");
        nuevosDatos.setNit("111"); // mismo nit
        nuevosDatos.setEmailContacto("new@test.com");
        nuevosDatos.setEditorModificaEstructura(true);

        Empresa actualizada = empresaService.actualizar(1, nuevosDatos);

        assertThat(actualizada.getNombre()).isEqualTo("Nuevo Nombre");
        assertThat(actualizada.getEmailContacto()).isEqualTo("new@test.com");
        assertThat(actualizada.getEditorModificaEstructura()).isTrue();
    }

    @Test
    @DisplayName("Actualizar empresa con NIT que ya pertenece a otra empresa lanza ConflictoDominioException")
    void actualizar_nitCambiaADuplicado_lanzaConflicto() {
        Empresa existente = new Empresa();
        existente.setId(1);
        existente.setNombre("Empresa 1");
        existente.setNit("111");
        existente.setEmailContacto("test@test.com");

        when(empresaRepository.findById(1)).thenReturn(Optional.of(existente));
        when(empresaRepository.existsByNit("222")).thenReturn(true);

        Empresa nuevosDatos = new Empresa();
        nuevosDatos.setNombre("Empresa 1");
        nuevosDatos.setNit("222"); // nit en conflicto
        nuevosDatos.setEmailContacto("test@test.com");

        assertThatThrownBy(() -> empresaService.actualizar(1, nuevosDatos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("Ya existe una empresa registrada con ese NIT");
    }
}
