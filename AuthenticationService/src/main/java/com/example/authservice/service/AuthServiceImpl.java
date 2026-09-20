package com.example.authservice.service;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.LoginResponseDto;
import com.example.authservice.dto.UserSessionDto;
import com.example.authservice.entity.JwtToken;
import com.example.authservice.entity.User;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UnauthorizedException;
import com.example.authservice.repository.JwtTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UserRepository userRepository,
            JwtTokenRepository jwtTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequestDto dto) {
        // 1. Fetch user from user table
        User user = userRepository.findByName(dto.getName().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // 2. Verify hashed password with BCrypt
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // 3. Generate signed JWT containing subject, issued-at, expiration
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        String token = jwtUtil.generateToken(user.getName(), claims);

        // 4. Save record into MySQL jwt_token table (uid references user.id)
        LocalDateTime expiryTime = LocalDateTime.now().plusNanos(jwtUtil.getExpirationMs() * 1_000_000);
        JwtToken jwtTokenEntity = new JwtToken(user.getId(), token, expiryTime);
        jwtTokenRepository.save(jwtTokenEntity);

        // 5. Build sanitized response DTO (no token or password in body, token will go to HttpOnly cookie)
        LoginResponseDto responseDto = new LoginResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                "Login successful"
        );

        return new AuthResult(token, responseDto);
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtTokenRepository.deleteByToken(token);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSessionDto getSessionUser(String token) {
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Session invalid or expired");
        }

        // Verify token is active in MySQL jwt_token database table
        JwtToken storedToken = jwtTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Session token has been revoked or expired"));

        if (storedToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Session has expired");
        }

        // Retrieve user
        User user = userRepository.findById(storedToken.getUid())
                .orElseThrow(() -> new UnauthorizedException("User associated with session not found"));

        return new UserSessionDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                true
        );
    }
}
