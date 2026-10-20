package py.edu.une.politecnica.robogest.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

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
                        Map<String, Object> properties = createEnvironmentOverrides();
                        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
                    } catch (Exception e) {
                        System.err.println("Error inicializando EntityManagerFactory: " + e.getMessage());
                        throw new ExceptionInInitializerError(e);
                    }
                }
            }
        }
        return emf;
    }

    private static Map<String, Object> createEnvironmentOverrides() {
        Map<String, Object> props = new HashMap<>();

        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            String dbHost = System.getenv("DB_HOST");
            if (dbHost != null && !dbHost.isBlank()) {
                String dbPort = System.getenv("DB_PORT");
                if (dbPort == null || dbPort.isBlank()) {
                    dbPort = "3306";
                }
                String dbName = System.getenv("DB_NAME");
                if (dbName == null || dbName.isBlank()) {
                    dbName = "club_robotica";
                }
                dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&autoReconnect=true";
            }
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            props.put("jakarta.persistence.jdbc.url", dbUrl);
        }

        String dbUser = System.getenv("DB_USER");
        if (dbUser != null && !dbUser.isBlank()) {
            props.put("jakarta.persistence.jdbc.user", dbUser);
        }

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null && !dbPassword.isBlank()) {
            props.put("jakarta.persistence.jdbc.password", dbPassword);
        }

        return props;
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
            try {
                if (em.isOpen()) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    em.close();
                }
            } catch (Exception e) {
                System.err.println("Error cerrando EntityManager: " + e.getMessage());
            } finally {
                threadLocalEm.remove();
            }
        }
    }

    public static void close() {
        closeEntityManager();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
