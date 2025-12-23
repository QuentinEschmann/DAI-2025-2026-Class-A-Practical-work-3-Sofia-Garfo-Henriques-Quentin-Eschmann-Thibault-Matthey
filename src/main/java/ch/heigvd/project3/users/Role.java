package ch.heigvd.project3.users;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
  UNKNOWN(0),
  READ(1),
  WRITE(2),
  ADMIN(3);

  private final int code;

  Role(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static boolean isValid(Role role) {
    return role != null;
  }
}
