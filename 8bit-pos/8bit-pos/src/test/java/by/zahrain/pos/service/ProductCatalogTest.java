package by.zahrain.pos.service;

import by.zahrain.pos.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogTest {

    @Test
    void shouldAddProduct() {
        ProductCatalog catalog = new ProductCatalog();
        Product product = new Product(
                "4810151000012",
                "Milk",
                3.49,
                10
        );

        catalog.addProduct(product);

        assertEquals(1, catalog.size());
        assertSame(product, catalog.findByBarcode("4810151000012"));
    }

    @Test
    void shouldFindProductByBarcode() {
        ProductCatalog catalog = new ProductCatalog();
        Product product = new Product(
                "4810151000029",
                "Coffee",
                12.90,
                5
        );

        catalog.addProduct(product);

        Product found = catalog.findByBarcode("4810151000029");

        assertNotNull(found);
        assertEquals("Coffee", found.getName());
        assertEquals(12.90, found.getPrice(), 0.001);
    }

    @Test
    void shouldReturnNullForUnknownBarcode() {
        ProductCatalog catalog = new ProductCatalog();

        Product found = catalog.findByBarcode("9999999999999");

        assertNull(found);
    }

    @Test
    void shouldRejectDuplicateBarcode() {
        ProductCatalog catalog = new ProductCatalog();

        catalog.addProduct(
                new Product("4810151000036", "Tea", 5.20, 8)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalog.addProduct(
                        new Product("4810151000036", "Another Tea", 7.50, 3)
                )
        );

        assertEquals(
                "Product with this barcode already exists",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProduct() {
        ProductCatalog catalog = new ProductCatalog();

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.addProduct(null)
        );
    }
}
