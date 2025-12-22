package ch.heigvd.project3.auth;

import ch.heigvd.project3.users.User;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.*;
import io.jsonwebtoken.Jwts;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

public class AuthController {
  private final ConcurrentHashMap<Integer, User> users;
  private final SecretKey key = Jwts.SIG.HS256.key().build();

  public AuthController(ConcurrentHashMap<Integer, User> users) {
    this.users = users;
  }

  public void login(Context ctx) {
    User loginUser =
        ctx.bodyValidator(User.class)
            .check(obj -> obj.email() != null, "Missing email")
            .check(obj -> obj.passwordHash() != null, "Missing password")
            .get();

    Argon2 argon2 = Argon2Factory.create();

    for (User user : users.values()) {
      if (user.email().equalsIgnoreCase(loginUser.email())
          && argon2.verify(user.passwordHash(), loginUser.passwordHash().toCharArray())) {
        ctx.cookie("user", String.valueOf(user.id()));
        ctx.status(HttpStatus.OK);
        return;
      }
    }

    throw new UnauthorizedResponse();
  }

  public void logout(Context ctx) {
    ctx.removeCookie("user");
    ctx.status(HttpStatus.OK);
  }

  public void profile(Context ctx) {
    String userIdCookie = ctx.cookie("user");

    if (userIdCookie == null) {
      throw new UnauthorizedResponse();
    }

    Integer userId = Integer.parseInt(userIdCookie);

    User user = users.get(userId);

    if (user == null) {
      throw new UnauthorizedResponse();
    }

    ctx.status(HttpStatus.OK);
    ctx.json(user);
  }

  private void createJWT(User u) {}
}
