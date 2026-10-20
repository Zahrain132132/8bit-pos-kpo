
package by.zahrain.pos.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    @Test
    void shouldCalculateSubtotal() {
        Product product = new Product(
                "101", "Coffee", 12.50, 10
        );

        CartItem item = new CartItem(product, 2);

        assertEquals(25.00, item.getSubtotal(), 0.001);
    }

    @Test
    void shouldIncreaseQuantity() {
        Product product = new Product(
                "102", "Milk", 3.50, 10
        );

        CartItem item = new CartItem(product, 2);
        item.increaseQuantity(3);

        assertEquals(5, item.getQuantity());
        assertEquals(17.50, item.getSubtotal(), 0.001);
    }

    @Test
    void shouldRejectNullProduct() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CartItem(null, 1)
        );
    }

    @Test
    void shouldRejectInvalidQuantity() {
        Product product = new Product(
                "103", "Tea", 5.00, 10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CartItem(product, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CartItem(product, -2)
        );
    }

    @Test
    void shouldRejectInvalidIncrease() {
        Product product = new Product(
                "104", "Water", 2.00, 10
        );

        CartItem item = new CartItem(product, 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> item.increaseQuantity(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> item.increaseQuantity(-3)
        );

        assertEquals(2, item.getQuantity());
    }
}
