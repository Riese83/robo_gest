package py.edu.une.politecnica.robogest.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Utilidad para la gestion del EntityManagerFactory y EntityManager en la capa JPA.
 * Implementa ThreadLocal para garantizar aislamiento por hilo y soporte multi-peticion.
 */
public final class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "RoboGestPU";
    private static volatile EntityManagerFactory emf;
    private static final ThreadLocal<EntityManager> threadLocalEm = new ThreadLocal<>();

    private JpaUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            synchronized (JpaUtil.class) {
                if (emf == null) {
                    try {
                        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
                    } catch (Exception e) {
                        System.err.println("Error inicializando EntityManagerFactory: " + e.getMessage());
                        throw new ExceptionInInitializerError(e);
                    }
                }
            }
        }
        return emf;
    }

    public static EntityManager getEntityManager() {
        EntityManager em = threadLocalEm.get();
        if (em == null || !em.isOpen()) {
            em = getEntityManagerFactory().createEntityManager();
            threadLocalEm.set(em);
        }
        return em;
    }

    public static void closeEntityManager() {
        EntityManager em = threadLocalEm.get();
        if (em != null) {
            if (em.isOpen()) {
                em.close();
            }
            threadLocalEm.remove();
        }
    }

    public static void close() {
        closeEntityManager();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
