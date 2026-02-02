package Adesk_OperationService.Filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthMiddleware extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Игнорируем эндпоинты Swagger
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.equals("/swagger-ui.html") ||
                path.equals("/v3/api-docs/swagger-config") ||
                path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty()){
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Request must come from gateway idi nahuy\"}");
            return;
        }

        chain.doFilter(request, response);
    }

}