package com.classroom.mindmeter.dto;

public class StudentResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String registerNumber;

    public StudentResponse() {
    }

    public StudentResponse(
            Long id,
            String name,
            String email,
            String role,
            String registerNumber) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.registerNumber = registerNumber;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }
}