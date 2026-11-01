package com.akkidarkhq.visaflow.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            logger.info("AuthTokenFilter: Processing request for URI: {}", request.getRequestURI());
            
            String jwt = parseJwt(request);
            if (jwt != null) {
                logger.info("AuthTokenFilter: Found JWT token in request header.");
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("AuthTokenFilter: Token is valid. Extracted username: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("AuthTokenFilter: Successfully loaded UserDetails for username: {}", username);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("AuthTokenFilter: Security context successfully set with authenticated user.");
                } else {
                    logger.warn("AuthTokenFilter: JWT token validation failed!");
                }
            } else {
                logger.warn("AuthTokenFilter: No JWT token found in request headers for URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("AuthTokenFilter: Cannot set user authentication! Reason: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // "Bearer " is 7 chars long
        }

        logger.debug("AuthTokenFilter: Authorization header is missing or does not start with 'Bearer '");
        return null;
    }
}
