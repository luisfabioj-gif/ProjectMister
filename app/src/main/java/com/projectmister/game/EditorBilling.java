package com.projectmister.game;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Google Play adapter. All UI and entitlement transitions run on the main thread. */
final class EditorBilling implements EditorEntitlement.Play {
    static final String BUY_OPTION = "editor-unlock";
    private static final int OK = BillingClient.BillingResponseCode.OK;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BillingClient client;
    private final EditorEntitlement entitlement;
    private final Runnable changed;
    private ProductDetails product;
    private ProductDetails.OneTimePurchaseOfferDetails offer;
    private Runnable waiting;
    private Consumer<EditorEntitlement.State> connectionFailed;
    private boolean connecting, closed, checkout, loadingOffer;
    private long catalogGeneration;
    private String note = "";

    EditorBilling(Context context, PublicKey publicKey, Runnable changed) {
        this.changed = changed;
        entitlement = new EditorEntitlement(context.getPackageName(), publicKey, this, changed);
        client = publicKey == null ? null : BillingClient.newBuilder(context.getApplicationContext())
                .setListener((result, purchases) -> main.post(() -> purchaseResult(result, purchases)))
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
    }

    boolean owned() { return entitlement.owned(); }
    EditorEntitlement.State state() { return entitlement.state(); }
    boolean configured() { return client != null; }
    boolean checkingOut() { return checkout; }
    boolean loadingOffer() { return loadingOffer; }
    String price() { return offer == null ? "" : offer.getFormattedPrice(); }
    String note() { return note; }

    void refresh() {
        if (closed || client == null) return;
        checkout = false;
        note = "";
        refreshOwnership();
    }

    private void refreshOwnership() {
        product = null;
        offer = null;
        loadingOffer = false;
        catalogGeneration++;
        entitlement.refresh();
    }

    void buy(Activity activity) {
        if (closed || checkout || state() != EditorEntitlement.State.LOCKED
                || product == null || offer == null || price().isEmpty()) return;
        checkout = true;
        note = "";
        changed.run();
        BillingFlowParams.ProductDetailsParams selection = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offer.getOfferToken())
                .build();
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(selection)).build());
        if (result.getResponseCode() != OK) purchaseResult(result, null);
    }

    private void purchaseResult(BillingResult result, List<Purchase> purchases) {
        if (closed) return;
        checkout = false;
        int code = result.getResponseCode();
        if (code == OK) {
            note = "";
            entitlement.purchaseUpdated(receipts(purchases));
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            refresh();
        } else {
            note = code == BillingClient.BillingResponseCode.USER_CANCELED
                    ? "Purchase cancelled." : "Google Play could not complete the purchase. Try again.";
            refreshOwnership();
        }
    }

    @Override public void query(Consumer<List<EditorEntitlement.Receipt>> success,
                                Consumer<EditorEntitlement.State> failure) {
        if (client == null || closed) { failure.accept(EditorEntitlement.State.UNAVAILABLE); return; }
        Runnable query = () -> {
            long request = catalogGeneration;
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP).build(), (result, purchases) -> main.post(() -> {
                if (closed || request != catalogGeneration) return;
                if (result.getResponseCode() == OK) {
                    success.accept(receipts(purchases));
                    if (state() == EditorEntitlement.State.LOCKED) loadOffer(request);
                } else {
                    failure.accept(failureState(result));
                }
            }));
        };
        if (client.isReady()) {
            query.run();
            return;
        }
        waiting = query;
        connectionFailed = failure;
        if (connecting) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                main.post(() -> {
                    if (closed) return;
                    connecting = false;
                    Runnable ready = waiting;
                    Consumer<EditorEntitlement.State> failed = connectionFailed;
                    waiting = null;
                    connectionFailed = null;
                    if (result.getResponseCode() == OK) {
                        if (ready != null) ready.run();
                    } else if (failed != null) failed.accept(failureState(result));
                });
            }

            @Override public void onBillingServiceDisconnected() {
                main.post(() -> {
                    if (closed) return;
                    connecting = false;
                    waiting = null;
                    connectionFailed = null;
                    checkout = false;
                    loadingOffer = false;
                    catalogGeneration++;
                    product = null;
                    offer = null;
                    entitlement.disconnected();
                });
            }
        });
    }

    private void loadOffer(long request) {
        loadingOffer = true;
        changed.run();
        QueryProductDetailsParams.Product editor = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(EditorEntitlement.PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build();
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(editor)).build(), (result, details) -> main.post(() -> {
            if (closed || request != catalogGeneration) return;
            loadingOffer = false;
            if (result.getResponseCode() == OK) {
                for (ProductDetails entry : details.getProductDetailsList()) {
                    if (!EditorEntitlement.PRODUCT_ID.equals(entry.getProductId())) continue;
                    List<ProductDetails.OneTimePurchaseOfferDetails> options = entry.getOneTimePurchaseOfferDetailsList();
                    if (options == null) continue;
                    for (ProductDetails.OneTimePurchaseOfferDetails option : options) {
                        if (BUY_OPTION.equals(option.getPurchaseOptionId()) && option.getOfferId() == null
                                && option.getRentalDetails() == null && option.getPreorderDetails() == null
                                && option.getPriceAmountMicros() > 0 && option.getOfferToken() != null
                                && !option.getOfferToken().isEmpty()) {
                            product = entry;
                            offer = option;
                            break;
                        }
                    }
                }
            }
            changed.run();
        }));
    }

    @Override public void acknowledge(String token, Consumer<Boolean> completion) {
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
                result -> main.post(() -> { if (!closed) completion.accept(result.getResponseCode() == OK); }));
    }

    private static List<EditorEntitlement.Receipt> receipts(List<Purchase> purchases) {
        List<EditorEntitlement.Receipt> result = new ArrayList<>();
        if (purchases == null) return result;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(EditorEntitlement.PRODUCT_ID)) continue;
            result.add(new EditorEntitlement.Receipt(purchase.getOriginalJson(), purchase.getSignature(),
                    purchase.getPurchaseToken(), purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED,
                    purchase.getPurchaseState() == Purchase.PurchaseState.PENDING, purchase.isAcknowledged()));
        }
        return result;
    }

    private static EditorEntitlement.State failureState(BillingResult result) {
        int code = result.getResponseCode();
        return code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                || code == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
                || code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                ? EditorEntitlement.State.UNAVAILABLE : EditorEntitlement.State.ERROR;
    }

    void close() {
        closed = true;
        waiting = null;
        connectionFailed = null;
        entitlement.close();
        if (client != null) client.endConnection();
        main.removeCallbacksAndMessages(null);
    }
}
