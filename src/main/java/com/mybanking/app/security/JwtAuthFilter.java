package com.mybanking.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthFilter(JwtTokenProvider jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        String token = (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7) : null;

        if (token != null && jwt.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String subject = jwt.getSubject(token);
            var roles = jwt.getRoles(token);

            var authorities = roles.stream()
                    .map(r -> "ROLE_" + r)
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .toList();

            var auth = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    authorities
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }
}
