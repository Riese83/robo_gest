package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones de persistencia de Asistencia.
 */
public interface AsistenciaDAO extends GenericDAO<Asistencia, Long> {

    List<Asistencia> findByIntegranteId(Long integranteId);

    List<Asistencia> findBetweenDates(LocalDateTime start, LocalDateTime end);

    /**
     * Obtiene la ultima marcacion de asistencia de un integrante en una fecha especifica.
     */
    Optional<Asistencia> findUltimaMarcacionDelDia(Long integranteId, LocalDate fecha);
}
