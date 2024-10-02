package dev.chords.choreographies;

import java.util.List;

public interface ProductCatalogService@A {
    // RPCs
    Products@A listProducts();
    Product@A getProduct(String@A productID);
    Products@A searchProducts(String@A query);

    // Helper methods
    Products@A lookupCartProducts(Cart@A cart);
}