package com.example.userservice.service;

import com.example.userservice.dto.RegisterRequestDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateResourceException;
import com.example.userservice.exception.PasswordMismatchException;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDto registerUser(RegisterRequestDto dto) {
        // 1. Password confirmation check
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and Confirm Password do not match");
        }

        // 2. Check duplicate username
        if (userRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Username '" + dto.getName() + "' is already registered");
        }

        // 3. Check duplicate email
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email '" + dto.getEmail() + "' is already registered");
        }

        // 4. Hash password using BCrypt - never store plain password
        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        // 5. Build and persist entity
        User user = new User();
        user.setName(dto.getName().trim());
        user.setPassword(hashedPassword);
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPhone(dto.getPhone().trim());

        User savedUser = userRepository.save(user);

        // 6. Return sanitized DTO (never return password)
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                "User registered successfully"
        );
    }
}
