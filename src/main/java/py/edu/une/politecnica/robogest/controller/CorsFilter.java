package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.io.IOException;

/**
 * Filtro JAX-RS para habilitar CORS (Cross-Origin Resource Sharing)
 * y garantizar la liberacion del EntityManager en cada respuesta HTTP.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            Response.ResponseBuilder builder = Response.ok();
            String origin = requestContext.getHeaderString("Origin");
            if (origin != null && !origin.isBlank()) {
                builder.header("Access-Control-Allow-Origin", origin);
                builder.header("Access-Control-Allow-Credentials", "true");
            } else {
                builder.header("Access-Control-Allow-Origin", "*");
            }
            builder.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with");
            builder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            builder.header("Access-Control-Max-Age", "3600");
            requestContext.abortWith(builder.build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        try {
            String origin = requestContext.getHeaderString("Origin");
            if (origin != null && !origin.isBlank()) {
                responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
                responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            } else {
                responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
            }
            responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with");
            responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            responseContext.getHeaders().putSingle("Access-Control-Max-Age", "3600");
        } finally {
            // Liberar el EntityManager del hilo actual para devolver la conexion JDBC al pool
            JpaUtil.closeEntityManager();
        }
    }
}
