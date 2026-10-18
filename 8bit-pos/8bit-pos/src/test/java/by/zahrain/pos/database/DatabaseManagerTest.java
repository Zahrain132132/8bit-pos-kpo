package by.zahrain.pos.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateProductsTable() throws Exception {
        Path databaseFile = tempDir.resolve("test-pos.db");

        DatabaseManager databaseManager =
                new DatabaseManager(databaseFile.toString());

        databaseManager.initializeDatabase();

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     """
                     SELECT name
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name = 'products';
                     """
             )) {

            assertTrue(resultSet.next());
            assertEquals(
                    "products",
                    resultSet.getString("name")
            );
        }
    }
}
