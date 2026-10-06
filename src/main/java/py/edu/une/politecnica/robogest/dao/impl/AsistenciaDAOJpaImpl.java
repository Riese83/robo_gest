package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.AsistenciaDAO;
import py.edu.une.politecnica.robogest.entity.Asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JPA de AsistenciaDAO con consultas JPQL tipadas y parametrizadas.
 */
public class AsistenciaDAOJpaImpl extends GenericDAOJpaImpl<Asistencia, Long> implements AsistenciaDAO {

    public AsistenciaDAOJpaImpl() {
        super(Asistencia.class);
    }

    public AsistenciaDAOJpaImpl(EntityManager em) {
        super(Asistencia.class, em);
    }

    @Override
    public List<Asistencia> findByIntegranteId(Long integranteId) {
        String jpql = "SELECT a FROM Asistencia a WHERE a.integrante.id = :integranteId ORDER BY a.fechaHora DESC";
        TypedQuery<Asistencia> query = getEntityManager().createQuery(jpql, Asistencia.class);
        query.setParameter("integranteId", integranteId);
        return query.getResultList();
    }

    @Override
    public List<Asistencia> findBetweenDates(LocalDateTime start, LocalDateTime end) {
        String jpql = "SELECT a FROM Asistencia a WHERE a.fechaHora BETWEEN :start AND :end ORDER BY a.fechaHora ASC";
        TypedQuery<Asistencia> query = getEntityManager().createQuery(jpql, Asistencia.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }

    @Override
    public Optional<Asistencia> findUltimaMarcacionDelDia(Long integranteId, LocalDate fecha) {
        LocalDateTime startOfDay = fecha.atStartOfDay();
        LocalDateTime endOfDay = fecha.atTime(LocalTime.MAX);

        String jpql = "SELECT a FROM Asistencia a " +
                      "WHERE a.integrante.id = :integranteId " +
                      "  AND a.fechaHora BETWEEN :startOfDay AND :endOfDay " +
                      "ORDER BY a.fechaHora DESC";
        TypedQuery<Asistencia> query = getEntityManager().createQuery(jpql, Asistencia.class);
        query.setParameter("integranteId", integranteId);
        query.setParameter("startOfDay", startOfDay);
        query.setParameter("endOfDay", endOfDay);
        query.setMaxResults(1);

        return query.getResultStream().findFirst();
    }
}
