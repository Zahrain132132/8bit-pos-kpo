package by.zahrain.pos.database;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Performs one entire sale in a SQLite transaction. Either ALL stock is
 * reduced or NOTHING changes, including when a later line runs out of stock.
 */
public final class SaleRepository {
    private final DatabaseManager databaseManager;

    public SaleRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public BigDecimal checkout(Map<String, Integer> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("Корзина пуста");
        }

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                String lookup = "SELECT name, price, quantity FROM products WHERE barcode = ?";
                String update = "UPDATE products SET quantity = quantity - ? WHERE barcode = ? AND quantity >= ?";

                try (PreparedStatement select = connection.prepareStatement(lookup);
                     PreparedStatement debit = connection.prepareStatement(update)) {
                    for (Map.Entry<String, Integer> line : requested.entrySet()) {
                        String barcode = line.getKey();
                        Integer amount = line.getValue();
                        if (barcode == null || barcode.isBlank() || amount == null || amount <= 0) {
                            throw new IllegalArgumentException("Недопустимая позиция в корзине");
                        }

                        select.setString(1, barcode);
                        String name;
                        BigDecimal price;
                        int stock;
                        try (ResultSet rs = select.executeQuery()) {
                            if (!rs.next()) {
                                throw new IllegalArgumentException("Товар больше не найден: " + barcode);
                            }
                            name = rs.getString("name");
                            price = BigDecimal.valueOf(rs.getDouble("price"));
                            stock = rs.getInt("quantity");
                        }
                        if (amount > stock) {
                            throw new IllegalStateException("Недостаточно на складе: " + name);
                        }
                        debit.setInt(1, amount);
                        debit.setString(2, barcode);
                        debit.setInt(3, amount);
                        if (debit.executeUpdate() != 1) {
                            throw new IllegalStateException("Остаток товара изменился: " + name);
                        }
                        total = total.add(price.multiply(BigDecimal.valueOf(amount)));
                    }
                }
                connection.commit();
                return total.setScale(2, RoundingMode.HALF_UP);
            } catch (Exception failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                if (failure instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Не удалось завершить продажу", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Ошибка соединения с базой данных", failure);
        }
    }
}
