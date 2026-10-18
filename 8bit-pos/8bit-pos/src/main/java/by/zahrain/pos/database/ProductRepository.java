package by.zahrain.pos.database;

import by.zahrain.pos.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRepository {

    private final DatabaseManager databaseManager;

    public ProductRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(Product product) {
        String sql = """
                INSERT INTO products (barcode, name, price, quantity)
                VALUES (?, ?, ?, ?);
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getBarcode());
            statement.setString(2, product.getName());
            statement.setDouble(3, product.getPrice());
            statement.setInt(4, product.getQuantity());

            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to save product",
                    exception
            );
        }
    }

    public Product findByBarcode(String barcode) {
        String sql = """
                SELECT barcode, name, price, quantity
                FROM products
                WHERE barcode = ?;
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, barcode);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new Product(
                        resultSet.getString("barcode"),
                        resultSet.getString("name"),
                        resultSet.getDouble("price"),
                        resultSet.getInt("quantity")
                );
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to find product",
                    exception
            );
        }
    }
public List<Product> findAll() {
    String sql = """
            SELECT barcode, name, price, quantity
            FROM products
            ORDER BY name;
            """;

    List<Product> products = new ArrayList<>();

    try (Connection connection = databaseManager.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {

        while (resultSet.next()) {
            products.add(
                    new Product(
                            resultSet.getString("barcode"),
                            resultSet.getString("name"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("quantity")
                    )
            );
        }

        return products;

    } catch (SQLException exception) {
        throw new IllegalStateException(
                "Failed to load products",
                exception
        );
    }
}
}
