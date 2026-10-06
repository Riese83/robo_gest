package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.IntegranteProyectoDAO;
import py.edu.une.politecnica.robogest.entity.IntegranteProyecto;
import py.edu.une.politecnica.robogest.entity.IntegranteProyectoId;

import java.util.List;

/**
 * Implementacion JPA de IntegranteProyectoDAO con consultas JPQL tipadas.
 */
public class IntegranteProyectoDAOJpaImpl extends GenericDAOJpaImpl<IntegranteProyecto, IntegranteProyectoId> implements IntegranteProyectoDAO {

    public IntegranteProyectoDAOJpaImpl() {
        super(IntegranteProyecto.class);
    }

    public IntegranteProyectoDAOJpaImpl(EntityManager em) {
        super(IntegranteProyecto.class, em);
    }

    @Override
    public List<IntegranteProyecto> findByProyectoId(Long proyectoId) {
        String jpql = "SELECT ip FROM IntegranteProyecto ip WHERE ip.proyecto.id = :proyectoId ORDER BY ip.fechaIncorporacion ASC";
        TypedQuery<IntegranteProyecto> query = em.createQuery(jpql, IntegranteProyecto.class);
        query.setParameter("proyectoId", proyectoId);
        return query.getResultList();
    }

    @Override
    public List<IntegranteProyecto> findByIntegranteId(Long integranteId) {
        String jpql = "SELECT ip FROM IntegranteProyecto ip WHERE ip.integrante.id = :integranteId ORDER BY ip.fechaIncorporacion DESC";
        TypedQuery<IntegranteProyecto> query = em.createQuery(jpql, IntegranteProyecto.class);
        query.setParameter("integranteId", integranteId);
        return query.getResultList();
    }
}
