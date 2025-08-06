package com.mm.user.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final RSAKey rsaKey;


    /**
     * Tạo JWT token từ thông tin xác thực của người dùng.
     * Token sẽ được ký bằng Private Key.
     */
    public String generateToken(Authentication authentication) {
        org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256) // Sử dụng RS256 với Private Key
                .compact();
    }

    /**
     * Lấy username từ JWT token.
     * Cần Public Key để xác minh chữ ký trước khi trích xuất thông tin.
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey) // Xác thực bằng Public Key
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Kiểm tra tính hợp lệ của token.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            // Các lỗi có thể xảy ra: SignatureException, MalformedJwtException, ExpiredJwtException, etc.
            logger.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Tạo và trả về JWKSet chứa Public Key.
     * Hàm .toPublicJWK() đảm bảo rằng Private Key không bị lộ ra ngoài.
     */
    public JWKSet getJwkSet() {
        return new JWKSet(this.rsaKey.toPublicJWK());
    }

    public JwtTokenProvider(
            @Value("classpath:private_key.pem") Resource privateKeyResource,
            @Value("classpath:public_key.pem") Resource publicKeyResource
    ) throws Exception {
        // Đọc Private Key từ InputStream
        try (InputStream privateKeyStream = privateKeyResource.getInputStream()) {
            byte[] privateKeyBytes = privateKeyStream.readAllBytes();
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateSpec);
        }

        // Đọc Public Key từ InputStream
        try (InputStream publicKeyStream = publicKeyResource.getInputStream()) {
            byte[] publicKeyBytes = publicKeyStream.readAllBytes();
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicSpec);
        }

        // Tạo RSAKey để sử dụng cho JWKS, gán một Key ID duy nhất
        this.rsaKey = new RSAKey.Builder(this.publicKey)
                .privateKey(this.privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}