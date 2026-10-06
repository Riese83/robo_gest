package py.edu.une.politecnica.robogest.service.impl;

import jakarta.transaction.Transactional;
import py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dao.PrestamoDAO;
import py.edu.une.politecnica.robogest.entity.DetallePrestamo;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;
import py.edu.une.politecnica.robogest.exception.EstadoPrestamoInvalidoException;
import py.edu.une.politecnica.robogest.exception.PrestamoNoEncontradoException;
import py.edu.une.politecnica.robogest.exception.StockInsuficienteException;
import py.edu.une.politecnica.robogest.service.PrestamoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Implementacion del servicio de prestamos con soporte transaccional y bloqueo pesimista de stock.
 */
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoDAO prestamoDAO;
    private final MaterialDAO materialDAO;
    private final DetallePrestamoDAO detallePrestamoDAO;

    public PrestamoServiceImpl(PrestamoDAO prestamoDAO, MaterialDAO materialDAO, DetallePrestamoDAO detallePrestamoDAO) {
        this.prestamoDAO = Objects.requireNonNull(prestamoDAO, "prestamoDAO no puede ser null");
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.detallePrestamoDAO = Objects.requireNonNull(detallePrestamoDAO, "detallePrestamoDAO no puede ser null");
    }

    @Override
    @Transactional
    public Prestamo aprobarPrestamo(Long prestamoId) {
        // Por defecto, asigna devolucion prevista a 7 dias si no se especifica
        return aprobarPrestamo(prestamoId, LocalDateTime.now().plusDays(7));
    }

    @Override
    @Transactional
    public Prestamo aprobarPrestamo(Long prestamoId, LocalDateTime fechaDevolucionPrevista) {
        if (prestamoId == null) {
            throw new IllegalArgumentException("El ID del prestamo no puede ser null");
        }

        // 1. Obtener el prestamo solicitado
        Prestamo prestamo = prestamoDAO.findById(prestamoId)
                .orElseThrow(() -> new PrestamoNoEncontradoException(prestamoId));

        // 2. Validar que el prestamo este en estado SOLICITADO
        if (prestamo.getEstado() != EstadoPrestamoEnum.SOLICITADO) {
            throw new EstadoPrestamoInvalidoException(String.format(
                    "Solo se puede aprobar un prestamo en estado SOLICITADO. Estado actual: %s",
                    prestamo.getEstado()
            ));
        }

        // 3. Validar fecha de devolucion prevista futura
        LocalDateTime fechaLimite = fechaDevolucionPrevista != null
                ? fechaDevolucionPrevista
                : prestamo.getFechaDevolucionPrevista();

        if (fechaLimite == null || fechaLimite.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de devolucion prevista debe ser posterior a la fecha actual");
        }

        // 4. Obtener items del prestamo
        List<DetallePrestamo> detalles = prestamo.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            detalles = detallePrestamoDAO.findByPrestamoId(prestamoId);
        }

        if (detalles.isEmpty()) {
            throw new IllegalStateException("El prestamo no contiene materiales asociados para aprobar");
        }

        // 5. Iterar sobre cada detalle aplicando bloqueo pesimista y validando stock disponible
        for (DetallePrestamo detalle : detalles) {
            Long materialId = detalle.getMaterial().getId();
            int cantidadSolicitada = detalle.getCantidad();

            // Bloqueo pesimista en base de datos (SELECT ... FOR UPDATE)
            Material materialBloqueado = materialDAO.findByIdWithLock(materialId)
                    .orElseThrow(() -> new IllegalStateException("Material no encontrado con ID: " + materialId));

            // Validar estado operativo del material
            if (materialBloqueado.getEstado() != EstadoMaterialEnum.ACTIVO) {
                throw new IllegalStateException(String.format(
                        "El material '%s' no se encuentra en estado ACTIVO (Estado actual: %s)",
                        materialBloqueado.getNombre(), materialBloqueado.getEstado()
                ));
            }

            // Calcular reservas activas (prestamos APROBADO, ENTREGADO o VENCIDO)
            int reservasActivas = detallePrestamoDAO.getCantidadReservadaPorMaterial(materialId);

            // Calcular stock disponible real
            int stockDisponible = materialBloqueado.getCantidadTotal() - reservasActivas;

            // Validar suficiencia de stock
            if (cantidadSolicitada > stockDisponible) {
                throw new StockInsuficienteException(
                        materialId,
                        materialBloqueado.getNombre(),
                        cantidadSolicitada,
                        stockDisponible
                );
            }
        }

        // 6. Transicionar el estado del prestamo a APROBADO
        prestamo.setEstado(EstadoPrestamoEnum.APROBADO);
        prestamo.setFechaDevolucionPrevista(fechaLimite);

        return prestamoDAO.update(prestamo);
    }
}
