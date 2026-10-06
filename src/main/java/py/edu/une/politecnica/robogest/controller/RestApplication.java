package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configuracion y punto de entrada para los servicios RESTful (JAX-RS / Jersey).
 */
@ApplicationPath("/")
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        packages("py.edu.une.politecnica.robogest.controller");
        register(ObjectMapperContextResolver.class);
        register(GlobalExceptionMapper.class);
        register(AuthController.class);
        register(AsistenciaController.class);
        register(PrestamoController.class);
    }
}
