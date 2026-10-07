package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configuracion y punto de entrada para los servicios RESTful (JAX-RS / Jersey).
 */
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        packages("py.edu.une.politecnica.robogest.controller");
        register(ObjectMapperContextResolver.class);
        register(CorsFilter.class);
        register(GlobalExceptionMapper.class);
        register(RootController.class);
        register(AuthController.class);
        register(AsistenciaController.class);
        register(PrestamoController.class);
        register(MaterialController.class);
        register(IntegranteController.class);
    }
}
