package ch.heigvd.project3.auth;

import ch.heigvd.project3.users.User;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

public class AuthController {
  public static final String SESSION_COOKIE_NAME = "session";
  public static final String AUTHENTICATED_USER_KEY = "authUser";
  private final ConcurrentHashMap<Integer, User> users;
  private static final SecretKey key = Jwts.SIG.HS256.key().build();

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
      if (user.email().equalsIgnoreCase(loginUser.email())) {
        if (argon2.verify(user.passwordHash(), loginUser.passwordHash().toCharArray())) {
          ctx.cookie(SESSION_COOKIE_NAME, createJWT(user));
          ctx.attribute(AUTHENTICATED_USER_KEY, user);
          ctx.status(HttpStatus.OK);
          return;
        }
        break;
      }
    }

    throw new UnauthorizedResponse();
  }

  public void logout(Context ctx) {
    ctx.removeCookie(SESSION_COOKIE_NAME);
    ctx.status(HttpStatus.OK);
  }

  public void profile(Context ctx) {
    User user = ctx.attribute(AUTHENTICATED_USER_KEY);
    if (user == null) {
      throw new UnauthorizedResponse();
    }

    ctx.status(HttpStatus.OK);
    ctx.json(user);
  }

  private String createJWT(User u) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(u.id()))
        .claim("id", u.id())
        .claim("email", u.email())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofHours(1))))
        .signWith(key)
        .compact();
  }

  public User validateJWT(String jwt) {
    if (jwt == null || jwt.isBlank()) {
      throw new UnauthorizedResponse();
    }

    try {
      Jws<Claims> parsedJwt = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt);
      Claims claims = parsedJwt.getPayload();

      Integer id = claims.get("id", Integer.class);
      if (id == null) {
        String subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
          id = Integer.parseInt(subject);
        }
      }

      if (id == null) {
        throw new UnauthorizedResponse();
      }

      User user = users.get(id);
      if (user == null
          || (claims.get("email") != null
              && !claims.get("email", String.class).equalsIgnoreCase(user.email()))) {
        throw new UnauthorizedResponse();
      }

      return user;
    } catch (JwtException | IllegalArgumentException e) {
      throw new UnauthorizedResponse();
    }
  }
}
