package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.AsistenciaDAO;
import py.edu.une.politecnica.robogest.entity.Asistencia;

import java.time.LocalDateTime;
import java.util.List;

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
        TypedQuery<Asistencia> query = em.createQuery(jpql, Asistencia.class);
        query.setParameter("integranteId", integranteId);
        return query.getResultList();
    }

    @Override
    public List<Asistencia> findBetweenDates(LocalDateTime start, LocalDateTime end) {
        String jpql = "SELECT a FROM Asistencia a WHERE a.fechaHora BETWEEN :start AND :end ORDER BY a.fechaHora ASC";
        TypedQuery<Asistencia> query = em.createQuery(jpql, Asistencia.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }
}
