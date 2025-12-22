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

    app.post("/auth/login", authController::login);
    app.post("/auth/logout", authController::logout);
    app.get("/auth/profile", authController::profile);

    // Users routes
    app.post("/users/create", usersController::create);
    app.get("/users/list", usersController::getMany);
    app.get("/users/list/{id}", usersController::getOne);
    app.put("/users/update/{id}", usersController::update);
    app.delete("/users/remove/{id}", usersController::delete);

    // Inventory Routes
    app.post("/inventory/create", inventoryController::create);
    app.get("/inventory/list", inventoryController::getMany);
    app.get("/inventory/list/{id}", inventoryController::getOne);
    app.put("/inventory/update/{id}", inventoryController::update);
    app.delete("/inventory/remove/{id}", inventoryController::delete);

    app.start(PORT);
  }
}
