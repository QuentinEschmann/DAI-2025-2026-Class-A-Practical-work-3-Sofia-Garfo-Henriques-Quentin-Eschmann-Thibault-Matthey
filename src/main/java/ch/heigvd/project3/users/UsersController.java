package ch.heigvd.project3.users;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import ch.heigvd.project3.auth.AuthUtil;

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
  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    User user = users.get(id);

    if (user == null) {
      throw new NotFoundResponse();
    }

    ctx.status(HttpStatus.OK);
    ctx.json(toPublicUser(user));
  }

  /**
   * Retrieves multiple users, optionally filtered by first name and/or last name.
   *
   * @param ctx the Javalin context containing the request and response
   */
  public void getMany(Context ctx) {
    String firstName = ctx.queryParam("firstName");
    String lastName = ctx.queryParam("lastName");

    List<PublicUser> users = new ArrayList<>();

    for (User user : this.users.values()) {
      if (firstName != null && !user.firstName().equalsIgnoreCase(firstName)) {
        continue;
      }

      if (lastName != null && !user.lastName().equalsIgnoreCase(lastName)) {
        continue;
      }

      users.add(toPublicUser(user));
    }

    ctx.status(HttpStatus.OK);
    ctx.json(users);
  }

  /**
   * Updates an existing user.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   * @throws ConflictResponse if a user with the same email already exists
   */
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
}
