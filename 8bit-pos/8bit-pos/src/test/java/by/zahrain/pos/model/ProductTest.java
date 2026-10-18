package by.zahrain.pos.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void shouldReduceStock() {
        Product product = new Product(
                "4810151000012",
                "Milk",
                3.49,
                10
        );

        product.reduceStock(3);

        assertEquals(7, product.getQuantity());
    }

    @Test
    void shouldAllowSellingAllAvailableStock() {
        Product product = new Product(
                "4810151000029",
                "Coffee",
                12.90,
                5
        );

        product.reduceStock(5);

        assertEquals(0, product.getQuantity());
    }

    @Test
    void shouldRejectAmountGreaterThanStock() {
        Product product = new Product(
                "4810151000036",
                "Tea",
                5.20,
                3
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> product.reduceStock(4)
        );

        assertEquals(
                "Not enough products in stock",
                exception.getMessage()
        );

        assertEquals(3, product.getQuantity());
    }

    @Test
    void shouldRejectZeroAmount() {
        Product product = new Product(
                "4810151000043",
                "Chocolate",
                4.50,
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> product.reduceStock(0)
        );

        assertEquals(10, product.getQuantity());
    }

    @Test
    void shouldRejectNegativeAmount() {
        Product product = new Product(
                "4810151000050",
                "Water",
                1.80,
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> product.reduceStock(-2)
        );

        assertEquals(10, product.getQuantity());
    }
}
