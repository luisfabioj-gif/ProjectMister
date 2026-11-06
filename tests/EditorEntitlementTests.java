package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/** Real RSA signatures over Play-shaped receipts; no test entitlement ships with the app. */
public final class EditorEntitlementTests {
    private static final String APP = "com.projectmister.game";
    private static final String SKU = EditorEntitlement.PRODUCT_ID;
    private static final String TOKEN = "example-purchase-token";
    private static int checks;

    private static final class Store implements EditorEntitlement.Play {
        final List<Consumer<List<EditorEntitlement.Receipt>>> queries = new ArrayList<>();
        final List<Consumer<EditorEntitlement.State>> failures = new ArrayList<>();
        final List<Consumer<Boolean>> acknowledgements = new ArrayList<>();

        @Override public void query(Consumer<List<EditorEntitlement.Receipt>> success,
                                    Consumer<EditorEntitlement.State> failure) {
            queries.add(success);
            failures.add(failure);
        }
        @Override public void acknowledge(String token, Consumer<Boolean> completion) {
            check(TOKEN.equals(token), "only the validated purchase token is acknowledged");
            acknowledgements.add(completion);
        }
        void complete(EditorEntitlement.Receipt... receipts) {
            queries.get(queries.size() - 1).accept(List.of(receipts));
        }
        void ack(boolean success) { acknowledgements.get(acknowledgements.size() - 1).accept(success); }
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair key = generator.generateKeyPair();
        KeyPair wrongKey = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(key.getPublic().getEncoded());
        check(EditorEntitlement.readPublicKey(publicKey).equals(key.getPublic()), "loads the Play public RSA key");
        check(EditorEntitlement.readPublicKey("fake") == null, "rejects malformed public key");
        Store store = new Store();
        EditorEntitlement entitlement = new EditorEntitlement(APP, EditorEntitlement.readPublicKey(publicKey), store, () -> { });
        EditorEntitlement.Receipt purchased = receipt(key, APP, SKU, 0, false);
        EditorEntitlement.Receipt restored = receipt(key, APP, SKU, 0, true);
        EditorEntitlement.Receipt pending = receipt(key, APP, SKU, 4, false);

        entitlement.refresh();
        state(entitlement, EditorEntitlement.State.CHECKING);
        store.complete();
        state(entitlement, EditorEntitlement.State.LOCKED);
        entitlement.purchaseUpdated(List.of(pending));
        state(entitlement, EditorEntitlement.State.PENDING);
        check(store.acknowledgements.isEmpty(), "pending payments are never acknowledged");
        entitlement.purchaseUpdated(List.of(purchased));
        state(entitlement, EditorEntitlement.State.CHECKING);
        store.ack(false);
        state(entitlement, EditorEntitlement.State.ERROR);
        entitlement.refresh();
        store.complete(purchased);
        store.ack(true);
        state(entitlement, EditorEntitlement.State.OWNED);
        entitlement.refresh();
        state(entitlement, EditorEntitlement.State.CHECKING);
        store.complete();
        state(entitlement, EditorEntitlement.State.LOCKED);
        check(!entitlement.owned(), "removed/refunded purchase revokes access on next successful query");

        entitlement.refresh();
        Consumer<List<EditorEntitlement.Receipt>> stale = store.queries.get(store.queries.size() - 1);
        entitlement.refresh();
        store.complete();
        stale.accept(List.of(restored));
        state(entitlement, EditorEntitlement.State.LOCKED);

        EditorEntitlement.Receipt tampered = new EditorEntitlement.Receipt(
                restored.json.replace(TOKEN, "modified"), restored.signature, "modified", true, false, true);
        for (EditorEntitlement.Receipt invalid : List.of(
                tampered, receipt(wrongKey, APP, SKU, 0, true),
                receipt(key, "wrong.app", SKU, 0, true), receipt(key, APP, "wrong_product", 0, true),
                new EditorEntitlement.Receipt(restored.json, restored.signature, "wrong-token", true, false, true),
                new EditorEntitlement.Receipt(pending.json, pending.signature, TOKEN, true, false, false))) {
            entitlement.refresh();
            store.complete(invalid);
            state(entitlement, EditorEntitlement.State.ERROR);
        }
        check(store.acknowledgements.size() == 2, "no invalid receipt reaches acknowledgement");

        EditorEntitlement restarted = new EditorEntitlement(APP, key.getPublic(), store, () -> { });
        state(restarted, EditorEntitlement.State.CHECKING);
        restarted.refresh();
        store.complete(restored);
        state(restarted, EditorEntitlement.State.OWNED);
        check(store.acknowledgements.size() == 2, "already acknowledged purchase restores without another charge");
        restarted.disconnected();
        state(restarted, EditorEntitlement.State.UNAVAILABLE);
        restarted.refresh();
        store.failures.get(store.failures.size() - 1).accept(EditorEntitlement.State.UNAVAILABLE);
        state(restarted, EditorEntitlement.State.UNAVAILABLE);
        restarted.refresh();
        restarted.close();
        store.complete(restored);
        state(restarted, EditorEntitlement.State.UNAVAILABLE);

        int queries = store.queries.size();
        EditorEntitlement unconfigured = new EditorEntitlement(APP, null, store, () -> { });
        unconfigured.refresh();
        unconfigured.purchaseUpdated(List.of(restored));
        state(unconfigured, EditorEntitlement.State.UNAVAILABLE);
        check(store.queries.size() == queries, "build without Play configuration cannot ask for or grant ownership");
        System.out.println("PASS " + checks + " paid editor checks: locked by default, verified purchase, pending, acknowledgement, restore, revocation and forgery rejection");
    }

    private static EditorEntitlement.Receipt receipt(KeyPair key, String app, String product,
                                                      int rawState, boolean acknowledged) throws Exception {
        String json = "{\"packageName\":\"" + app + "\",\"productId\":\"" + product + "\",\"purchaseToken\":\"" + TOKEN
                + "\",\"purchaseState\":" + rawState + ",\"acknowledged\":" + acknowledged + "}";
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(key.getPrivate());
        signer.update(json.getBytes(StandardCharsets.UTF_8));
        return new EditorEntitlement.Receipt(json, Base64.getEncoder().encodeToString(signer.sign()),
                TOKEN, rawState == 0, rawState == 4, acknowledged);
    }

    private static void state(EditorEntitlement entitlement, EditorEntitlement.State expected) {
        check(entitlement.state() == expected, "expected " + expected + ", was " + entitlement.state());
        check(entitlement.owned() == (expected == EditorEntitlement.State.OWNED), "gate state matches " + expected);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
