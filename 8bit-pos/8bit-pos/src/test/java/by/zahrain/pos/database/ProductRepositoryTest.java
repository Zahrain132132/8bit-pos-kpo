package by.zahrain.pos.database;

import by.zahrain.pos.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProductRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndFindProduct() {
        Path databaseFile = tempDir.resolve("products.db");

        DatabaseManager databaseManager =
                new DatabaseManager(databaseFile.toString());

        databaseManager.initializeDatabase();

        ProductRepository repository =
                new ProductRepository(databaseManager);

        Product original = new Product(
                "4810151000012",
                "Milk",
                3.49,
                10
        );

        repository.save(original);

        Product restored =
                repository.findByBarcode("4810151000012");

        assertNotNull(restored);
        assertEquals(original.getBarcode(), restored.getBarcode());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getPrice(), restored.getPrice(), 0.001);
        assertEquals(original.getQuantity(), restored.getQuantity());
    }

    @Test
    void shouldReturnNullForUnknownBarcode() {
        Path databaseFile = tempDir.resolve("empty.db");

        DatabaseManager databaseManager =
                new DatabaseManager(databaseFile.toString());

        databaseManager.initializeDatabase();

        ProductRepository repository =
                new ProductRepository(databaseManager);

        assertNull(
                repository.findByBarcode("9999999999999")
        );
    }
}
