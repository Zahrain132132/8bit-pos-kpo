package by.zahrain.pos.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    void shouldAddProduct() {
        Cart cart = new Cart();
        Product milk = new Product(
                "4810151000012", "Milk", 3.49, 10
        );

        cart.addProduct(milk, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void shouldIncreaseQuantityForSameProduct() {
        Cart cart = new Cart();
        Product coffee = new Product(
                "4810151000029", "Coffee", 12.90, 10
        );

        cart.addProduct(coffee, 1);
        cart.addProduct(coffee, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    void shouldCalculateTotal() {
        Cart cart = new Cart();

        Product milk = new Product(
                "4810151000012", "Milk", 3.50, 10
        );

        Product coffee = new Product(
                "4810151000029", "Coffee", 12.00, 5
        );

        cart.addProduct(milk, 2);
        cart.addProduct(coffee, 1);

        assertEquals(19.00, cart.getTotal(), 0.001);
    }

    @Test
    void shouldRemoveProduct() {
        Cart cart = new Cart();

        Product tea = new Product(
                "4810151000036", "Tea", 5.20, 8
        );

        cart.addProduct(tea, 2);
        cart.removeProduct("4810151000036");

        assertTrue(cart.isEmpty());
    }

    @Test
    void shouldClearCart() {
        Cart cart = new Cart();

        cart.addProduct(
                new Product("1", "Milk", 3.49, 10),
                2
        );

        cart.addProduct(
                new Product("2", "Coffee", 12.90, 5),
                1
        );

        cart.clear();

        assertTrue(cart.isEmpty());
        assertEquals(0.0, cart.getTotal(), 0.001);
    }

    @Test
    void shouldRejectInvalidQuantity() {
        Cart cart = new Cart();

        Product product = new Product(
                "1", "Water", 1.80, 10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cart.addProduct(product, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cart.addProduct(product, -1)
        );
    }
}
