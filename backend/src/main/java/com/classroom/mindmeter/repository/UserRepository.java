package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRegisterNumber(
            String registerNumber
    );

    boolean existsByRegisterNumber(
            String registerNumber
    );
}