package dev.chords.choreographies;

public class ServiceResources {
    public static final ServiceResources shared = new ServiceResources();

    private ServiceResources() {
    }

    public String frontendToCart = System.getenv().getOrDefault("CHORAL_FRONTEND_TO_CART", "0.0.0.0:5401");
    public String frontendToCurrency = System.getenv().getOrDefault("CHORAL_FRONTEND_TO_CURRENCY", "0.0.0.0:5402");
    public String frontendToShipping = System.getenv().getOrDefault("CHORAL_FRONTEND_TO_SHIPPING", "0.0.0.0:5403");
    public String frontendToPayment = System.getenv().getOrDefault("CHORAL_FRONTEND_TO_PAYMENT", "0.0.0.0:5404");

    public String cartToFrontend = System.getenv().getOrDefault("CHORAL_CART_TO_FRONTEND", "0.0.0.0:5405");
    public String cartToProductcatalog = System.getenv().getOrDefault("CHORAL_CART_TO_PRODUCTCATALOG", "0.0.0.0:5406");
    public String cartToShipping = System.getenv().getOrDefault("CHORAL_CART_TO_SHIPPING", "0.0.0.0:5407");

    public String productcatalogToCurrency = System.getenv().getOrDefault("CHORAL_PRODUCTCATALOG_TO_CURRENCY",
            "0.0.0.0:5408");

    public String currencyToFrontend = System.getenv().getOrDefault("CHORAL_CURRENCY_TO_FRONTEND", "0.0.0.0:5409");
    public String shippingToFrontend = System.getenv().getOrDefault("CHORAL_SHIPPING_TO_FRONTEND", "0.0.0.0:5410");

}
