package com.classroom.mindmeter.config;

import com.classroom.mindmeter.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // PASSWORD ENCODER
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AUTHENTICATION MANAGER
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // CORS CONFIGURATION
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://mindmeter.local:5173"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET", "POST", "PUT", "DELETE",
                        "PATCH", "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "Cache-Control",
                        "Pragma"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // SECURITY FILTER CHAIN
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Enable CORS before authorization checks
            .cors(cors -> cors.configurationSource(
                    corsConfigurationSource()
            ))

            // REST API uses JWT, so disable CSRF
            .csrf(csrf -> csrf.disable())

            // Stateless authentication
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            // Distinguish missing or invalid JWTs from authenticated access denial.
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED
                            )
                    )
            )

            // AUTHORIZATION RULES
            .authorizeHttpRequests(auth -> auth

                // Allow browser CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Authentication APIs
                .requestMatchers("/api/auth/**").permitAll()

                // SUBJECTS
                .requestMatchers(HttpMethod.GET, "/api/subjects/**")
                    .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/subjects")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.PUT, "/api/subjects/**")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.DELETE, "/api/subjects/**")
                    .hasRole("TEACHER")

                // QUIZZES
                .requestMatchers(HttpMethod.GET, "/api/quizzes/**")
                    .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/quizzes")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.PUT, "/api/quizzes/**")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**")
                    .hasRole("TEACHER")

                // STUDENT QUESTIONS
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/questions/student/**"
                ).hasRole("STUDENT")

                // TEACHER QUESTIONS
                .requestMatchers(HttpMethod.GET, "/api/questions/**")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.POST, "/api/questions")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.PUT, "/api/questions/**")
                    .hasRole("TEACHER")
                .requestMatchers(HttpMethod.DELETE, "/api/questions/**")
                    .hasRole("TEACHER")

                // ATTEMPTS
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/attempts/submit"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/attempts/quiz/*/start"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/attempts/all"
                ).hasRole("TEACHER")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/attempts/*"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/attempts/*/answer/*"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/attempts/*/pause",
                        "/api/attempts/*/resume"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/attempts/my"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/attempts/subject/**"
                ).hasRole("TEACHER")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/attempts/quiz/**"
                ).hasRole("TEACHER")

                // TEACHER APIs
                .requestMatchers("/api/teacher/**")
                    .hasRole("TEACHER")

                // STUDENT APIs
                .requestMatchers("/api/student/**")
                    .hasRole("STUDENT")

                .requestMatchers("/api/practice/**")
                    .hasRole("STUDENT")

                // ALL OTHER REQUESTS
                .anyRequest().authenticated()
            )

            // JWT AUTHENTICATION FILTER
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}