package ch.heigvd.project3.users;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Represents a user with personal details, email, password hash, and role. */
public record User(
    Integer id,
    String firstName,
    String lastName,
    String email,
    @JsonAlias("password") String passwordHash,
    Role role) {}
