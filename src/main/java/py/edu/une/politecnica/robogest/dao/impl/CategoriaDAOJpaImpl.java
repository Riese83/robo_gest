package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.CategoriaDAO;
import py.edu.une.politecnica.robogest.entity.Categoria;

import java.util.Optional;

/**
 * Implementacion JPA de CategoriaDAO con consultas JPQL parametrizadas.
 */
public class CategoriaDAOJpaImpl extends GenericDAOJpaImpl<Categoria, Long> implements CategoriaDAO {

    public CategoriaDAOJpaImpl() {
        super(Categoria.class);
    }

    public CategoriaDAOJpaImpl(EntityManager em) {
        super(Categoria.class, em);
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        String jpql = "SELECT c FROM Categoria c WHERE c.nombre = :nombre";
        TypedQuery<Categoria> query = em.createQuery(jpql, Categoria.class);
        query.setParameter("nombre", nombre);
        return query.getResultStream().findFirst();
    }
}
