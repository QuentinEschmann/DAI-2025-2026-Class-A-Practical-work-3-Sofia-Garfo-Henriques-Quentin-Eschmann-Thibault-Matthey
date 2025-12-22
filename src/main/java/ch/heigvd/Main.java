package ch.heigvd;

import ch.heigvd.auth.AuthController;
import ch.heigvd.inventory.InventoryController;
import ch.heigvd.inventory.Item;
import ch.heigvd.users.User;
import ch.heigvd.users.UsersController;
import io.javalin.Javalin;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
  // TODO : DEFINE THE PORT
  public static final int PORT = 8080;

  public static void main(String[] args) {

    Javalin app = Javalin.create();

    ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Item> inventory = new ConcurrentHashMap<>();

    AuthController authController = new AuthController(users);
    UsersController usersController = new UsersController(users);
    InventoryController inventoryController = new InventoryController(inventory);

    app.post("/login", authController::login);
    app.post("/logout", authController::logout);
    app.get("/profile", authController::profile);

    // Users routes
    app.post("/users", usersController::create);
    app.get("/users", usersController::getMany);
    app.get("/users/{id}", usersController::getOne);
    app.put("/users/{id}", usersController::update);
    app.delete("/users/{id}", usersController::delete);

    // Inventory Routes
    app.post("/inventory", inventoryController::create);
    app.get("/inventory", inventoryController::getMany);
    app.get("/inventory/{id}", inventoryController::getOne);
    app.put("/inventory/{id}", inventoryController::update);
    app.delete("/inventory/{id}", inventoryController::delete);

    app.start(PORT);
  }
}
