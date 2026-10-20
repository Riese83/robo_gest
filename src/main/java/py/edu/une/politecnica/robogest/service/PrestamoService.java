package py.edu.une.politecnica.robogest.service;

import py.edu.une.politecnica.robogest.entity.Prestamo;

import java.time.LocalDateTime;

/**
 * Servicio de gestion del ciclo de vida y logica transaccional de prestamos de materiales.
 */
public interface PrestamoService {

    /**
     * Aprueba un prestamo en estado SOLICITADO.
     * Operacion transaccional (ACID) que bloquea pesimistamente los materiales
     * y valida la disponibilidad real de stock antes de confirmar.
     *
     * @param prestamoId Identificador unico del prestamo a aprobar
     * @return El prestamo con su estado actualizado a APROBADO
     */
    Prestamo aprobarPrestamo(Long prestamoId);

    /**
     * Aprueba un prestamo estableciendo la fecha de devolucion prevista asignada.
     *
     * @param prestamoId Identificador unico del prestamo
     * @param fechaDevolucionPrevista Fecha limite estimada para la devolucion
     * @return El prestamo actualizado
     */
    Prestamo aprobarPrestamo(Long prestamoId, LocalDateTime fechaDevolucionPrevista);

    /**
     * Registra la entrega fisica de materiales al solicitante (APROBADO -> ENTREGADO).
     *
     * @param prestamoId Identificador unico del prestamo
     * @return El prestamo actualizado
     */
    Prestamo entregarPrestamo(Long prestamoId);

    /**
     * Registra la devolucion formal de los materiales al inventario (ENTREGADO/VENCIDO -> DEVUELTO).
     *
     * @param prestamoId Identificador unico del prestamo
     * @return El prestamo actualizado
     */
    Prestamo devolverPrestamo(Long prestamoId);

    /**
     * Cancela un prestamo en estado SOLICITADO o APROBADO (libera reservas de stock).
     *
     * @param prestamoId Identificador unico del prestamo
     * @return El prestamo actualizado
     */
    Prestamo cancelarPrestamo(Long prestamoId);

    /**
     * Rechaza una solicitud de prestamo en estado SOLICITADO con un motivo explicativo.
     *
     * @param prestamoId Identificador unico del prestamo
     * @param motivo Justificacion del rechazo
     * @return El prestamo actualizado
     */
    Prestamo rechazarPrestamo(Long prestamoId, String motivo);
}
