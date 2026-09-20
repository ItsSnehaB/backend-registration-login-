package com.example.userservice.service;

import com.example.userservice.dto.RegisterRequestDto;
import com.example.userservice.dto.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(RegisterRequestDto registerRequestDto);
}
