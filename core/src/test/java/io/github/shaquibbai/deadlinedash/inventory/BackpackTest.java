package io.github.shaquibbai.deadlinedash.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackpackTest {

    private Backpack backpack;

    @BeforeEach
    void setUp() {
        backpack = new Backpack();
    }

    @Test
    @DisplayName("New backpack should be empty")
    void testInitialEmptyBackpack() {
        assertTrue(backpack.isEmpty());
        assertEquals(0, backpack.getDistinctItemCount());
        assertEquals(0, backpack.getEntries().size());
    }

    @Test
    @DisplayName("Adding item increases its quantity rather than creating duplicate entries")
    void testAddItemQuantityAggregation() {
        Item pen = new Item("Pen");

        backpack.addItem(pen, 10);
        assertEquals(1, backpack.getDistinctItemCount());
        assertEquals(10, backpack.getQuantity(pen));

        // Re-adding same item name
        Item penDuplicate = new Item("Pen");
        backpack.addItem(penDuplicate, 5);

        assertEquals(1, backpack.getDistinctItemCount(), "Duplicate item entries should not be created");
        assertEquals(15, backpack.getQuantity(pen));
        assertTrue(backpack.hasItem(pen));
    }

    @Test
    @DisplayName("Removing item decreases quantity or removes entry when zero")
    void testRemoveItem() {
        Item notebook = new Item("Notebook");
        backpack.addItem(notebook, 5);

        assertTrue(backpack.removeItem(notebook, 2));
        assertEquals(3, backpack.getQuantity(notebook));

        assertTrue(backpack.removeItem(notebook, 3));
        assertEquals(0, backpack.getQuantity(notebook));
        assertFalse(backpack.hasItem(notebook));
        assertTrue(backpack.isEmpty());
    }

    @Test
    @DisplayName("Backpack maintains insertion order of entries")
    void testInsertionOrder() {
        Item pen = new Item("Pen");
        Item notebook = new Item("Notebook");
        Item controller = new Item("Controller");
        Item money = new Item("Money");

        backpack.addItem(pen, 10);
        backpack.addItem(notebook, 5);
        backpack.addItem(controller, 3);
        backpack.addItem(money, 500);

        List<Backpack.Entry> entries = backpack.getEntries();
        assertEquals(4, entries.size());

        assertEquals("Pen", entries.get(0).getItem().getName());
        assertEquals(10, entries.get(0).getQuantity());

        assertEquals("Notebook", entries.get(1).getItem().getName());
        assertEquals(5, entries.get(1).getQuantity());

        assertEquals("Controller", entries.get(2).getItem().getName());
        assertEquals(3, entries.get(2).getQuantity());

        assertEquals("Money", entries.get(3).getItem().getName());
        assertEquals(500, entries.get(3).getQuantity());
    }

    @Test
    @DisplayName("Unlimited capacity: can add many distinct items")
    void testUnlimitedCapacity() {
        for (int i = 0; i < 500; i++) {
            Item item = new Item("Item_" + i);
            backpack.addItem(item, i + 1);
        }

        assertEquals(500, backpack.getDistinctItemCount());
        assertEquals(50, backpack.getQuantity(new Item("Item_49")));
    }
}
