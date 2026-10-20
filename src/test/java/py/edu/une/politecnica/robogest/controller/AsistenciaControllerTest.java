package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dto.MarcacionManualRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionResponseDTO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;
import py.edu.une.politecnica.robogest.security.SecurityUtils;
import py.edu.une.politecnica.robogest.service.AsistenciaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaControllerTest {

    @Mock
    private AsistenciaService asistenciaService;

    @Mock
    private py.edu.une.politecnica.robogest.dao.AsistenciaDAO asistenciaDAO;

    @Mock
    private py.edu.une.politecnica.robogest.dao.IntegranteDAO integranteDAO;

    private AsistenciaController asistenciaController;

    @BeforeEach
    void setUp() {
        asistenciaController = new AsistenciaController(asistenciaService, asistenciaDAO, integranteDAO);
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

    @Test
    @DisplayName("Debe obtener el ultimo UID escaneado despues de una marcacion")
    void testObtenerUltimoEscaneado() {
        Integrante integrante = new Integrante(
                "Test", "User", "123456", "FP-001", "0981", "t@f.com",
                LocalDate.now(), EstadoIntegranteEnum.ACTIVO, "CARD1234",
                CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(1L);
        Asistencia asistencia = new Asistencia(integrante, LocalDateTime.now(), TipoMarcacionEnum.ENTRADA, "ESP32_AUTO");
        asistencia.setId(10L);

        when(asistenciaService.registrarMarcacion("CARD1234", "ESP32_AUTO")).thenReturn(asistencia);

        asistenciaController.marcar(new MarcacionRequestDTO("CARD1234", "ESP32_AUTO"));

        Response response = asistenciaController.obtenerUltimoEscaneado();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(java.util.Map.class, response.getEntity());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("CARD1234", map.get("nfcUid"));
        assertEquals("ESP32_AUTO", map.get("dispositivo"));
        assertNotNull(map.get("timestamp"));
    }

    @Test
    @DisplayName("Debe registrar marcacion manual para integrante existente encontrado por CI")
    void testMarcarManual_ExistentePorCi() {
        Integrante integrante = new Integrante(
                "Maria", "Gonzalez", "4567890", "FP-2023-10", "0981112233",
                "mgonzalez@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "UID_MG_01", CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(25L);

        when(integranteDAO.findByCi("4567890")).thenReturn(Optional.of(integrante));
        when(asistenciaDAO.findUltimaMarcacionDelDia(eq(25L), any(LocalDate.class))).thenReturn(Optional.empty());

        Asistencia guardada = new Asistencia(integrante, LocalDateTime.now(), TipoMarcacionEnum.ENTRADA, "REGISTRO_MANUAL_LAB");
        guardada.setId(201L);
        when(asistenciaDAO.save(any(Asistencia.class))).thenReturn(guardada);

        MarcacionManualRequestDTO request = new MarcacionManualRequestDTO();
        request.setCi("4567890");
        request.setTipoMarcacion("AUTO");

        Response response = asistenciaController.marcarManual(request, SecurityUtils.INTERNAL_ADMIN_TOKEN);

        assertEquals(200, response.getStatus());
        assertInstanceOf(MarcacionResponseDTO.class, response.getEntity());
        MarcacionResponseDTO dto = (MarcacionResponseDTO) response.getEntity();
        assertEquals(201L, dto.getAsistenciaId());
        assertEquals(25L, dto.getIntegranteId());
        assertEquals("ENTRADA", dto.getTipoMarcacion());
        assertTrue(dto.getMensaje().contains("Maria"));
    }

    @Test
    @DisplayName("Debe auto-registrar nuevo participante cuando no existe y se provee CI y Nombre")
    void testMarcarManual_NuevoParticipante() {
        when(integranteDAO.findByCi("9998887")).thenReturn(Optional.empty());

        Integrante nuevoGuardado = new Integrante(
                "Carlos", "Lopez", "9998887", "ASIST-9998887", null,
                "asistente_9998887@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                null, CarreraEnum.INGENIERIA_ELECTRICA, RolEnum.MIEMBRO, "hash"
        );
        nuevoGuardado.setId(77L);
        when(integranteDAO.save(any(Integrante.class))).thenReturn(nuevoGuardado);

        Asistencia guardada = new Asistencia(nuevoGuardado, LocalDateTime.now(), TipoMarcacionEnum.ENTRADA, "MANUAL_RECEPCION");
        guardada.setId(301L);
        when(asistenciaDAO.save(any(Asistencia.class))).thenReturn(guardada);

        MarcacionManualRequestDTO request = new MarcacionManualRequestDTO();
        request.setNombre("Carlos Lopez");
        request.setCi("9998887");
        request.setCarrera("INGENIERIA_ELECTRICA");
        request.setTipoMarcacion("ENTRADA");
        request.setDispositivo("MANUAL_RECEPCION");

        Response response = asistenciaController.marcarManual(request, SecurityUtils.INTERNAL_ADMIN_TOKEN);

        assertEquals(200, response.getStatus());
        assertInstanceOf(MarcacionResponseDTO.class, response.getEntity());
        MarcacionResponseDTO dto = (MarcacionResponseDTO) response.getEntity();
        assertEquals(301L, dto.getAsistenciaId());
        assertEquals(77L, dto.getIntegranteId());
        verify(integranteDAO, times(1)).save(any(Integrante.class));
    }

    @Test
    @DisplayName("Debe fallar al marcar manual si no existe y no se especifica CI o Nombre")
    void testMarcarManual_FaltaDatosObligatorios() {
        MarcacionManualRequestDTO request = new MarcacionManualRequestDTO();
        request.setNombre("Solo Nombre");

        assertThrows(IllegalArgumentException.class, () -> asistenciaController.marcarManual(request, SecurityUtils.INTERNAL_ADMIN_TOKEN));
    }

    @Test
    @DisplayName("Debe calcular correctamente horas, sesiones y Tier a partir de marcaciones de asistencia")
    void testCalcularHorasParaIntegrante_SesionesCompletasYTier() {
        Integrante integrante = new Integrante(
                "Maria", "Gomez", "4445556", "FP-2023-12", "0981999888",
                "maria@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "TAG_MARIA", CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(25L);

        // Día 1: 4 horas (08:00 a 12:00)
        LocalDate dia1 = LocalDate.now().minusDays(5);
        Asistencia e1 = new Asistencia(integrante, dia1.atTime(8, 0), TipoMarcacionEnum.ENTRADA, "ESP32");
        Asistencia s1 = new Asistencia(integrante, dia1.atTime(12, 0), TipoMarcacionEnum.SALIDA, "ESP32");

        // Día 2: 7 horas (13:00 a 20:00)
        LocalDate dia2 = LocalDate.now().minusDays(3);
        Asistencia e2 = new Asistencia(integrante, dia2.atTime(13, 0), TipoMarcacionEnum.ENTRADA, "ESP32");
        Asistencia s2 = new Asistencia(integrante, dia2.atTime(20, 0), TipoMarcacionEnum.SALIDA, "ESP32");

        py.edu.une.politecnica.robogest.dto.HorasIntegranteDTO dto =
                AsistenciaController.calcularHorasParaIntegrante(integrante, java.util.List.of(e1, s1, e2, s2), 1);

        assertNotNull(dto);
        assertEquals(25L, dto.getIntegranteId());
        assertEquals("Maria Gomez", dto.getNombreCompleto());
        assertEquals(11.0, dto.getHorasTotales());
        assertEquals(2, dto.getTotalSesiones());
        assertEquals("PLATA", dto.getTier());
        assertTrue(dto.getTierNombre().contains("Tier Plata"));
    }

    @Test
    @DisplayName("Debe computar 2 horas estandar para entradas pasadas sin marcacion de salida")
    void testCalcularHorasParaIntegrante_EntradaSinSalidaPasada() {
        Integrante integrante = new Integrante(
                "Lucas", "Benitez", "3332221", "FP-2022-01", "0981111222",
                "lucas@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "TAG_LUCAS", CarreraEnum.INGENIERIA_ELECTRICA, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(30L);

        LocalDate diaPasado = LocalDate.now().minusDays(2);
        Asistencia e1 = new Asistencia(integrante, diaPasado.atTime(9, 0), TipoMarcacionEnum.ENTRADA, "ESP32");

        py.edu.une.politecnica.robogest.dto.HorasIntegranteDTO dto =
                AsistenciaController.calcularHorasParaIntegrante(integrante, java.util.List.of(e1), 1);

        assertNotNull(dto);
        assertEquals(2.0, dto.getHorasTotales());
        assertEquals("BRONCE", dto.getTier());
    }

    @Test
    @DisplayName("Debe retornar HTTP 200 y ResumenHorasDashboardDTO con ranking para rol administrativo")
    void testObtenerHorasDashboard_AdminExitoso() {
        Integrante admin = new Integrante(
                "Admin", "Club", "1234567", "FP-001", "0981",
                "admin@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "ADMIN_TAG", CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.ADMIN, "hash"
        );
        admin.setId(1L);

        when(integranteDAO.findAll()).thenReturn(java.util.List.of(admin));
        when(asistenciaDAO.findAll()).thenReturn(java.util.Collections.emptyList());

        Response response = asistenciaController.obtenerHorasDashboard(SecurityUtils.INTERNAL_ADMIN_TOKEN);

        assertEquals(200, response.getStatus());
        assertInstanceOf(py.edu.une.politecnica.robogest.dto.ResumenHorasDashboardDTO.class, response.getEntity());
        py.edu.une.politecnica.robogest.dto.ResumenHorasDashboardDTO dto =
                (py.edu.une.politecnica.robogest.dto.ResumenHorasDashboardDTO) response.getEntity();
        assertTrue(dto.isEsUsuarioAutorizado());
        assertNotNull(dto.getRanking());
    }
}
