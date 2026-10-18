package by.zahrain.pos.service;

import by.zahrain.pos.model.Cart;
import by.zahrain.pos.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    @Test
    void shouldCheckoutCart() {
        Product milk = new Product(
                "1", "Milk", 3.50, 10
        );

        Product coffee = new Product(
                "2", "Coffee", 12.00, 5
        );

        Cart cart = new Cart();
        cart.addProduct(milk, 2);
        cart.addProduct(coffee, 1);

        CheckoutService checkoutService = new CheckoutService();

        double total = checkoutService.checkout(cart);

        assertEquals(19.00, total, 0.001);

        assertEquals(8, milk.getQuantity());
        assertEquals(4, coffee.getQuantity());

        assertTrue(cart.isEmpty());
    }

    @Test
    void shouldRejectEmptyCart() {
        Cart cart = new Cart();
        CheckoutService checkoutService = new CheckoutService();

        assertThrows(
                IllegalStateException.class,
                () -> checkoutService.checkout(cart)
        );
    }

    @Test
    void shouldRejectInsufficientStock() {
        Product coffee = new Product(
                "1", "Coffee", 12.00, 2
        );

        Cart cart = new Cart();
        cart.addProduct(coffee, 3);

        CheckoutService checkoutService = new CheckoutService();

        assertThrows(
                IllegalStateException.class,
                () -> checkoutService.checkout(cart)
        );

        assertEquals(2, coffee.getQuantity());
        assertFalse(cart.isEmpty());
    }

    @Test
    void shouldNotPartiallyChangeStockWhenCheckoutFails() {
        Product milk = new Product(
                "1", "Milk", 3.50, 10
        );

        Product coffee = new Product(
                "2", "Coffee", 12.00, 1
        );

        Cart cart = new Cart();
        cart.addProduct(milk, 2);
        cart.addProduct(coffee, 5);

        CheckoutService checkoutService = new CheckoutService();

        assertThrows(
                IllegalStateException.class,
                () -> checkoutService.checkout(cart)
        );

        // Ни один товар не должен быть списан.
        assertEquals(10, milk.getQuantity());
        assertEquals(1, coffee.getQuantity());

        // Корзина тоже должна сохраниться.
        assertEquals(2, cart.getItems().size());
    }
}
