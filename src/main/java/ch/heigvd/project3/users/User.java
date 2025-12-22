package ch.heigvd.project3.users;

public record User(
    Integer id, String firstName, String lastName, String email, String password, Role role) {}
