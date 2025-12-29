package ch.heigvd.project3.inventory;

import io.javalin.http.*;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for inventory-related actions such as creating, retrieving, updating, and deleting
 * items.
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
   *
   * @param ctx the Javalin context containing the request and response
   * @throws ConflictResponse if an item with the same name already exists
   */
  @OpenApi(
      path = "/inventory/create",
      methods = {HttpMethod.POST},
      summary = "Create a new item",
      description = "Creates a new item in the inventory.",
      requestBody =
          @OpenApiRequestBody(
              content = {
                @OpenApiContent(
                    type = "application/json",
                    example = "{\"name\":\"itemName\",\"num\":10}")
              }),
      tags = {"Inventory Management"},
      responses = {
        @OpenApiResponse(
            status = "201",
            description = "Item created successfully",
            content = {@OpenApiContent(from = Item.class)}),
        @OpenApiResponse(status = "409", description = "Item with the same name already exists")
      })
  public void create(Context ctx) {
    Item newitem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() >= 0, "Incorrect ammount")
            .get();

    String name = newitem.name();
    for (Item item : inventory.values()) {
      if (name.equalsIgnoreCase(item.name())) {
        throw new ConflictResponse("Item with the same name already exists.");
      }
    }

    newitem = new Item(uniqueId.getAndIncrement(), newitem.name(), newitem.num());

    inventory.put(newitem.id(), newitem);

    ctx.status(HttpStatus.CREATED);
    ctx.json(newitem);
  }

  /**
   * Retrieves a single item from the inventory by its ID.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   */
  @OpenApi(
      path = "/inventory/list/{id}",
      methods = {HttpMethod.GET},
      summary = "Get an item by ID",
      description = "Retrieves a single item from the inventory by its ID.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      tags = {"Inventory Management"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Item retrieved successfully",
            content = {@OpenApiContent(from = Item.class)}),
        @OpenApiResponse(status = "404", description = "Item not found")
      })
  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    Item item = inventory.get(id);
    if (item == null) {
      throw new NotFoundResponse("Item not found.");
    }

    ctx.status(HttpStatus.OK);
    ctx.json(item);
  }

  /**
   * Retrieves multiple items from the inventory, optionally filtered by name.
   *
   * @param ctx the Javalin context containing the request and response
   */
  @OpenApi(
      path = "/inventory/list",
      methods = {HttpMethod.GET},
      summary = "Get multiple items",
      description = "Retrieves multiple items from the inventory, optionally filtered by name.",
      tags = {"Inventory Management"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Items retrieved successfully",
            content = {@OpenApiContent(from = Item[].class)})
      })
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
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   * @throws ConflictResponse if an item with the same name already exists
   */
  @OpenApi(
      path = "/inventory/update/{id}",
      methods = {HttpMethod.PUT},
      summary = "Update an item",
      description = "Updates an existing item in the inventory.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      requestBody =
          @OpenApiRequestBody(
              content = {
                @OpenApiContent(
                    type = "application/json",
                    example = "{\"name\":\"itemName\",\"num\":10}")
              }),
      tags = {"Inventory Management"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Item updated successfully",
            content = {@OpenApiContent(from = Item.class)}),
        @OpenApiResponse(status = "404", description = "Item not found"),
        @OpenApiResponse(status = "409", description = "Item with the same name already exists")
      })
  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse("Item not found.");
    }

    Item updateItem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() >= 0, "Incorrect ammount")
            .get();

    for (Item item : inventory.values()) {
      if (updateItem.name().equalsIgnoreCase(item.name()) && item.id() != id) {
        throw new ConflictResponse("Item with the same name already exists.");
      }
    }

    updateItem = new Item(id, updateItem.name(), updateItem.num());

    inventory.put(id, updateItem);

    ctx.status(HttpStatus.OK);
    ctx.json(updateItem);
  }

  /**
   * Deletes an item from the inventory by its ID.
   *
   * @param ctx the Javalin context containing the request and response
   * @throws NotFoundResponse if the item with the specified ID does not exist
   */
  @OpenApi(
      path = "/inventory/remove/{id}",
      methods = {HttpMethod.DELETE},
      summary = "Delete an item",
      description = "Deletes an item from the inventory by its ID.",
      pathParams = {
        @OpenApiParam(name = "id", type = Integer.class, description = "User ID", required = true)
      },
      tags = {"Inventory Management"},
      responses = {
        @OpenApiResponse(status = "200", description = "Item deleted successfully"),
        @OpenApiResponse(status = "404", description = "Item not found")
      })
  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse("Item not found.");
    }

    inventory.remove(id);

    ctx.status(HttpStatus.OK);
  }
}
