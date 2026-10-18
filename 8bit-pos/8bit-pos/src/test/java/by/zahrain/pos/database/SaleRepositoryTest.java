package by.zahrain.pos.database;

import by.zahrain.pos.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaleRepositoryTest {
    @TempDir Path tempDir;

    private DatabaseManager setup() {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("sale-test.db").toString());
        db.initializeDatabase();
        ProductRepository products = new ProductRepository(db);
        products.save(new Product("a", "Первый", 2.50, 3));
        products.save(new Product("b", "Второй", 1.10, 1));
        return db;
    }

    @Test
    void saleUpdatesStoredStockAndReturnsTotal() {
        DatabaseManager db = setup();
        BigDecimal total = new SaleRepository(db).checkout(Map.of("a", 2, "b", 1));
        assertEquals(new BigDecimal("6.10"), total);
        ProductRepository products = new ProductRepository(db);
        assertEquals(1, products.findByBarcode("a").getQuantity());
        assertEquals(0, products.findByBarcode("b").getQuantity());
    }

    @Test
    void failedSaleRollsBackAllChanges() {
        DatabaseManager db = setup();
        Map<String, Integer> request = new LinkedHashMap<>();
        request.put("a", 2);
        request.put("b", 5); // not enough stock on the SECOND item
        assertThrows(IllegalStateException.class, () -> new SaleRepository(db).checkout(request));
        ProductRepository products = new ProductRepository(db);
        assertEquals(3, products.findByBarcode("a").getQuantity());
        assertEquals(1, products.findByBarcode("b").getQuantity());
    }

    @Test
    void emptySaleIsRejected() {
        DatabaseManager db = setup();
        assertThrows(IllegalArgumentException.class, () -> new SaleRepository(db).checkout(Map.of()));
    }
}
