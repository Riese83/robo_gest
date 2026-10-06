package py.edu.une.politecnica.robogest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.AsistenciaDAO;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;
import py.edu.une.politecnica.robogest.exception.IntegranteInactivoException;
import py.edu.une.politecnica.robogest.exception.IntegranteNoEncontradoException;
import py.edu.une.politecnica.robogest.service.impl.AsistenciaServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceImplTest {

    @Mock
    private IntegranteDAO integranteDAO;

    @Mock
    private AsistenciaDAO asistenciaDAO;

    private AsistenciaServiceImpl asistenciaService;

    @BeforeEach
    void setUp() {
        asistenciaService = new AsistenciaServiceImpl(integranteDAO, asistenciaDAO);
    }

    private Integrante crearIntegrante(EstadoIntegranteEnum estado) {
        Integrante integrante = new Integrante(
                "Fernando", "Rios", "5123456", "FP-2024-02", "0991888777",
                "fernando.rios@fpune.edu.py", LocalDate.now(), estado,
                "11223344", CarreraEnum.ANALISIS_DE_SISTEMAS, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(20L);
        return integrante;
    }

    @Test
    @DisplayName("Debe registrar ENTRADA como primera marcacion del dia")
    void testRegistrarMarcacion_PrimeraDelDia_Entrada() {
        Integrante integrante = crearIntegrante(EstadoIntegranteEnum.ACTIVO);
        when(integranteDAO.findByNfcUid("11223344")).thenReturn(Optional.of(integrante));
        when(asistenciaDAO.findUltimaMarcacionDelDia(eq(20L), any(LocalDate.class))).thenReturn(Optional.empty());

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        when(asistenciaDAO.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Asistencia registrada = asistenciaService.registrarMarcacion("11223344", "ESP32_PUERTA_PRINCIPAL");

        assertNotNull(registrada);
        assertEquals(TipoMarcacionEnum.ENTRADA, registrada.getTipoMarcacion());
        assertEquals("ESP32_PUERTA_PRINCIPAL", registrada.getDispositivoUtilizado());
        assertEquals(integrante, registrada.getIntegrante());
    }

    @Test
    @DisplayName("Debe alternar a SALIDA cuando la ultima marcacion del dia fue ENTRADA")
    void testRegistrarMarcacion_AlternaASalida() {
        Integrante integrante = crearIntegrante(EstadoIntegranteEnum.ACTIVO);
        Asistencia marcacionPrevia = new Asistencia(integrante, LocalDateTime.now().minusHours(4), TipoMarcacionEnum.ENTRADA, "ESP32_PUERTA_PRINCIPAL");

        when(integranteDAO.findByNfcUid("11223344")).thenReturn(Optional.of(integrante));
        when(asistenciaDAO.findUltimaMarcacionDelDia(eq(20L), any(LocalDate.class))).thenReturn(Optional.of(marcacionPrevia));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        when(asistenciaDAO.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Asistencia registrada = asistenciaService.registrarMarcacion("11223344", "ESP32_PUERTA_PRINCIPAL");

        assertNotNull(registrada);
        assertEquals(TipoMarcacionEnum.SALIDA, registrada.getTipoMarcacion());
    }

    @Test
    @DisplayName("Debe alternar a ENTRADA cuando la ultima marcacion del dia fue SALIDA")
    void testRegistrarMarcacion_AlternaAEntrada() {
        Integrante integrante = crearIntegrante(EstadoIntegranteEnum.ACTIVO);
        Asistencia marcacionPrevia = new Asistencia(integrante, LocalDateTime.now().minusHours(2), TipoMarcacionEnum.SALIDA, "ESP32_PUERTA_PRINCIPAL");

        when(integranteDAO.findByNfcUid("11223344")).thenReturn(Optional.of(integrante));
        when(asistenciaDAO.findUltimaMarcacionDelDia(eq(20L), any(LocalDate.class))).thenReturn(Optional.of(marcacionPrevia));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        when(asistenciaDAO.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Asistencia registrada = asistenciaService.registrarMarcacion("11223344", "ESP32_LAB_ROBOTICA");

        assertNotNull(registrada);
        assertEquals(TipoMarcacionEnum.ENTRADA, registrada.getTipoMarcacion());
        assertEquals("ESP32_LAB_ROBOTICA", registrada.getDispositivoUtilizado());
    }

    @Test
    @DisplayName("Debe lanzar IntegranteInactivoException cuando el integrante esta INACTIVO")
    void testRegistrarMarcacion_IntegranteInactivo() {
        Integrante integrante = crearIntegrante(EstadoIntegranteEnum.INACTIVO);
        when(integranteDAO.findByNfcUid("11223344")).thenReturn(Optional.of(integrante));

        assertThrows(IntegranteInactivoException.class, () ->
                asistenciaService.registrarMarcacion("11223344", "ESP32_PUERTA"));

        verify(asistenciaDAO, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar IntegranteNoEncontradoException cuando el nfcUid no esta registrado")
    void testRegistrarMarcacion_NoEncontrado() {
        when(integranteDAO.findByNfcUid("DESCONOCIDO")).thenReturn(Optional.empty());

        assertThrows(IntegranteNoEncontradoException.class, () ->
                asistenciaService.registrarMarcacion("DESCONOCIDO", "ESP32_PUERTA"));

        verify(asistenciaDAO, never()).save(any());
    }
}
