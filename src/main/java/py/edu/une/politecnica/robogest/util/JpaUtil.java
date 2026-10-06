package py.edu.une.politecnica.robogest.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Utilidad para la gestion del EntityManagerFactory y EntityManager en la capa JPA.
 */
public final class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "RoboGestPU";
    private static volatile EntityManagerFactory emf;

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
        return getEntityManagerFactory().createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
