package com.example.userservice.controller;

import com.example.userservice.dto.RegisterRequestDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/reg
     * Registration endpoint:
     * Frontend -> UserService -> Validation -> BCrypt -> MySQL
     */
    @PostMapping("/reg")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        UserResponseDto response = userService.registerUser(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
