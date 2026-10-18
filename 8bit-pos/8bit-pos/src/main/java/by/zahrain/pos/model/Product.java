package by.zahrain.pos.model;

public class Product {

    private final String barcode;
    private String name;
    private double price;
    private int quantity;

    public Product(String barcode, String name, double price, int quantity) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (amount > quantity) {
            throw new IllegalArgumentException("Not enough products in stock");
        }

        quantity -= amount;
    }

    @Override
    public String toString() {
        return name + " [" + barcode + "] - " + price + " BYN"
                + " | stock: " + quantity;
    }
}
