package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.PrestamoDAO;
import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementacion JPA de PrestamoDAO con consultas JPQL tipadas y parametrizadas.
 */
public class PrestamoDAOJpaImpl extends GenericDAOJpaImpl<Prestamo, Long> implements PrestamoDAO {

    public PrestamoDAOJpaImpl() {
        super(Prestamo.class);
    }

    public PrestamoDAOJpaImpl(EntityManager em) {
        super(Prestamo.class, em);
    }

    @Override
    public List<Prestamo> findByIntegranteId(Long integranteId) {
        String jpql = "SELECT p FROM Prestamo p WHERE p.integrante.id = :integranteId ORDER BY p.fechaSolicitud DESC";
        TypedQuery<Prestamo> query = getEntityManager().createQuery(jpql, Prestamo.class);
        query.setParameter("integranteId", integranteId);
        return query.getResultList();
    }

    @Override
    public List<Prestamo> findByProyectoId(Long proyectoId) {
        String jpql = "SELECT p FROM Prestamo p WHERE p.proyecto.id = :proyectoId ORDER BY p.fechaSolicitud DESC";
        TypedQuery<Prestamo> query = getEntityManager().createQuery(jpql, Prestamo.class);
        query.setParameter("proyectoId", proyectoId);
        return query.getResultList();
    }

    @Override
    public List<Prestamo> findByEstado(EstadoPrestamoEnum estado) {
        String jpql = "SELECT p FROM Prestamo p WHERE p.estado = :estado ORDER BY p.fechaSolicitud DESC";
        TypedQuery<Prestamo> query = getEntityManager().createQuery(jpql, Prestamo.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<Prestamo> findVencidos() {
        String jpql = "SELECT p FROM Prestamo p " +
                      "WHERE p.estado = :estado " +
                      "  AND p.fechaDevolucionReal IS NULL " +
                      "  AND p.fechaDevolucionPrevista < :now " +
                      "ORDER BY p.fechaDevolucionPrevista ASC";
        TypedQuery<Prestamo> query = getEntityManager().createQuery(jpql, Prestamo.class);
        query.setParameter("estado", EstadoPrestamoEnum.ENTREGADO);
        query.setParameter("now", LocalDateTime.now());
        return query.getResultList();
    }
}
