package com.mybanking.app.security;

import com.mybanking.app.user.dto.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiresInSeconds;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expires-in-seconds}") long expiresInSeconds,
            @Value("${app.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiresInSeconds = expiresInSeconds;
        this.issuer = issuer;
    }

    public String createToken(String userId, Collection<Role> roles) {
        Instant now = Instant.now();
        var userRoles = roles.stream().map(Enum::name).toList();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .claim("roles", userRoles)
                .setExpiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getRoles(String token) {
        var claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        Object raw = claims.get("roles");
        if (raw instanceof java.util.List<?> list) {
            return (java.util.List<String>) list;
        }
        return java.util.List.of();
    }

    public boolean isValid(String token) {
        try {
            var parser = Jwts.parserBuilder().setSigningKey(key).build();
            var claims = parser.parseClaimsJws(token).getBody();

            if (!issuer.equals(claims.getIssuer())) return false;

            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public long getExpiryEpochSeconds() {
        return Instant.now().plusSeconds(expiresInSeconds).getEpochSecond();
    }
}
