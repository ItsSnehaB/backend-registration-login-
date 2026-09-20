package com.example.authservice.service;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.LoginResponseDto;
import com.example.authservice.dto.UserSessionDto;

public interface AuthService {

    AuthResult login(LoginRequestDto loginRequestDto);

    void logout(String token);

    UserSessionDto getSessionUser(String token);

    class AuthResult {
        private final String token;
        private final LoginResponseDto responseDto;

        public AuthResult(String token, LoginResponseDto responseDto) {
            this.token = token;
            this.responseDto = responseDto;
        }

        public String getToken() {
            return token;
        }

        public LoginResponseDto getResponseDto() {
            return responseDto;
        }
    }
}
