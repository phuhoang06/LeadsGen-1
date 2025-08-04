package com.mm.image_aws.security;

import com.mm.image_aws.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final Environment env;
    private String jwtSecret;
    private int jwtExpirationInMs;
    private SecretKey key;

    // === SỬA LỖI: Inject toàn bộ Environment để đảm bảo đọc được properties ===
    public JwtTokenProvider(Environment env) {
        this.env = env;
    }

    /**
     * Sử dụng @PostConstruct để đọc properties và tạo key.
     * Phương thức này chạy sau khi bean được khởi tạo và environment đã sẵn sàng.
     */
    @PostConstruct
    public void init() {
        this.jwtSecret = Objects.requireNonNull(env.getProperty("jwt.secret"), "jwt.secret must be set in application.properties");
        this.jwtExpirationInMs = Integer.parseInt(Objects.requireNonNull(env.getProperty("jwt.expiration-ms"), "jwt.expiration-ms must be set in application.properties"));
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private SecretKey getSigningKey() {
        return key;
    }

    public String generateToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            // SỬA LỖI: Thêm log chi tiết
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
