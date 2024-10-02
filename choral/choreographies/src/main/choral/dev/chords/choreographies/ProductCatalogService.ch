package dev.chords.choreographies;

import java.util.List;

public interface ProductCatalogService@A {
    // RPCs
    List@A<Product> listProducts();
    Product@A getProduct(String@A productID);
    List@A<Product> searchProducts(String@A query);

    // Helper methods
    List@A<OrderItem> prepOrderItems(Cart@A cart);
}