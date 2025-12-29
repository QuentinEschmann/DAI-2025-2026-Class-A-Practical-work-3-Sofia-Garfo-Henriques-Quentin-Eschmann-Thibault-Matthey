package ch.heigvd.project3.auth;

import ch.heigvd.project3.users.User;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.*;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

/** Controller for authentication-related actions such as login, logout, and profile retrieval. */
public class AuthController {
  public static final String SESSION_COOKIE_NAME = "session";
  public static final String AUTHENTICATED_USER_KEY = "authUser";
  private final ConcurrentHashMap<Integer, User> users;
  private static final SecretKey key = Jwts.SIG.HS256.key().build();

  public AuthController(ConcurrentHashMap<Integer, User> users) {
    this.users = users;
  }

  /**
   * Handles user login by validating credentials and issuing a JWT upon successful authentication.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws UnauthorizedResponse if the credentials are invalid
   */
  @OpenApi(
      path = "/auth/login",
      methods = {HttpMethod.POST},
      summary = "User login",
      description = "Authenticates a user and issues a JWT upon successful login.",
      requestBody =
          @OpenApiRequestBody(
              content = {
                @OpenApiContent(
                    type = "application/json",
                    example = "{\"email\":\"user@example.com\",\"password\":\"password\"}")
              }),
      tags = {"Authentication"},
      responses = {
        @OpenApiResponse(status = "200", description = "Login successful"),
        @OpenApiResponse(status = "401", description = "Invalid email or password")
      })
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

    throw new UnauthorizedResponse("Invalid email or password.");
  }

  /**
   * Handles user logout by removing the session cookie.
   *
   * @param ctx the Javalin context containing the request and response
   */
  @OpenApi(
      path = "/auth/logout",
      methods = {HttpMethod.POST},
      summary = "User logout",
      description = "Logs out the user by removing the session cookie.",
      tags = {"Authentication"},
      responses = {@OpenApiResponse(status = "200", description = "Logout successful")})
  public void logout(Context ctx) {
    ctx.removeCookie(SESSION_COOKIE_NAME);
    ctx.status(HttpStatus.OK);
  }

  /**
   * Retrieves the profile of the authenticated user.
   *
   * @param ctx the Javalin context containing the request and response
   */
  @OpenApi(
      path = "/auth/profile",
      methods = {HttpMethod.GET},
      summary = "Get user profile",
      description = "Retrieves the profile of the authenticated user.",
      tags = {"Authentication"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Profile retrieved successfully",
            content = {@OpenApiContent(from = User.class)}),
        @OpenApiResponse(status = "401", description = "User not authenticated")
      })
  public void profile(Context ctx) {
    User user = ctx.attribute(AUTHENTICATED_USER_KEY);
    if (user == null) {
      throw new UnauthorizedResponse("User not authenticated.");
    }

    ctx.status(HttpStatus.OK);
    ctx.json(user);
  }

  /**
   * Creates a JWT for the given user.
   *
   * @param u the user for whom to create the JWT
   * @return the generated JWT as a string
   */
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

  /**
   * Validates the provided JWT and returns the associated user if valid.
   *
   * @param jwt the JWT to validate
   * @return the user associated with the JWT
   * @throws UnauthorizedResponse if the JWT is invalid or the user does not exist
   */
  public User validateJWT(String jwt) {
    if (jwt == null || jwt.isBlank()) {
      throw new UnauthorizedResponse("Missing or empty JWT.");
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
        throw new UnauthorizedResponse("Invalid JWT: missing user ID.");
      }

      User user = users.get(id);
      if (user == null
          || (claims.get("email") != null
              && !claims.get("email", String.class).equalsIgnoreCase(user.email()))) {
        throw new UnauthorizedResponse("Invalid JWT: user does not exist.");
      }

      return user;
    } catch (JwtException | IllegalArgumentException e) {
      throw new UnauthorizedResponse("Invalid JWT.");
    }
  }
}
