package py.edu.une.politecnica.robogest.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.io.IOException;

/**
 * Filtro de Servlet para garantizar el patron Session-Per-Request.
 * Libera de forma determinista el EntityManager y las conexiones de base de datos
 * al finalizar cada peticion HTTP, evitando bloqueos y fugas en el pool de conexiones.
 */
@WebFilter(filterName = "JpaCleanupFilter", urlPatterns = {"/api/*"})
public class JpaCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            JpaUtil.closeEntityManager();
        }
    }
}
