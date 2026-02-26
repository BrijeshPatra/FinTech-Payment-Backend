package com.paypal.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;

public class JwtUtil {
    private static final String SECRET="secret123secret123secret123";

    //this method will encrypt the SECRET token by hashing
    private static Key getSigningKey(){
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
    public static Claims validateToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
