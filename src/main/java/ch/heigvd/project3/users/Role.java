package ch.heigvd.project3.users;

import io.javalin.security.RouteRole;

/**
 * Enumeration of user roles with associated access levels.
 */
public enum Role implements RouteRole {
  UNKNOWN(0),
  READ(1),
  WRITE(2),
  ADMIN(3);

  private final int code;

  /**
   * Constructor for Role enum.
   * @param code the access level code associated with the role
   */
  Role(int code) {
    this.code = code;
  }

  /**
   * Gets the access level code of the role.
   * @return the access level code
   */
  public int getCode() {
    return code;
  }

  /**
   * Validates if the provided role is a valid Role enum value.
   * @param role the role to validate
   * @return true if the role is valid, false otherwise
   */
  public static boolean isValid(Role role) {
    return role != null;
  }
}
