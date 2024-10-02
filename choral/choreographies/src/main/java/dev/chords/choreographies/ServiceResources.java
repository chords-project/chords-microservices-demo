package dev.chords.choreographies;

public class ServiceResources {
    public static final ServiceResources shared = new ServiceResources();

    private ServiceResources() {
    }

    public String frontendToCart = System.getenv().getOrDefault("CHORAL_FRONTEND_TO_CART", "0.0.0.0:5400");
    public String cartToFrontend = System.getenv().getOrDefault("CHORAL_CART_TO_FRONTEND", "0.0.0.0:5500");
    public String cartToProductcatalog = System.getenv().getOrDefault("CHORAL_CART_TO_PRODUCTCATALOG", "0.0.0.0:5600");
}
