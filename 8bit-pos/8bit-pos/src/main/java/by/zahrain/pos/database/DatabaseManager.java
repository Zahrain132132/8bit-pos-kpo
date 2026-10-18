package by.zahrain.pos.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final String url;

    public DatabaseManager(String databasePath) {
        this.url = "jdbc:sqlite:" + databasePath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void initializeDatabase() {
        String createProductsTable = """
                CREATE TABLE IF NOT EXISTS products (
                    barcode TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    price REAL NOT NULL CHECK (price >= 0),
                    quantity INTEGER NOT NULL CHECK (quantity >= 0)
                );
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createProductsTable);

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to initialize database",
                    exception
            );
        }
    }
}
