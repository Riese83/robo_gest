package py.edu.une.politecnica.robogest.dao;

import py.edu.une.politecnica.robogest.entity.Asistencia;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz DAO para operaciones de persistencia de Asistencia.
 */
public interface AsistenciaDAO extends GenericDAO<Asistencia, Long> {

    List<Asistencia> findByIntegranteId(Long integranteId);

    List<Asistencia> findBetweenDates(LocalDateTime start, LocalDateTime end);
}
