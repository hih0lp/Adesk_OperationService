package Adesk_OperationService.Filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Filter;

@Component
public class AuthMiddleware extends OncePerRequestFilter {


    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty()){
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Request must come from gateway\"}");
            return;
        }

        chain.doFilter(request, response);
    }

}
