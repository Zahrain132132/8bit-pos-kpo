package by.zahrain.pos;

import by.zahrain.pos.database.DatabaseManager;
import by.zahrain.pos.database.ProductRepository;
import by.zahrain.pos.model.Product;

import java.util.List;

public class App {

    public static void main(String[] args) {
        System.out.println("================================");
        System.out.println("        8-BIT POS SYSTEM");
        System.out.println("================================");

        DatabaseManager databaseManager =
                new DatabaseManager("data/pos.db");

        databaseManager.initializeDatabase();

        ProductRepository repository =
                new ProductRepository(databaseManager);

        List<Product> products = repository.findAll();

        System.out.println("[SYSTEM] Database connected");
        System.out.println("[STATUS] Products loaded: " + products.size());
        System.out.println("--------------------------------");

        for (Product product : products) {
            System.out.println(product);
        }

        System.out.println("--------------------------------");
        System.out.println("[STATUS] Ready for operation");
        System.out.println("================================");
    }
}
