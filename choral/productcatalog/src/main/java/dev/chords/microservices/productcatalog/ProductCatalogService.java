package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import dev.chords.choreographies.Cart;
import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItem;
import dev.chords.choreographies.OrderItems;
import dev.chords.choreographies.Product;
import dev.chords.choreographies.Products;
import hipstershop.Demo;
import hipstershop.ProductCatalogServiceGrpc;
import hipstershop.Demo.Empty;
import hipstershop.Demo.GetProductRequest;
import hipstershop.Demo.ListProductsResponse;
import hipstershop.Demo.SearchProductsRequest;
import hipstershop.ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ProductCatalogService implements dev.chords.choreographies.ProductCatalogService {

    protected ManagedChannel channel;
    protected ProductCatalogServiceBlockingStub connection;

    public ProductCatalogService(InetSocketAddress address) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        this.connection = ProductCatalogServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public Products listProducts() {
        System.out.println("[PRODUCT_CATALOG] List products");

        ListProductsResponse response = connection.listProducts(Empty.getDefaultInstance());

        List<Product> products = response.getProductsList().stream().map(prod -> new Product(
                prod.getId(),
                prod.getName(),
                prod.getDescription(),
                prod.getPicture(),
                new Money(prod.getPriceUsd().getCurrencyCode(),
                        (int) prod.getPriceUsd().getUnits(),
                        prod.getPriceUsd().getNanos()),
                prod.getCategoriesList())).toList();

        return new Products(new ArrayList<>(products));
    }

    @Override
    public Product getProduct(String productID) {
        System.out.println("[PRODUCT_CATALOG] Get product: productID=" + productID);

        GetProductRequest request = GetProductRequest.newBuilder().setId(productID).build();
        Demo.Product p = connection.getProduct(request);

        return convertProduct(p);
    }

    @Override
    public Products searchProducts(String query) {
        System.out.println("[PRODUCT_CATALOG] Search products: query=" + query);

        SearchProductsRequest request = SearchProductsRequest.newBuilder().setQuery(query).build();
        List<Product> products = connection
                .searchProducts(request)
                .getResultsList()
                .stream()
                .map(p -> convertProduct(p)).toList();

        return new Products(new ArrayList<>(products));
    }

    @Override
    public OrderItems lookupCartPrices(Cart cart) {
        System.out.println("[PRODUCT_CATALOG] Lookup cart prices");

        List<OrderItem> products = cart.items.stream()
                .map(item -> new OrderItem(item, getProduct(item.productID).priceUSD)).toList();

        return new OrderItems(new ArrayList<>(products));
    }

    private Product convertProduct(Demo.Product p) {
        Money money = new Money(
                p.getPriceUsd().getCurrencyCode(),
                (int) p.getPriceUsd().getUnits(),
                p.getPriceUsd().getNanos());

        List<String> categories = p.getCategoriesList();

        return new Product(p.getId(), p.getName(), p.getDescription(), p.getPicture(), money, categories);
    }

}
