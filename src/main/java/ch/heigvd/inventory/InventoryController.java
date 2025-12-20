package ch.heigvd.inventory;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryController {
  // TODO : Create an object seems to facilitate the database, i store int, item so we are also able
  // to expand with ease our item, for example we could stock the number of reserved item there and
  // it will be easier
  private final ConcurrentHashMap<Integer, Item> inventory;

  private final AtomicInteger uniqueId = new AtomicInteger(1);

  public InventoryController(ConcurrentHashMap<Integer, Item> inventory) {
    this.inventory = inventory;
  }

  public void create(Context ctx) {
    Item newitem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() <= 0, "Incorrect ammount")
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

  public void getOne(Context ctx) {
    String name = ctx.queryParam("name");

    Item itemSearched = null;

    for (Item item : inventory.values()) {
      if (name.equalsIgnoreCase(item.name())) {
        itemSearched = item;
      }
    }

    if (itemSearched == null) {
      throw new NotFoundResponse();
    }

    ctx.json(itemSearched);
  }

  public void getMany(Context ctx) {
    String name = ctx.queryParam("Name");

    List<Item> items = new ArrayList<>();

    if (name.equals("ALL")) {
      for (ConcurrentHashMap.Entry<Integer, Item> e : inventory.entrySet()) {
        items.add(e.getValue());
      }
    } else {

      for (Item item : this.inventory.values()) {
        if (name != null && !item.name().equalsIgnoreCase(name)) {
          continue;
        }

        items.add(item);
      }
    }

    ctx.json(items);
  }

  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse();
    }

    Item updateItem =
        ctx.bodyValidator(Item.class)
            .check(obj -> obj.name() != null, "Missing item's name")
            .check(obj -> obj.num() <= 0, "Incorrect ammount")
            .get();

    for (Item item : inventory.values()) {
      if (updateItem.name().equalsIgnoreCase(item.name())) {
        throw new ConflictResponse();
      }
    }

    inventory.put(id, updateItem);

    ctx.json(updateItem);
  }

  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!inventory.containsKey(id)) {
      throw new NotFoundResponse();
    }

    inventory.remove(id);

    ctx.status(HttpStatus.NO_CONTENT);
  }
}
