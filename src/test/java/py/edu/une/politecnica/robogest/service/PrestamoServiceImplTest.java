package py.edu.une.politecnica.robogest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dao.PrestamoDAO;
import py.edu.une.politecnica.robogest.entity.Categoria;
import py.edu.une.politecnica.robogest.entity.DetallePrestamo;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.exception.EstadoPrestamoInvalidoException;
import py.edu.une.politecnica.robogest.exception.PrestamoNoEncontradoException;
import py.edu.une.politecnica.robogest.exception.StockInsuficienteException;
import py.edu.une.politecnica.robogest.service.impl.PrestamoServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceImplTest {

    @Mock
    private PrestamoDAO prestamoDAO;

    @Mock
    private MaterialDAO materialDAO;

    @Mock
    private DetallePrestamoDAO detallePrestamoDAO;

    private PrestamoServiceImpl prestamoService;

    @BeforeEach
    void setUp() {
        prestamoService = new PrestamoServiceImpl(prestamoDAO, materialDAO, detallePrestamoDAO);
    }

    private Prestamo crearPrestamoConDetalle(Long prestamoId, Long materialId, int cantidadSolicitada, EstadoPrestamoEnum estado) {
        Integrante integrante = new Integrante(
                "Maria", "Lopez", "4567890", "FP-2023-11", "0981999888",
                "maria.lopez@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "EEDDCCBB", CarreraEnum.INGENIERIA_ELECTRICA, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(1L);

        Prestamo prestamo = new Prestamo(
                integrante, null, LocalDateTime.now(), null,
                LocalDateTime.now().plusDays(5), null, estado, "Para prototipo de robot"
        );
        prestamo.setId(prestamoId);

        Material material = new Material(
                new Categoria("Sensores", "Sensores opticos e infrarrojos"),
                "Sensor Ultrasonico HC-SR04", "Modulo medicion distancia",
                "SparkFun", "HC-SR04", 10, EstadoMaterialEnum.ACTIVO, "Caja B-2"
        );
        material.setId(materialId);

        DetallePrestamo detalle = new DetallePrestamo(prestamo, material, cantidadSolicitada);
        List<DetallePrestamo> detalles = new ArrayList<>();
        detalles.add(detalle);
        prestamo.setDetalles(detalles);

        return prestamo;
    }

    @Test
    @DisplayName("Debe aprobar prestamo exitosamente aplicando bloqueo pesimista cuando hay stock suficiente")
    void testAprobarPrestamo_Exitoso() {
        Long prestamoId = 100L;
        Long materialId = 50L;
        int cantidadSolicitada = 3;

        Prestamo prestamo = crearPrestamoConDetalle(prestamoId, materialId, cantidadSolicitada, EstadoPrestamoEnum.SOLICITADO);
        Material materialBloqueado = prestamo.getDetalles().get(0).getMaterial();

        when(prestamoDAO.findById(prestamoId)).thenReturn(Optional.of(prestamo));
        when(materialDAO.findByIdWithLock(materialId)).thenReturn(Optional.of(materialBloqueado));
        when(detallePrestamoDAO.getCantidadReservadaPorMaterial(materialId)).thenReturn(2); // total 10, reservado 2, disponible 8
        when(prestamoDAO.update(any(Prestamo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prestamo resultado = prestamoService.aprobarPrestamo(prestamoId);

        assertNotNull(resultado);
        assertEquals(EstadoPrestamoEnum.APROBADO, resultado.getEstado());

        // Verificaciones criticas: se aplico findByIdWithLock y update
        verify(materialDAO, times(1)).findByIdWithLock(materialId);
        verify(prestamoDAO, times(1)).update(prestamo);
    }

    @Test
    @DisplayName("Debe lanzar StockInsuficienteException cuando la cantidad solicitada supera el stock disponible")
    void testAprobarPrestamo_StockInsuficiente() {
        Long prestamoId = 101L;
        Long materialId = 51L;
        int cantidadSolicitada = 5;

        Prestamo prestamo = crearPrestamoConDetalle(prestamoId, materialId, cantidadSolicitada, EstadoPrestamoEnum.SOLICITADO);
        Material materialBloqueado = prestamo.getDetalles().get(0).getMaterial(); // cantidadTotal = 10

        when(prestamoDAO.findById(prestamoId)).thenReturn(Optional.of(prestamo));
        when(materialDAO.findByIdWithLock(materialId)).thenReturn(Optional.of(materialBloqueado));
        // Reservas actuales = 8, por lo que quedan solo 2 disponibles (10 - 8 = 2). Solicitado: 5
        when(detallePrestamoDAO.getCantidadReservadaPorMaterial(materialId)).thenReturn(8);

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () ->
                prestamoService.aprobarPrestamo(prestamoId));

        assertEquals(materialId, ex.getMaterialId());
        assertEquals(5, ex.getCantidadSolicitada());
        assertEquals(2, ex.getCantidadDisponible());

        // Debe haberse intentado el bloqueo pero NO guardado la aprobacion
        verify(materialDAO, times(1)).findByIdWithLock(materialId);
        verify(prestamoDAO, never()).update(any());
    }

    @Test
    @DisplayName("Debe lanzar EstadoPrestamoInvalidoException si el prestamo no esta en estado SOLICITADO")
    void testAprobarPrestamo_EstadoInvalido() {
        Long prestamoId = 102L;
        Prestamo prestamo = crearPrestamoConDetalle(prestamoId, 52L, 1, EstadoPrestamoEnum.ENTREGADO);

        when(prestamoDAO.findById(prestamoId)).thenReturn(Optional.of(prestamo));

        assertThrows(EstadoPrestamoInvalidoException.class, () ->
                prestamoService.aprobarPrestamo(prestamoId));

        verify(materialDAO, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Debe lanzar PrestamoNoEncontradoException cuando el prestamo no existe")
    void testAprobarPrestamo_NoEncontrado() {
        Long prestamoId = 999L;
        when(prestamoDAO.findById(prestamoId)).thenReturn(Optional.empty());

        assertThrows(PrestamoNoEncontradoException.class, () ->
                prestamoService.aprobarPrestamo(prestamoId));
    }
}
