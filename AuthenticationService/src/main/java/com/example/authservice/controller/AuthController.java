package com.example.authservice.controller;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.LoginResponseDto;
import com.example.authservice.dto.UserSessionDto;
import com.example.authservice.exception.UnauthorizedException;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final long expirationMs;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            AuthService authService,
            @Value("${jwt.cookie-name:jwt_token}") String cookieName,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${jwt.cookie-secure:false}") boolean cookieSecure,
            @Value("${jwt.cookie-samesite:Lax}") String cookieSameSite
    ) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.expirationMs = expirationMs;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    /**
     * POST /api/login
     * Flow: Frontend -> AuthenticationService -> Validate -> BCrypt verification -> JWT -> jwt_token -> HttpOnly cookie -> Home
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        AuthService.AuthResult result = authService.login(loginRequestDto);

        // Issue JWT in an HttpOnly cookie so JavaScript cannot access it
        ResponseCookie jwtCookie = ResponseCookie.from(cookieName, result.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(result.getResponseDto());
    }

    /**
     * POST /api/logout
     * Invalidates JWT in database and destroys HttpOnly cookie (Max-Age=0)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "${jwt.cookie-name:jwt_token}", required = false) String cookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = cookieToken;
        if ((token == null || token.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }

        // Overwrite cookie with Max-Age=0
        ResponseCookie clearCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Collections.singletonMap("message", "Logged out successfully"));
    }

    /**
     * GET /api/me
     * Protected endpoint verified via HttpOnly cookie or Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<UserSessionDto> getCurrentUser(
            @CookieValue(name = "${jwt.cookie-name:jwt_token}", required = false) String cookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = cookieToken;
        if ((token == null || token.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Unauthorized: No active authentication session provided");
        }

        UserSessionDto session = authService.getSessionUser(token);
        return ResponseEntity.ok(session);
    }
}
