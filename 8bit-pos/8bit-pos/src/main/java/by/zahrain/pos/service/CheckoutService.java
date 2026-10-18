package by.zahrain.pos.service;

import by.zahrain.pos.model.Cart;
import by.zahrain.pos.model.CartItem;
import by.zahrain.pos.model.Product;

public class CheckoutService {

    public double checkout(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }

        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        // Сначала проверяем всю покупку.
        // На этом этапе состояние склада не изменяется.
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            if (item.getQuantity() > product.getQuantity()) {
                throw new IllegalStateException(
                        "Not enough stock for product: " + product.getName()
                );
            }
        }

        double total = cart.getTotal();

        // Только после успешной проверки всех позиций
        // действительно списываем товары.
        for (CartItem item : cart.getItems()) {
            item.getProduct().reduceStock(item.getQuantity());
        }

        cart.clear();

        return total;
    }
}
