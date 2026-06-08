package com.project.elms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expiration-ms:86400000}") private long expiry;

    private Key key(){ return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generate(String email, String role){
        return Jwts.builder().setSubject(email).claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+expiry))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public String email(String token){
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean valid(String token){
        try { Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); return true; }
        catch (Exception e){ return false; }
    }
}