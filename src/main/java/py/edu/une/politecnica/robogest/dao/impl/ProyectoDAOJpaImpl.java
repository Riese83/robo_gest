package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.ProyectoDAO;
import py.edu.une.politecnica.robogest.entity.Proyecto;
import py.edu.une.politecnica.robogest.entity.enums.EstadoProyectoEnum;

import java.util.List;

/**
 * Implementacion JPA de ProyectoDAO con consultas JPQL tipadas y parametrizadas.
 */
public class ProyectoDAOJpaImpl extends GenericDAOJpaImpl<Proyecto, Long> implements ProyectoDAO {

    public ProyectoDAOJpaImpl() {
        super(Proyecto.class);
    }

    public ProyectoDAOJpaImpl(EntityManager em) {
        super(Proyecto.class, em);
    }

    @Override
    public List<Proyecto> findByEstado(EstadoProyectoEnum estado) {
        String jpql = "SELECT p FROM Proyecto p WHERE p.estado = :estado ORDER BY p.fechaInicio DESC";
        TypedQuery<Proyecto> query = em.createQuery(jpql, Proyecto.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<Proyecto> findByNombreContaining(String nombre) {
        String jpql = "SELECT p FROM Proyecto p WHERE LOWER(p.nombre) LIKE LOWER(:nombre) ORDER BY p.nombre ASC";
        TypedQuery<Proyecto> query = em.createQuery(jpql, Proyecto.class);
        query.setParameter("nombre", "%" + nombre + "%");
        return query.getResultList();
    }
}
