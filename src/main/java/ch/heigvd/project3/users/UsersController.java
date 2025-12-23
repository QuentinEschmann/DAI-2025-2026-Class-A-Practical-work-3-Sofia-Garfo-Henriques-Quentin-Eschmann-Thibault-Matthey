package ch.heigvd.project3.users;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.*;
import java.util.ArrayList;
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

    String hash = createHash(newUser.passwordHash());

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
    ctx.json(user);
  }

  /**
   * Retrieves multiple users, optionally filtered by first name and/or last name.
   * @param ctx the Javalin context containing the request and response
   */
  public void getMany(Context ctx) {
    String firstName = ctx.queryParam("firstName");
    String lastName = ctx.queryParam("lastName");

    List<User> users = new ArrayList<>();

    for (User user : this.users.values()) {
      if (firstName != null && !user.firstName().equalsIgnoreCase(firstName)) {
        continue;
      }

      if (lastName != null && !user.lastName().equalsIgnoreCase(lastName)) {
        continue;
      }

      users.add(user);
    }

    ctx.status(HttpStatus.OK);
    ctx.json(users);
  }

  /**
   * Updates an existing user.
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   * @throws ConflictResponse if a user with the same email already exists
   */
  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!users.containsKey(id)) {
      throw new NotFoundResponse();
    }

    User updateUser =
        ctx.bodyValidator(User.class)
            .check(obj -> obj.firstName() != null, "Missing first name")
            .check(obj -> obj.lastName() != null, "Missing last name")
            .check(obj -> obj.email() != null, "Missing email")
            .check(obj -> obj.passwordHash() != null, "Missing password")
            .get();

    for (User user : users.values()) {
      if (updateUser.email().equalsIgnoreCase(user.email()) && user.id() != updateUser.id()) {
        throw new ConflictResponse();
      }
    }

    String hash = createHash(updateUser.passwordHash());

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
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the user with the specified ID does not exist
   */
  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!users.containsKey(id)) {
      throw new NotFoundResponse();
    }

    users.remove(id);

    ctx.status(HttpStatus.OK);
  }

  /**
   * Creates a hash of the given password using Argon2.
   * @param pass the password to hash
   * @return the hashed password
   * @throws InternalServerErrorResponse if hashing fails
   */
  private String createHash(String pass) {
    Argon2 argon2 = Argon2Factory.create();

    char[] password = pass.toCharArray();
    String hash = argon2.hash(3, 65536, 1, password);

    if (!argon2.verify(hash, password)) throw new InternalServerErrorResponse();

    return hash;
  }
}
