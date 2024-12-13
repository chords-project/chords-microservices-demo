package dev.chords.choreographies;

import java.io.Serializable;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
//import choral.channels.DiChannel;
//import choral.channels.SymChannel;
import choral.channels.AsyncDiChannel;
import choral.channels.AsyncSymChannel;
import choral.channels.Future;

public class ChorPlaceOrder@(Client, Cart, ProductCatalog, Currency, Payment, Shipping, Email) {

    private ClientService@Client clientSvc;
    private CartService@Cart cartSvc;
    private ProductCatalogService@ProductCatalog productCatalogSvc;
    private CurrencyService@Currency currencySvc;
    private ShippingService@Shipping shippingSvc;
    private PaymentService@Payment paymentSvc;
    private EmailService@Email emailSvc;

    private AsyncSymChannel@(Client, Currency)<Serializable> ch_clientCurrency;
    private AsyncSymChannel@(Client, Shipping)<Serializable> ch_clientShipping;
    private AsyncSymChannel@(Client, Payment)<Serializable> ch_clientPayment;
    private AsyncSymChannel@(Client, Email)<Serializable> ch_clientEmail;

    private AsyncDiChannel@(Client, Cart)<Serializable> ch_clientCart;

    private AsyncDiChannel@(Cart, ProductCatalog)<Serializable> ch_cartProduct;
    private AsyncDiChannel@(Cart, Shipping)<Serializable> ch_cartShipping;

    private AsyncDiChannel@(ProductCatalog, Currency)<Serializable> ch_productCurrency;

    private AsyncDiChannel@(Shipping, Currency)<Serializable> ch_shippingCurrency;

    public ChorPlaceOrder(
        ClientService@Client clientSvc,
        CartService@Cart cartSvc,
        ProductCatalogService@ProductCatalog productCatalogSvc,
        CurrencyService@Currency currencySvc,
        ShippingService@Shipping shippingSvc,
        PaymentService@Payment paymentSvc,
        EmailService@Email emailSvc,
        AsyncSymChannel@(Client, Currency)<Serializable> ch_clientCurrency,
        AsyncSymChannel@(Client, Shipping)<Serializable> ch_clientShipping,
        AsyncSymChannel@(Client, Payment)<Serializable> ch_clientPayment,
        AsyncSymChannel@(Client, Email)<Serializable> ch_clientEmail,
        AsyncDiChannel@(Client, Cart)<Serializable> ch_clientCart,
        AsyncDiChannel@(Cart, ProductCatalog)<Serializable> ch_cartProduct,
        AsyncDiChannel@(Cart, Shipping)<Serializable> ch_cartShipping,
        AsyncDiChannel@(ProductCatalog, Currency)<Serializable> ch_productCurrency,
        AsyncDiChannel@(Shipping, Currency)<Serializable> ch_shippingCurrency
    ) {
        this.clientSvc = clientSvc;
        this.cartSvc = cartSvc;
        this.productCatalogSvc = productCatalogSvc;
        this.currencySvc = currencySvc;
        this.shippingSvc = shippingSvc;
        this.paymentSvc = paymentSvc;
        this.emailSvc = emailSvc;

        this.ch_clientCart = ch_clientCart;
        this.ch_clientCurrency = ch_clientCurrency;
        this.ch_clientShipping = ch_clientShipping;
        this.ch_clientPayment = ch_clientPayment;
        this.ch_clientEmail = ch_clientEmail;

        this.ch_cartProduct = ch_cartProduct;
        this.ch_cartShipping = ch_cartShipping;

        this.ch_productCurrency = ch_productCurrency;

        this.ch_shippingCurrency = ch_shippingCurrency;
    }

    public OrderResult@Client placeOrder(ReqPlaceOrder@Client req) {
        System@Client.out.println("Starting place order choreography: user_id="@Client + req.user_id + " user_currency="@Client + req.user_currency);

        Future@Cart<SerializableString> userID_cart = ch_clientCart.<SerializableString>fcom(new SerializableString@Client(req.user_id));
        Future@Currency<SerializableString> userCurrency = ch_clientCurrency.<SerializableString>fcom(new SerializableString@Client(req.user_currency));
        Future@Shipping<Address> shippingAddress = ch_clientShipping.<Address>fcom(req.address);
        Future@Payment<CreditCardInfo> creditCartInfo = ch_clientPayment.<CreditCardInfo>fcom(req.credit_card);
        Future@Email<SerializableString> emailAddress = ch_clientEmail.<SerializableString>fcom(new SerializableString@Client(req.email));

        // Fetch user cart items
        Cart@Cart userCart = cartSvc.getCart(userID_cart.get().string);
        Future@ProductCatalog<Cart> cart_pc = ch_cartProduct.<Cart>fcom(userCart);
        Future@Shipping<Cart> cart_shipping = ch_cartShipping.<Cart>fcom(userCart);

        cartSvc.emptyCart(userID_cart.get().string);

        // Lookup cart item prices
        OrderItems@ProductCatalog cartPrices = productCatalogSvc.lookupCartPrices(cart_pc.get());
        Future@Currency<OrderItems> cartPrices_currency = ch_productCurrency.<OrderItems>fcom(cartPrices);

        // Convert currency of products
        OrderItems@Currency orderItems = currencySvc.convertProducts(cartPrices_currency.get(), userCurrency.get().string);
        Future@Client<OrderItems> orderItems_client = ch_clientCurrency.<OrderItems>fcom(orderItems);

        // Calculate shipping
        Money@Shipping shippingCost = shippingSvc.getQuote(shippingAddress.get(), cart_shipping.get());
        Future@Currency<Money> shippingCost_currency = ch_shippingCurrency.<Money>fcom(shippingCost);

        // Convert shipping currency
        Money@Currency convertedShippingCost = currencySvc.convert(shippingCost_currency.get(), userCurrency.get().string);
        Future@Client<Money> shippingCost_client = ch_clientCurrency.<Money>fcom(convertedShippingCost);

        // Ship order
        String@Shipping trackingID = shippingSvc.shipOrder(shippingAddress.get(), cart_shipping.get());
        Future@Client<SerializableString> trackingID_client = ch_clientShipping.<SerializableString>fcom(new SerializableString@Shipping(trackingID));

        // Charge card
        Money@Client totalPrice = clientSvc.totalPrice(orderItems_client.get(), shippingCost_client.get());
        Future@Payment<Money> chargePrice = ch_clientPayment.<Money>fcom(totalPrice);
        String@Payment txID = paymentSvc.charge(chargePrice.get(), creditCartInfo.get());

        // Send back charge confirmation
        Boolean@Client chargeSuccess = ch_clientPayment.<Boolean>fcom(true@Payment).get();

        // Bundle response at client
        UUID@Client orderID = UUID@Client.randomUUID();

        OrderResult@Client orderResult = new OrderResult@Client(
            orderID.toString(),
            trackingID_client.get().string,
            shippingCost_client.get(),
            req.address,
            orderItems_client.get()
        );

        // Send confirmation email
        Future@Email<OrderResult> orderResult_email = ch_clientEmail.<OrderResult>fcom(orderResult);
        emailSvc.sendOrderConfirmation(emailAddress.get().string, orderResult_email.get());
        Boolean@Client emailSent = ch_clientEmail.<Boolean>fcom(true@Email).get();

        return orderResult;
    }
}
