package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import dev.chords.choreographies.Cart;
import dev.chords.choreographies.OrderItem;
import dev.chords.choreographies.Product;

public class ProductCatalogService implements dev.chords.choreographies.ProductCatalogService {

    public ProductCatalogService(InetSocketAddress address) {
    }

    @Override
    public List<Product> listProducts() {
        return new ArrayList<>();
    }

    @Override
    public Product getProduct(String productID) {
        return null;
    }

    @Override
    public List<Product> searchProducts(String query) {
        return new ArrayList<>();
    }

    @Override
    public List<OrderItem> prepOrderItems(Cart cart) {
        return new ArrayList<>();
    }

}
