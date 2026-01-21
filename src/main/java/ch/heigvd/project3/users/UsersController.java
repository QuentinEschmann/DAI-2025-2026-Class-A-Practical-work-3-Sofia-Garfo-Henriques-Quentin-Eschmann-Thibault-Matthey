package ch.heigvd.project3.users;

import ch.heigvd.project3.auth.AuthUtil;
import io.javalin.http.*;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for user-related actions such as creating, retrieving, updating, and deleting users.
 */
public class UsersController {
  private final ConcurrentHashMap<Integer, User> users;

  private final AtomicInteger uniqueId = new AtomicInteger(1);

  public UsersController(ConcurrentHashMap<Integer, User> users) {
    this.users = users;
  }

  /**
   * Creates a new user.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws ConflictResponse if a user with the same email already exists
   */
  @OpenApi(
      path = "/users/create",
      methods = {HttpMethod.POST},
      summary = "Create a new user",
      description = "Creates a new user in the system.",
      requestBody =
          @OpenApiRequestBody(
              content = {
                @OpenApiContent(
                    type = "application/json",
                    example =
                        "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\",\"password\":\"password\",\"role\":\"1\"}")
              }),
      tags = {"User Management"},
      responses = {
        @OpenApiResponse(status = "201", description = "User created successfully"),
        @OpenApiResponse(status = "409", description = "User with the same email already exists")
      })
  public void create(Context ctx) {
    User newUser =
        ctx.bodyValidator(User.class)
            .check(obj -> obj.firstName() != null, "Missing first name")
            .check(obj -> obj.lastName() != null, "Missing last name")
            .check(obj -> obj.email() != null, "Missing email")
            .check(obj -> obj.passwordHash() != null, "Missing password")
            .check(obj -> Role.isValid(obj.role()), "Missing role")
            .get();

    for (User user : users.values()) {
      if (newUser.email().equalsIgnoreCase(user.email())) {
        throw new ConflictResponse();
      }
    }

    String hash = AuthUtil.createHash(newUser.passwordHash());

    newUser =
        new User(
            uniqueId.getAndIncrement(),
            newUser.firstName(),
            newUser.lastName(),
            newUser.email(),
            hash,
            newUser.role());

    users.put(newUser.id(), newUser);

    ctx.status(HttpStatus.CREATED);
  }

  /**
   * Retrieves a single user by ID.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   */
  @OpenApi(
      path = "/users/list/{id}",
      methods = {HttpMethod.GET},
      summary = "Get a user by ID",
      description = "Retrieves a single user by their ID.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      tags = {"User Management"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "User retrieved successfully",
            content = {@OpenApiContent(from = PublicUser.class)},
            headers = {
              @OpenApiParam(
                  name = "ETag",
                  description = "Weak entity tag to support cache revalidation",
                  type = String.class),
              @OpenApiParam(
                  name = "Cache-Control",
                  description = "Cache policy directives for this response",
                  type = String.class)
            }),
        @OpenApiResponse(status = "404", description = "User not found"),
        @OpenApiResponse(
            status = "304",
            description = "User not modified",
            headers = {
              @OpenApiParam(
                  name = "ETag",
                  description = "Weak entity tag to support cache revalidation",
                  type = String.class),
              @OpenApiParam(
                  name = "Cache-Control",
                  description = "Cache policy directives for this response",
                  type = String.class)
            })
      })
  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    User user = users.get(id);

    if (user == null) {
      throw new NotFoundResponse();
    }

    PublicUser publicUser = toPublicUser(user);
    String etag = computeUserEtag(publicUser);
    String ifNoneMatch = ctx.header(Header.IF_NONE_MATCH);

    if (etagMatches(ifNoneMatch, etag)) {
      ctx.header(Header.ETAG, etag);
      ctx.header(Header.CACHE_CONTROL, "public, max-age=0, must-revalidate");
      ctx.status(HttpStatus.NOT_MODIFIED);
      return;
    }

    ctx.header(Header.ETAG, etag);
    ctx.header(Header.CACHE_CONTROL, "public, max-age=0, must-revalidate");
    ctx.status(HttpStatus.OK);
    ctx.json(publicUser);
  }

  /**
   * Retrieves multiple users, optionally filtered by first name and/or last name.
   *
   * @param ctx the Javalin context containing the request and response
   */
  @OpenApi(
      path = "/users/list",
      methods = {HttpMethod.GET},
      summary = "Get multiple users",
      description = "Retrieves multiple users, optionally filtered by first name and/or last name.",
      tags = {"User Management"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Users retrieved successfully",
            content = {@OpenApiContent(from = PublicUser[].class)},
            headers = {
              @OpenApiParam(
                  name = "ETag",
                  description = "Weak entity tag to support cache revalidation",
                  type = String.class),
              @OpenApiParam(
                  name = "Cache-Control",
                  description = "Cache policy directives for this response",
                  type = String.class)
            }),
        @OpenApiResponse(
            status = "304",
            description = "Users not modified",
            headers = {
              @OpenApiParam(
                  name = "ETag",
                  description = "Weak entity tag to support cache revalidation",
                  type = String.class),
              @OpenApiParam(
                  name = "Cache-Control",
                  description = "Cache policy directives for this response",
                  type = String.class)
            })
      })
  public void getMany(Context ctx) {
    String firstName = ctx.queryParam("firstName");
    String lastName = ctx.queryParam("lastName");

    List<PublicUser> usersResult = new ArrayList<>();

    for (User user : this.users.values()) {
      if (firstName != null && !user.firstName().equalsIgnoreCase(firstName)) {
        continue;
      }

      if (lastName != null && !user.lastName().equalsIgnoreCase(lastName)) {
        continue;
      }

      usersResult.add(toPublicUser(user));
    }

    String etag = computeUserListEtag(usersResult, firstName, lastName);
    String ifNoneMatch = ctx.header(Header.IF_NONE_MATCH);

    if (etagMatches(ifNoneMatch, etag)) {
      ctx.header(Header.ETAG, etag);
      ctx.header(Header.CACHE_CONTROL, "private, max-age=0, must-revalidate");
      ctx.status(HttpStatus.NOT_MODIFIED);
      return;
    }

    ctx.header(Header.ETAG, etag);
    ctx.header(Header.CACHE_CONTROL, "private, max-age=0, must-revalidate");
    ctx.status(HttpStatus.OK);
    ctx.json(usersResult);
  }

  /**
   * Updates an existing user.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   * @throws ConflictResponse if a user with the same email already exists
   */
  @OpenApi(
      path = "/users/update/{id}",
      methods = {HttpMethod.PUT},
      summary = "Update an existing user",
      description = "Updates the details of an existing user.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      requestBody =
          @OpenApiRequestBody(
              content = {
                @OpenApiContent(
                    type = "application/json",
                    example =
                        "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\",\"password\":\"password\",\"role\":\"1\"}")
              }),
      tags = {"User Management"},
      responses = {
        @OpenApiResponse(status = "200", description = "User updated successfully"),
        @OpenApiResponse(status = "404", description = "User not found"),
        @OpenApiResponse(status = "409", description = "User with the same email already exists")
      })
  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!users.containsKey(id)) {
      throw new NotFoundResponse("User not found.");
    }

    User updateUser =
        ctx.bodyValidator(User.class)
            .check(obj -> obj.firstName() != null, "Missing first name")
            .check(obj -> obj.lastName() != null, "Missing last name")
            .check(obj -> obj.email() != null, "Missing email")
            .check(obj -> obj.passwordHash() != null, "Missing password")
            .check(obj -> Role.isValid(obj.role()), "Missing role")
            .get();

    for (User user : users.values()) {
      if (updateUser.email().equalsIgnoreCase(user.email()) && user.id() != id) {
        throw new ConflictResponse("Email already in use by another user.");
      }
    }

    if (users.get(id).role() == Role.ADMIN && updateUser.role() != Role.ADMIN) {
      long adminCount = users.values().stream().filter(u -> u.role() == Role.ADMIN).count();
      if (adminCount <= 1) {
        throw new ConflictResponse("Cannot remove the last admin user.");
      }
    }

    String hash = AuthUtil.createHash(updateUser.passwordHash());

    updateUser =
        new User(
            id,
            updateUser.firstName(),
            updateUser.lastName(),
            updateUser.email(),
            hash,
            updateUser.role());

    users.put(id, updateUser);

    ctx.status(HttpStatus.OK);
  }

  /**
   * Deletes a user by ID.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   */
  @OpenApi(
      path = "/users/remove/{id}",
      methods = {HttpMethod.DELETE},
      summary = "Delete a user",
      description = "Deletes a user by their ID.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      tags = {"User Management"},
      responses = {
        @OpenApiResponse(status = "200", description = "User deleted successfully"),
        @OpenApiResponse(status = "404", description = "User not found")
      })
  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!users.containsKey(id)) {
      throw new NotFoundResponse("User not found.");
    }

    users.remove(id);

    ctx.status(HttpStatus.OK);
  }

  /**
   * Converts a User to a PublicUser by omitting sensitive information.
   *
   * @param u the User object to convert
   * @return a PublicUser object with sensitive information omitted
   */
  private PublicUser toPublicUser(User u) {
    return new PublicUser(u.id(), u.firstName(), u.lastName(), u.email(), u.role());
  }

  /**
   * Checks if the provided ETag matches the If-None-Match header.
   *
   * @param ifNoneMatch String, the value of the If-None-Match header
   * @param etag String, the computed ETag of the resource
   * @return boolean, true if there is a match, false otherwise
   */
  private boolean etagMatches(String ifNoneMatch, String etag) {
    if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
      return false;
    }
    String candidate = ifNoneMatch.trim();
    if ("*".equals(candidate)) {
      return true;
    }
    String[] parts = candidate.split(",");
    for (String part : parts) {
      if (part.trim().equals(etag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Computes a weak ETag for a PublicUser.
   *
   * @param user PublicUser, the PublicUser for which to compute the ETag
   * @return a weak ETag string representing the PublicUser
   */
  private String computeUserEtag(PublicUser user) {
    String payload =
        user.id()
            + "|"
            + user.firstName()
            + "|"
            + user.lastName()
            + "|"
            + user.email()
            + "|"
            + user.role();
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
      return "W/\"" + b64 + "\"";
    } catch (NoSuchAlgorithmException e) {
      String fallback = Integer.toHexString(payload.hashCode());
      return "W/\"" + fallback + "\"";
    }
  }

  /**
   * Computes a weak ETag for a list of PublicUsers, considering optional filters.
   *
   * @param users List<PublicUser>, the list of PublicUsers to compute the ETag for
   * @param filterFirstName String, optional first name filter
   * @param filterLastName String, optional last name filter
   * @return a weak ETag string representing the list of PublicUsers and applied filters
   */
  private String computeUserListEtag(
      List<PublicUser> users, String filterFirstName, String filterLastName) {
    List<PublicUser> ordered = new ArrayList<>(users);
    ordered.sort((a, b) -> Integer.compare(a.id(), b.id()));

    String first = filterFirstName == null ? "*" : filterFirstName.trim().toLowerCase();
    String last = filterLastName == null ? "*" : filterLastName.trim().toLowerCase();

    StringBuilder sb = new StringBuilder(first).append('|').append(last);
    for (PublicUser user : ordered) {
      sb.append('|')
          .append(user.id())
          .append(':')
          .append(user.firstName())
          .append(':')
          .append(user.lastName())
          .append(':')
          .append(user.email())
          .append(':')
          .append(user.role());
    }

    byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(data));
      return "W/\"" + b64 + "\"";
    } catch (NoSuchAlgorithmException e) {
      String fallback = Integer.toHexString(sb.toString().hashCode());
      return "W/\"" + fallback + "\"";
    }
  }
}
