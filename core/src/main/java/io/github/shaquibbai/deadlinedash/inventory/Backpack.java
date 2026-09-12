package io.github.shaquibbai.deadlinedash.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated inventory/backpack system for Deadline Dash.
 * Supports unlimited capacity and tracks item quantities without duplicate entries.
 */
public class Backpack {

    /** Inner class representing an item and its stored quantity. */
    public static class Entry {
        private final Item item;
        private int quantity;

        public Entry(Item item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        public Item getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    private final Map<Item, Integer> items = new LinkedHashMap<>();

    public Backpack() {
    }

    /**
     * Adds a single unit of the item to the backpack.
     * If item exists, increases its quantity; otherwise adds a new entry.
     */
    public void addItem(Item item) {
        addItem(item, 1);
    }

    /**
     * Adds the specified quantity of the item to the backpack.
     */
    public void addItem(Item item, int quantity) {
        if (item == null || quantity <= 0) {
            return;
        }
        int currentQty = items.getOrDefault(item, 0);
        items.put(item, currentQty + quantity);
    }

    /**
     * Removes a single unit of the item from the backpack.
     */
    public boolean removeItem(Item item) {
        return removeItem(item, 1);
    }

    /**
     * Removes the specified quantity of the item from the backpack.
     * Removes the item entry entirely if quantity reaches zero or below.
     */
    public boolean removeItem(Item item, int quantity) {
        if (item == null || quantity <= 0 || !items.containsKey(item)) {
            return false;
        }
        int currentQty = items.get(item);
        if (currentQty <= quantity) {
            items.remove(item);
        } else {
            items.put(item, currentQty - quantity);
        }
        return true;
    }

    /**
     * Checks if the backpack contains at least 1 unit of the item.
     */
    public boolean hasItem(Item item) {
        return getQuantity(item) > 0;
    }

    /**
     * Returns the quantity of the specified item currently stored in the backpack.
     */
    public int getQuantity(Item item) {
        if (item == null) return 0;
        return items.getOrDefault(item, 0);
    }

    /**
     * Returns an unmodifiable list of item entries currently in the backpack in insertion order.
     */
    public List<Entry> getEntries() {
        List<Entry> entryList = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : items.entrySet()) {
            entryList.add(new Entry(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(entryList);
    }

    /**
     * Checks if the backpack is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns total number of distinct item types stored.
     */
    public int getDistinctItemCount() {
        return items.size();
    }

    /**
     * Clears all items from the backpack.
     */
    public void clear() {
        items.clear();
    }
}
