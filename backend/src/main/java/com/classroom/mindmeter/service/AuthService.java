
package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.AuthResponse;
import com.classroom.mindmeter.dto.LoginRequest;
import com.classroom.mindmeter.dto.RegisterRequest;
import com.classroom.mindmeter.dto.TeacherRegisterRequest;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.UserRepository;
import com.classroom.mindmeter.security.JwtService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    // STUDENT REGISTRATION
    public AuthResponse register(RegisterRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (request.getRegisterNumber() == null
                || request.getRegisterNumber().isBlank()) {
            throw new RuntimeException("College register number is required");
        }

        if (request.getDateOfBirth() == null
                || request.getDateOfBirth().isBlank()) {
            throw new RuntimeException("Date of birth is required");
        }

        String registerNumber = request.getRegisterNumber().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        if (userRepository.existsByRegisterNumber(registerNumber)) {
            throw new RuntimeException(
                    "College register number is already registered");
        }

        try {
            LocalDate.parse(request.getDateOfBirth());
        } catch (DateTimeParseException ex) {
            throw new RuntimeException(
                    "Date of birth must use yyyy-MM-dd format");
        }

        User user = new User();
        user.setUserCode(generateUserCode("STUDENT"));
        user.setRegisterNumber(registerNumber);
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getDateOfBirth()));
        user.setRole("STUDENT");

        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser);
    }

    // TEACHER REGISTRATION — NO REGISTRATION CODE REQUIRED
    public AuthResponse registerTeacher(TeacherRegisterRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException(
                    "Password must contain at least 6 characters");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        User teacher = new User();
        teacher.setUserCode(generateUserCode("TEACHER"));
        teacher.setName(request.getName().trim());
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode(request.getPassword()));
        teacher.setRole("TEACHER");

        User savedTeacher = userRepository.save(teacher);

        return createAuthResponse(savedTeacher);
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {

        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            throw new RuntimeException("Username is required");
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new RuntimeException("Please select a role");
        }

        String selectedRole =
                request.getRole().trim().toUpperCase(Locale.ROOT);

        String username = request.getUsername().trim();

        User user;

        if ("STUDENT".equals(selectedRole)) {
            user = userRepository.findByRegisterNumber(username)
                    .orElseThrow(() -> new RuntimeException(
                            "Invalid register number or password"));

        } else if ("TEACHER".equals(selectedRole)) {
            user = userRepository.findByEmail(
                    username.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new RuntimeException(
                            "Invalid email or password"));

        } else {
            throw new RuntimeException("Invalid selected role");
        }

        if (!selectedRole.equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException(
                    "Selected role does not match this account");
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (user.getUserCode() == null || user.getUserCode().isBlank()) {
            user.setUserCode(generateUserCode(user.getRole()));
            user = userRepository.save(user);
        }

        return createAuthResponse(user);
    }

    // CREATE JWT RESPONSE
    private AuthResponse createAuthResponse(User user) {

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUserCode(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }

    // GENERATE UNIQUE DISPLAY CODE
    private String generateUserCode(String role) {

        String prefix = "TEACHER".equalsIgnoreCase(role) ? "TCH" : "STU";
        int highestNumber = 0;

        for (User user : userRepository.findAll()) {
            String existingCode = user.getUserCode();

            if (existingCode == null || !existingCode.startsWith(prefix)) {
                continue;
            }

            try {
                int number = Integer.parseInt(
                        existingCode.substring(prefix.length()));

                if (number > highestNumber) {
                    highestNumber = number;
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed user codes.
            }
        }

        return String.format("%s%03d", prefix, highestNumber + 1);
    }
}
