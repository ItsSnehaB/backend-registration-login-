package com.example.authservice.dto;

public class UserSessionDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private boolean authenticated;

    public UserSessionDto() {
    }

    public UserSessionDto(Long id, String name, String email, String phone, boolean authenticated) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.authenticated = authenticated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
