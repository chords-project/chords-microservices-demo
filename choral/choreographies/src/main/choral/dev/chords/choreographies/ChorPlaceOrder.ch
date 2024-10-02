package dev.chords.choreographies;

import java.io.Serializable;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import choral.channels.DiChannel;

public class ChorPlaceOrder@(Client, Cart, ProductCatalog) {

    private CartService@Cart cartSvc;
    private ProductCatalogService@ProductCatalog productCatalogSvc;

    public DiChannel@(Client, Cart)<Serializable> ch_clientCart;
    public DiChannel@(Cart, ProductCatalog)<Serializable> ch_cartProduct;

    public ChorPlaceOrder(
        CartService@Cart cartSvc,
        DiChannel@(Client, Cart)<Serializable> ch_clientCart,
        DiChannel@(Cart, ProductCatalog)<Serializable> ch_cartProduct
    ) {
        this.cartSvc = cartSvc;

        this.ch_clientCart = ch_clientCart;
        this.ch_cartProduct = ch_cartProduct;
    }

    public void placeOrder(ReqPlaceOrder@Client req) {
        System@Client.out.println("Starting place order choreography: user_id="@Client + req.userID + " user_currency="@Client + req.userCurrency);

        UUID@Client orderID = UUID@Client.randomUUID();

        // Go method: prepareOrderItemsAndShippingQuoteFromCart
        String@Cart userID_cart = ch_clientCart.<SerializableString>com(new SerializableString@Client(req.userID)).string;
        Cart@Cart userCart = cartSvc.getCart(userID_cart);
        
        Cart@ProductCatalog cart_pc = ch_cartProduct.<Cart>com(userCart);
        List@ProductCatalog<OrderItem> orderItems = productCatalogSvc.prepOrderItems(cart_pc);
    }
}