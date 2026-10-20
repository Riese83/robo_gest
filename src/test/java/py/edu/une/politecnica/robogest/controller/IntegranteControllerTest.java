package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.dto.IntegranteRequestDTO;
import py.edu.une.politecnica.robogest.dto.IntegranteResponseDTO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegranteControllerTest {

    @Mock
    private IntegranteDAO integranteDAO;

    private IntegranteController controller;

    @BeforeEach
    void setUp() {
        controller = new IntegranteController(integranteDAO);
    }

    @Test
    @DisplayName("Debe listar integrantes exitosamente")
    void testListarIntegrantes() {
        Integrante i = new Integrante("Laura", "Gomez", "555111", "FP-2024-01", "0981", "laura@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO, "UID1", CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash");
        i.setId(1L);
        when(integranteDAO.findAll()).thenReturn(List.of(i));

        Response response = controller.listarIntegrantes();

        assertEquals(200, response.getStatus());
        List<?> list = (List<?>) response.getEntity();
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Debe crear un nuevo integrante exitosamente con contrasena hasheada")
    void testCrearIntegrante_Exitoso() {
        when(integranteDAO.findByCi("666777")).thenReturn(Optional.empty());
        when(integranteDAO.findByEmail("nuevo@fpune.edu.py")).thenReturn(Optional.empty());

        IntegranteRequestDTO dto = new IntegranteRequestDTO();
        dto.setNombre("Pedro");
        dto.setApellido("Alonso");
        dto.setCi("666777");
        dto.setCarnetUniversitario("FP-2025-10");
        dto.setEmail("nuevo@fpune.edu.py");
        dto.setCarrera("INGENIERIA_DE_SISTEMAS");
        dto.setRol("MIEMBRO");
        dto.setPassword("miClaveSegura123");

        Integrante guardado = new Integrante("Pedro", "Alonso", "666777", "FP-2025-10", null, "nuevo@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO, null, CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hashed");
        guardado.setId(50L);

        when(integranteDAO.save(any(Integrante.class))).thenReturn(guardado);

        Response response = controller.crearIntegrante(dto);

        assertEquals(201, response.getStatus());
        IntegranteResponseDTO res = (IntegranteResponseDTO) response.getEntity();
        assertEquals(50L, res.getId());
        assertEquals("Pedro Alonso", res.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe rechazar la creacion si la cedula (CI) ya esta registrada")
    void testCrearIntegrante_CiDuplicado() {
        Integrante existente = new Integrante();
        when(integranteDAO.findByCi("123456")).thenReturn(Optional.of(existente));

        IntegranteRequestDTO dto = new IntegranteRequestDTO();
        dto.setNombre("Ana");
        dto.setApellido("Vera");
        dto.setCi("123456");
        dto.setCarnetUniversitario("FP-2024-99");
        dto.setEmail("ana@fpune.edu.py");

        assertThrows(IllegalArgumentException.class, () -> controller.crearIntegrante(dto));
    }

    @Test
    @DisplayName("Debe actualizar los datos de un integrante existente")
    void testActualizarIntegrante() {
        Integrante existente = new Integrante("Ana", "Vera", "123456", "FP-2024-99", "0981", "ana@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO, null, CarreraEnum.ANALISIS_DE_SISTEMAS, RolEnum.MIEMBRO, "hash");
        existente.setId(20L);

        when(integranteDAO.findById(20L)).thenReturn(Optional.of(existente));
        when(integranteDAO.update(any(Integrante.class))).thenReturn(existente);

        IntegranteRequestDTO dto = new IntegranteRequestDTO();
        dto.setNombre("Ana Maria");
        dto.setRol("DIRECTIVA");

        Response response = controller.actualizarIntegrante(20L, dto);

        assertEquals(200, response.getStatus());
        verify(integranteDAO).update(existente);
        assertEquals("Ana Maria", existente.getNombre());
        assertEquals(RolEnum.DIRECTIVA, existente.getRol());
    }

    @Test
    @DisplayName("Debe obtener el perfil del usuario autenticado correctamente")
    void testObtenerMiPerfil_Exitoso() {
        py.edu.une.politecnica.robogest.security.JwtTokenProvider tokenProvider = new py.edu.une.politecnica.robogest.security.JwtTokenProvider();
        controller = new IntegranteController(integranteDAO, tokenProvider);

        Integrante i = new Integrante("Carlos", "Benitez", "111222", "FP-2024-50", "0971", "carlos@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO, null, CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash");
        i.setId(77L);

        when(integranteDAO.findById(77L)).thenReturn(Optional.of(i));

        String token = tokenProvider.generateToken(i);
        Response response = controller.obtenerMiPerfil("Bearer " + token);

        assertEquals(200, response.getStatus());
        IntegranteResponseDTO dto = (IntegranteResponseDTO) response.getEntity();
        assertEquals(77L, dto.getId());
        assertEquals("Carlos Benitez", dto.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe actualizar el perfil del usuario autenticado")
    void testActualizarMiPerfil_Exitoso() {
        py.edu.une.politecnica.robogest.security.JwtTokenProvider tokenProvider = new py.edu.une.politecnica.robogest.security.JwtTokenProvider();
        controller = new IntegranteController(integranteDAO, tokenProvider);

        Integrante i = new Integrante("Carlos", "Benitez", "111222", "FP-2024-50", "0971", "carlos@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO, null, CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash");
        i.setId(77L);

        when(integranteDAO.findById(77L)).thenReturn(Optional.of(i));
        when(integranteDAO.update(any(Integrante.class))).thenReturn(i);

        String token = tokenProvider.generateToken(i);

        IntegranteRequestDTO dto = new IntegranteRequestDTO();
        dto.setNombre("Carlos Alberto");
        dto.setTelefono("0981999888");
        dto.setPassword("nuevaClaveSegura");

        Response response = controller.actualizarMiPerfil("Bearer " + token, dto);

        assertEquals(200, response.getStatus());
        verify(integranteDAO).update(i);
        assertEquals("Carlos Alberto", i.getNombre());
        assertEquals("0981999888", i.getTelefono());
    }

    @Test
    @DisplayName("Debe rechazar la consulta de perfil sin token de autorizacion")
    void testObtenerMiPerfil_SinToken() {
        assertThrows(jakarta.ws.rs.WebApplicationException.class, () -> controller.obtenerMiPerfil(null));
        assertThrows(jakarta.ws.rs.WebApplicationException.class, () -> controller.obtenerMiPerfil("TokenInvalido"));
    }
}
