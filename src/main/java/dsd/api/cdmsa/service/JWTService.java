package dsd.api.cdmsa.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.model.User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    // Secret key stored in application.properties (Base64 encoded)
    @Value("${jwt.secret}")
    private String secretKey;

    // Token expiration time in milliseconds
    @Value("${jwt.expiration:900000}") // default 15 min
    private long jwtExpiration;

    // Generate a JWT with username as the subject
    public String generateToken(User user) {
        
        Map<String, Object> claims = Map.of(
            "userId", user.getId(),
            "email", user.getEmail(),
            "orgId", user.getOrg().getId()
        );

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                // Expiration is configurable from properties
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .and()
                .signWith(getSigningKey()) // Sign with secret HMAC key
                .compact();
    }

    // Convert the Base64-encoded secret into a SecretKey object
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extract username (subject) from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract any claim using a resolver function
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parse and verify token signature, return claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validate the token integrity, user match and expiration date
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Check if expiration date is in the past
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extract expiration time
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}