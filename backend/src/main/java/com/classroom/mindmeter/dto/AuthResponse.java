package com.classroom.mindmeter.dto;

public class AuthResponse {

    private String token;

    private Long id;
    private String userCode;

    private String name;
    private String email;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(
            String token,
            Long id,
            String userCode,
            String name,
            String email,
            String role) {

        this.token = token;
        this.id = id;
        this.userCode = userCode;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}