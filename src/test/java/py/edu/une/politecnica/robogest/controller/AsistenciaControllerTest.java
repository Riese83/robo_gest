package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dto.MarcacionRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionResponseDTO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;
import py.edu.une.politecnica.robogest.service.AsistenciaService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaControllerTest {

    @Mock
    private AsistenciaService asistenciaService;

    private AsistenciaController asistenciaController;

    @BeforeEach
    void setUp() {
        asistenciaController = new AsistenciaController(asistenciaService);
    }

    @Test
    @DisplayName("Debe registrar marcacion para ESP32 y retornar HTTP 200 con MarcacionResponseDTO")
    void testMarcar_Exitoso() {
        Integrante integrante = new Integrante(
                "Esteban", "Duarte", "5001002", "FP-2024-55", "0991000111",
                "esteban@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "E1E2E3E4", CarreraEnum.INGENIERIA_ELECTRICA, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(8L);

        Asistencia asistencia = new Asistencia(
                integrante, LocalDateTime.now(), TipoMarcacionEnum.ENTRADA, "ESP32_LAB_01"
        );
        asistencia.setId(100L);

        when(asistenciaService.registrarMarcacion("E1E2E3E4", "ESP32_LAB_01")).thenReturn(asistencia);

        MarcacionRequestDTO request = new MarcacionRequestDTO("E1E2E3E4", "ESP32_LAB_01");
        Response response = asistenciaController.marcar(request);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(MarcacionResponseDTO.class, response.getEntity());

        MarcacionResponseDTO dto = (MarcacionResponseDTO) response.getEntity();
        assertEquals(100L, dto.getAsistenciaId());
        assertEquals("ENTRADA", dto.getTipoMarcacion());
        assertEquals("Esteban Duarte", dto.getNombreIntegrante());
        assertTrue(dto.getMensaje().contains("ENTRADA"));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si falta el nfcUid")
    void testMarcar_SinNfcUid() {
        MarcacionRequestDTO request = new MarcacionRequestDTO(null, "ESP32_LAB_01");
        assertThrows(IllegalArgumentException.class, () -> asistenciaController.marcar(request));
    }
}
