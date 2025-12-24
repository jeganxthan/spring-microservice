package com.example.demo.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Robust JwtAuthenticationFilter:
 *  - uses shouldNotFilter to skip public endpoints (handles contextPath and servletPath)
 *  - prints minimal debug lines to the console for verification
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Normalize path pieces so matching is robust
        String context = safe(request.getContextPath());   // often "", but may be "/app"
        String uri = safe(request.getRequestURI());        // full URI including context path
        String servlet = safe(request.getServletPath());   // path within app

        // canonical path to match against (servletPath is easiest)
        String pathToCheck = servlet;
        if (pathToCheck == null || pathToCheck.isEmpty()) {
            pathToCheck = uri;
            if (context != null && !context.isEmpty() && pathToCheck.startsWith(context)) {
                pathToCheck = pathToCheck.substring(context.length());
            }
        }

        // Normalize multiple slashes
        pathToCheck = pathToCheck.replaceAll("/{2,}", "/");

        // Public endpoints that do NOT require JWT
        if (pathToCheck.startsWith("/api/shows/") || pathToCheck.equals("/api/shows")) return true;
        if (pathToCheck.startsWith("/api/auth/") || pathToCheck.equals("/api/auth")) return true;
        if (pathToCheck.startsWith("/static/") || pathToCheck.startsWith("/actuator") || pathToCheck.startsWith("/favicon")) return true;

        return false;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String reqPath = request.getRequestURI();
        // debug line - shows whether filter is active for this request
        System.out.println("[JWT-FILTER] requestURI=" + reqPath + " | shouldNotFilter=" + shouldNotFilter(request));

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // no token - just continue (if endpoint allowed, request will succeed; otherwise Security will handle)
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            System.err.println("[JWT-FILTER] token parse failed: " + ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("[JWT-FILTER] authenticated user=" + userEmail);
                } else {
                    System.out.println("[JWT-FILTER] token invalid for user=" + userEmail);
                }
            } catch (Exception e) {
                System.err.println("[JWT-FILTER] auth error for " + userEmail + ": " + e.getMessage());
                // continue, don't throw — Security will deny where needed
            }
        }

        filterChain.doFilter(request, response);
    }
}
