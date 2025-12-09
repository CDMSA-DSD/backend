package dsd.api.cdmsa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dsd.api.cdmsa.service.JWTService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Read the Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        try {
            // Extract token if header uses the expected "Bearer <token>" format
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                username = jwtService.extractUsername(token);
            }

            // Proceed only if username exists and no user is authenticated yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // Validate token signature and expiration
                if (jwtService.validateToken(token, userDetails)) {
                    // Create authentication object stored in SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());
                    // Attach request details (IP, session info, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource()
                            .buildDetails(request));
                    // Mark this user as authenticated for the current request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // Continue with the next filter in the chain
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // Expired toke -> 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate",
                    "Bearer error=\"invalid_token\", error_description=\"Token expired\"");
            response.setContentType("application/json");
            response.getWriter().write("""
                        {"error":"invalid_token","error_description":"Token expired"}
                    """);

        } catch (JwtException e) {
            // Invalid token -> 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate",
                    "Bearer error=\"invalid_token\", error_description=\"Malformed token\"");
            response.setContentType("application/json");
            response.getWriter().write("""
                        {"error":"invalid_token","error_description":"Malformed token"}
                    """);        
        }
    }
}