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
}
