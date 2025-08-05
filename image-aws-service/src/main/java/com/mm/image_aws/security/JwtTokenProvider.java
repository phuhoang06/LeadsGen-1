package com.mm.image_aws.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader; // THÊM MỚI: Import để sử dụng hằng số KEY_ID
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.PublicKey;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${auth.jwks-url}")
    private String jwksUrl;

    public String getUsernameFromJWT(String token) {
        try {
            Jwt<Header, Claims> untrustedJwt = Jwts.parserBuilder().build().parseClaimsJwt(token.substring(0, token.lastIndexOf('.') + 1));
            // SỬA LỖI: Sử dụng .get() để lấy 'kid' từ header
            String kid = (String) untrustedJwt.getHeader().get(JwsHeader.KEY_ID);

            JwkProvider provider = new UrlJwkProvider(new URL(jwksUrl));
            Jwk jwk = provider.get(kid);
            PublicKey publicKey = jwk.getPublicKey();

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Could not get username from JWT: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwt<Header, Claims> untrustedJwt = Jwts.parserBuilder().build().parseClaimsJwt(authToken.substring(0, authToken.lastIndexOf('.') + 1));
            // SỬA LỖI: Sử dụng .get() để lấy 'kid' từ header
            String kid = (String) untrustedJwt.getHeader().get(JwsHeader.KEY_ID);

            JwkProvider provider = new UrlJwkProvider(new URL(jwksUrl));
            Jwk jwk = provider.get(kid);
            PublicKey publicKey = jwk.getPublicKey();

            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }
}
