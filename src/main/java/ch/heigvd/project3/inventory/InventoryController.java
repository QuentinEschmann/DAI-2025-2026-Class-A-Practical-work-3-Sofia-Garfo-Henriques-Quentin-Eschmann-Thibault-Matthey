package ch.heigvd.project3.inventory;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for inventory-related actions such as creating, retrieving, updating, and deleting items.
 */
public class InventoryController {
  // TODO : Create an object seems to facilitate the database, i store int, item so we are also able
  // to expand with ease our item, for example we could stock the number of reserved item there and
  // it will be easier
  private final ConcurrentHashMap<Integer, Item> inventory;

  private final AtomicInteger uniqueId = new AtomicInteger(1);

  public InventoryController(ConcurrentHashMap<Integer, Item> inventory) {
    this.inventory = inventory;
  }

  /**
   * Creates a new item in the inventory.
   * @param ctx the Javalin context containing the request and response
   * @throws ConflictResponse if an item with the same name already exists
   */
  public void create(Context ctx) {
    Item newitem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() >= 0, "Incorrect ammount")
            .get();

    String name = newitem.name();
    for (Item item : inventory.values()) {
      if (name.equalsIgnoreCase(item.name())) {
        throw new ConflictResponse();
      }
    }

    newitem = new Item(uniqueId.getAndIncrement(), newitem.name(), newitem.num());

    inventory.put(newitem.id(), newitem);

    ctx.status(HttpStatus.CREATED);
    ctx.json(newitem);
  }

  /**
   * Retrieves a single item from the inventory by its ID.
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   */
  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    Item item = inventory.get(id);
    if (item == null) {
      throw new NotFoundResponse();
    }

    ctx.status(HttpStatus.OK);
    ctx.json(item);
  }

  /**
   * Retrieves multiple items from the inventory, optionally filtered by name.
   * @param ctx the Javalin context containing the request and response
   */
  public void getMany(Context ctx) {
    String name = ctx.queryParam("name");

    List<Item> items = new ArrayList<>();

    if (name == null || name.equalsIgnoreCase("all")) {
      items.addAll(inventory.values());
    } else {
      for (Item item : inventory.values()) {
        if (item.name().equalsIgnoreCase(name)) {
          items.add(item);
        }
      }
    }

    ctx.status(HttpStatus.OK);
    ctx.json(items);
  }

  /**
   * Updates an existing item in the inventory.
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   * @throws ConflictResponse if an item with the same name already exists
   */
  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse();
    }

    Item updateItem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() >= 0, "Incorrect ammount")
            .get();

    for (Item item : inventory.values()) {
      if (updateItem.name().equalsIgnoreCase(item.name()) && item.id() != id) {
        throw new ConflictResponse();
      }
    }

    updateItem = new Item(id, updateItem.name(), updateItem.num());

    inventory.put(id, updateItem);

    ctx.status(HttpStatus.OK);
  }

  /**
   * Deletes an item from the inventory by its ID.
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   */
  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse();
    }

    inventory.remove(id);

    ctx.status(HttpStatus.OK);
  }
}
