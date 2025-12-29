package ch.heigvd.project3;

import ch.heigvd.project3.auth.AuthController;
import ch.heigvd.project3.auth.AuthUtil;
import ch.heigvd.project3.inventory.InventoryController;
import ch.heigvd.project3.inventory.Item;
import ch.heigvd.project3.users.Role;
import ch.heigvd.project3.users.User;
import ch.heigvd.project3.users.UsersController;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.Javalin;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main application class that sets up the Javalin server and configures routes for authentication,
 * user management, and inventory management.
 */
public class Main {
  // TODO : DEFINE THE PORT
  public static final int PORT = 8080;

  /**
   * Main method to start the Javalin server and configure routes.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Item> inventory = new ConcurrentHashMap<>();

    // default admin user
    Argon2 argon2 = Argon2Factory.create();
    User defaultAdmin =
        new User(
            0,
            "Admin",
            "User",
            "admin@example.com",
            AuthUtil.createHash("admin"),
            Role.ADMIN);
    users.put(defaultAdmin.id(), defaultAdmin);

    AuthController authController = new AuthController(users);
    UsersController usersController = new UsersController(users);
    InventoryController inventoryController = new InventoryController(inventory);

    // for testing purposes
    Javalin app =
        Javalin.create(
            config ->
                config.bundledPlugins.enableCors(
                    cors ->
                        cors.addRule(
                            rule -> {
                              // allow credentialed requests from any origin
                              rule.reflectClientOrigin = true;
                              rule.allowCredentials = true;
                            })));

    app.before(
        ctx -> {
          String jwt = ctx.cookie(AuthController.SESSION_COOKIE_NAME);
          if (jwt == null || jwt.isBlank()) {
            ctx.attribute(AuthController.AUTHENTICATED_USER_KEY, null);
            return;
          }

          try {
            User user = authController.validateJWT(jwt);
            ctx.attribute(AuthController.AUTHENTICATED_USER_KEY, user);
          } catch (UnauthorizedResponse e) {
            ctx.removeCookie(AuthController.SESSION_COOKIE_NAME);
            ctx.attribute(AuthController.AUTHENTICATED_USER_KEY, null);
          }
        });

    app.beforeMatched(
        ctx -> {
          var requiredRoles = ctx.routeRoles();
          if (requiredRoles == null || requiredRoles.isEmpty()) {
            return;
          }

          User authenticated = ctx.attribute(AuthController.AUTHENTICATED_USER_KEY);
          if (authenticated == null) {
            throw new UnauthorizedResponse("User not authenticated.");
          }

          boolean allowed =
              requiredRoles.stream()
                  .filter(Role.class::isInstance)
                  .map(Role.class::cast)
                  .anyMatch(
                      required ->
                          authenticated.role() != null
                              && authenticated.role().getCode() >= required.getCode());

          if (!allowed) {
            throw new ForbiddenResponse("User does not have the required role.");
          }
        });

    app.post("/auth/login", authController::login);
    app.post("/auth/logout", authController::logout);
    app.get("/auth/profile", authController::profile, Role.READ, Role.WRITE, Role.ADMIN);

    // Users routes
    app.post("/users/create", usersController::create, Role.ADMIN);
    app.get("/users/list", usersController::getMany, Role.ADMIN);
    app.get("/users/list/{id}", usersController::getOne, Role.ADMIN);
    app.put("/users/update/{id}", usersController::update, Role.ADMIN);
    app.delete("/users/remove/{id}", usersController::delete, Role.ADMIN);

    // Inventory Routes
    app.post("/inventory/create", inventoryController::create, Role.WRITE, Role.ADMIN);
    app.get("/inventory/list", inventoryController::getMany, Role.READ, Role.WRITE, Role.ADMIN);
    app.get("/inventory/list/{id}", inventoryController::getOne, Role.READ, Role.WRITE, Role.ADMIN);
    app.put("/inventory/update/{id}", inventoryController::update, Role.WRITE, Role.ADMIN);
    app.delete("/inventory/remove/{id}", inventoryController::delete, Role.WRITE, Role.ADMIN);

    app.start(PORT);
  }
}
