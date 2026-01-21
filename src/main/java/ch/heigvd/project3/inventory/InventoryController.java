package ch.heigvd.project3.inventory;

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
            content = {@OpenApiContent(from = Item.class)},
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
        @OpenApiResponse(status = "404", description = "Item not found"),
        @OpenApiResponse(
            status = "304",
            description = "Item not modified",
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

    Item item = inventory.get(id);
    if (item == null) {
      throw new NotFoundResponse("Item not found.");
    }
    String etag = computeItemEtag(item);
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
            content = {@OpenApiContent(from = Item[].class)},
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
            description = "Items not modified",
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

    String etag = computeListEtag(items, name);
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

  /**
   * Computes the etag for a single item
   *
   * @param item Item, the item to compute the etag for
   * @return String, the computed etag
   */
  private String computeItemEtag(Item item) {
    String payload = item.id() + "|" + item.name() + "|" + item.num();
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
   * Checks if the provided ETag matches any of the ETags in the If-None-Match header.
   *
   * @param ifNoneMatch the value of the If-None-Match header
   * @param etag the ETag to compare against
   * @return true if there is a match, false otherwise
   */
  private boolean etagMatches(String ifNoneMatch, String etag) {
    if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
      return false;
    }
    String candidate = ifNoneMatch.trim();
    if ("*".equals(candidate)) {
      return true;
    }
    // Support a simple comma-separated list of tags
    String[] parts = candidate.split(",");
    for (String part : parts) {
      if (part.trim().equals(etag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Computes the etag for multiples items item
   *
   * @param items List<Items>, list of all items in the db
   * @param filterName String, name filter used
   * @return String, the computed etag
   */
  private String computeListEtag(List<Item> items, String filterName) {
    List<Item> ordered = new ArrayList<>(items);
    ordered.sort((a, b) -> Integer.compare(a.id(), b.id()));

    String key =
        (filterName == null || filterName.equalsIgnoreCase("all"))
            ? "all"
            : filterName.toLowerCase();

    StringBuilder sb = new StringBuilder(key);
    for (Item it : ordered) {
      sb.append('|').append(it.id()).append(':').append(it.name()).append(':').append(it.num());
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
