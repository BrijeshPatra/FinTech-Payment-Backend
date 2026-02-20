package com.paypal.user_service.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.security.Security;
import java.util.List;

public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);


    public JwtRequestFilter(JwtUtil jwtUtil){
        this.jwtUtil=jwtUtil;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader=request.getHeader("Authorization");

        String username=null;
        String jwtToken=null;

        if (authorizationHeader!=null && authorizationHeader.startsWith("Bearer")){
            jwtToken=authorizationHeader.substring(7);
            log.debug("Authorization header found , extracting JWT");

            try {
                username=jwtUtil.extractUserName(jwtToken);
                log.debug("Username extracted from JWT {} ", username);
            }catch (Exception e){
                //log: implement logger here
                log.warn("Failed to extract username",e);
            }
        }

        if (username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
            if (jwtUtil.validateToken(jwtToken,username)){
                log.debug("JWT validated successfully for user: {}", username);
                UsernamePasswordAuthenticationToken authToken=
                        new UsernamePasswordAuthenticationToken(username,null,null);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }else{
                log.warn("Invalid jwt token for user {} ",username);
            }
        }
        if (authorizationHeader!=null && authorizationHeader.startsWith("Bearer")){
            jwtToken=authorizationHeader.substring(7);
            if (jwtToken==null || jwtToken.isBlank()){
                log.debug("JWT token is blank continuing filter chain");
                filterChain.doFilter(request,response);
                return;
            }
            try {
                username=jwtUtil.extractUserName(jwtToken);

                String role=jwtUtil.extractRole(jwtToken);
                log.debug("User {} has role {} ", username,role);
                UsernamePasswordAuthenticationToken authenticationToken=
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                filterChain.doFilter(request,response);
            }catch (Exception e){
                //log
                log.error("JWT Authentication failed",e);
            }
        }else {
            filterChain.doFilter(request,response);
        }
    }
}
