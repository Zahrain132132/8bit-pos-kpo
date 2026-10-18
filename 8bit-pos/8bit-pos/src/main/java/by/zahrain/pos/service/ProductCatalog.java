package by.zahrain.pos.service;

import by.zahrain.pos.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductCatalog {

    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        if (findByBarcode(product.getBarcode()) != null) {
            throw new IllegalArgumentException(
                    "Product with this barcode already exists"
            );
        }

        products.add(product);
    }

    public Product findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }

        for (Product product : products) {
            if (product.getBarcode().equals(barcode)) {
                return product;
            }
        }

        return null;
    }

    public List<Product> getAllProducts() {
        return Collections.unmodifiableList(products);
    }

    public int size() {
        return products.size();
    }
}
