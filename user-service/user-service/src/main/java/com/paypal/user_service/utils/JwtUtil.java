package com.paypal.user_service.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final String SECRET="secret123secret123secret123";

  public Key getSigningKey(){
      return Keys.hmacShaKeyFor(SECRET.getBytes());
  }

  //extract email
    public String extractEmail(String token){
      return Jwts.parserBuilder()
              .setSigningKey(getSigningKey())
              .build()
              .parseClaimsJws(token)
              .getBody()
              .getSubject();
    }

    public boolean validateToken(String token,String username){
      try {
        extractEmail(token);
        return true;
      }catch (Exception e){
          return false;
      }
    }

    public String extractUserName(String token){
      return Jwts.parserBuilder()
              .setSigningKey(getSigningKey())
              .build()
              .parseClaimsJws(token)
              .getBody()
              .getSubject();
    }

    public String generateToken(Map<String,Object> claims,String email){
       return Jwts.builder()
               .setClaims(claims)
               .setSubject(email)
               .setIssuedAt(new Date(System.currentTimeMillis()))
               .setExpiration(new Date(System.currentTimeMillis()+86400000))
               .signWith(getSigningKey(), SignatureAlgorithm.HS256)
               .compact();
    }
  public String extractRole(String token){
    return (String)Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("role");
  }
}
