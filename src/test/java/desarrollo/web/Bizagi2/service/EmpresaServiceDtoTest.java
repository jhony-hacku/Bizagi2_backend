package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.DatosDto.EMPRESA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import desarrollo.web.Bizagi2.config.ModelMapperConfig;
import desarrollo.web.Bizagi2.dto.empresa.EmpresaRequest;
import desarrollo.web.Bizagi2.dto.empresa.EmpresaResponse;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaService - conversion entre DTOs y entidades")
class EmpresaServiceDtoTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Spy
    private ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @InjectMocks
    private EmpresaService servicio;

    private EmpresaService empresaService;

    @BeforeEach
    void setUp() {
        empresaService = spy(servicio);
    }

    @Test
    @DisplayName("buscarPorIdDto devuelve los datos de la empresa")
    void buscarPorIdDto() {
        doReturn(DatosDto.empresa()).when(empresaService).buscarPorId(EMPRESA_ID);

        EmpresaResponse dto = empresaService.buscarPorIdDto(EMPRESA_ID);

        assertThat(dto.getNit()).isEqualTo("900123456");
        assertThat(dto.getEditorModificaEstructura()).isTrue();
    }

    @Test
    @DisplayName("actualizar con EmpresaRequest devuelve la empresa modificada")
    void actualizar() {
        doReturn(DatosDto.empresa()).when(empresaService).actualizar(eq(EMPRESA_ID), any(Empresa.class));

        EmpresaResponse dto = empresaService.actualizar(EMPRESA_ID,
                new EmpresaRequest("Acme SAS", "900123456", "contacto@acme.com", true));

        assertThat(dto.getNombre()).isEqualTo("Acme SAS");
    }
}
