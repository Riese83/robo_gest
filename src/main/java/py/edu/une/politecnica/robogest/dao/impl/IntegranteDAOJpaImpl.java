package py.edu.une.politecnica.robogest.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;

import java.util.List;
import java.util.Optional;

/**
 * Implementacion JPA de IntegranteDAO con consultas JPQL tipadas y parametrizadas.
 */
public class IntegranteDAOJpaImpl extends GenericDAOJpaImpl<Integrante, Long> implements IntegranteDAO {

    public IntegranteDAOJpaImpl() {
        super(Integrante.class);
    }

    public IntegranteDAOJpaImpl(EntityManager em) {
        super(Integrante.class, em);
    }

    @Override
    public Optional<Integrante> findByCi(String ci) {
        String jpql = "SELECT i FROM Integrante i WHERE i.ci = :ci";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("ci", ci);
        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<Integrante> findByEmail(String email) {
        String jpql = "SELECT i FROM Integrante i WHERE i.email = :email";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("email", email);
        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<Integrante> findByCarnetUniversitario(String carnetUniversitario) {
        String jpql = "SELECT i FROM Integrante i WHERE i.carnetUniversitario = :carnetUniversitario";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("carnetUniversitario", carnetUniversitario);
        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<Integrante> findByNfcUid(String nfcUid) {
        String jpql = "SELECT i FROM Integrante i WHERE i.nfcUid = :nfcUid";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("nfcUid", nfcUid);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<Integrante> findByCarrera(CarreraEnum carrera) {
        String jpql = "SELECT i FROM Integrante i WHERE i.carrera = :carrera";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("carrera", carrera);
        return query.getResultList();
    }

    @Override
    public List<Integrante> findByEstado(EstadoIntegranteEnum estado) {
        String jpql = "SELECT i FROM Integrante i WHERE i.estado = :estado";
        TypedQuery<Integrante> query = getEntityManager().createQuery(jpql, Integrante.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
}
