package com.mm.image_aws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; // === THÊM MỚI ===
import org.slf4j.LoggerFactory; // === THÊM MỚI ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.util.Map;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // === SỬA LỖI: Khai báo một logger SLF4J mới để sử dụng ===
    private static final Logger slf4jLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final RestTemplate restTemplate;
    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, RestTemplate restTemplate) {
        this.tokenProvider = tokenProvider;
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt)) {
                slf4jLogger.debug("Validating token with user-service at: {}", userServiceUrl);
                
                // Gọi user-service để xác thực token
                String validateUrl = userServiceUrl + "/api/auth/validate";
                try {
                    ResponseEntity<Map> resp = restTemplate.postForEntity(validateUrl, Map.of("token", jwt), Map.class);
                    Map body = resp.getBody();
                    
                    if (resp.getStatusCode().is2xxSuccessful() && body != null && Boolean.TRUE.equals(body.get("valid"))) {
                        String username = (String) body.get("username");
                        // Có thể tạo UserDetails đơn giản hoặc custom nếu cần
                        UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(username)
                            .password("")
                            .authorities("USER")
                            .build();
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        slf4jLogger.info("Token validated by user-service for user: '{}'", username);
                    } else {
                        String error = body != null ? (String) body.get("error") : "unknown error";
                        slf4jLogger.warn("Token validation failed by user-service: {}", error);
                    }
                } catch (HttpClientErrorException e) {
                    slf4jLogger.error("HTTP error when calling user-service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                } catch (ResourceAccessException e) {
                    slf4jLogger.error("Cannot connect to user-service at {}: {}", userServiceUrl, e.getMessage());
                } catch (Exception e) {
                    slf4jLogger.error("Unexpected error when calling user-service: {}", e.getMessage(), e);
                }
            }
        } catch (Exception ex) {
            slf4jLogger.error("Could not set user authentication in security context (user-service)", ex);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
