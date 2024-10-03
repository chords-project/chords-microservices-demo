package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;

import dev.chords.choreographies.Cart;
import dev.chords.choreographies.Product;
import dev.chords.choreographies.Products;

public class ProductCatalogService implements dev.chords.choreographies.ProductCatalogService {

    public ProductCatalogService(InetSocketAddress address) {
    }

    @Override
    public Products listProducts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listProducts'");
    }

    @Override
    public Product getProduct(String productID) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProduct'");
    }

    @Override
    public Products searchProducts(String query) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchProducts'");
    }

    @Override
    public Products lookupCartProducts(Cart cart) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'lookupCartProducts'");
    }

}
