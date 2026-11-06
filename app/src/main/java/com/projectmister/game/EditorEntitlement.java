package com.projectmister.game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/** Session-only entitlement. The only source of ownership is a verified Play purchase. */
final class EditorEntitlement {
    static final String PRODUCT_ID = "boss_xi_game_editor";

    enum State { CHECKING, LOCKED, PENDING, OWNED, UNAVAILABLE, ERROR }

    static final class Receipt {
        final String json, signature, token;
        final boolean purchased, pending, acknowledged;

        Receipt(String json, String signature, String token, boolean purchased, boolean pending, boolean acknowledged) {
            this.json = json;
            this.signature = signature;
            this.token = token;
            this.purchased = purchased;
            this.pending = pending;
            this.acknowledged = acknowledged;
        }
    }

    interface Play {
        void query(Consumer<List<Receipt>> success, Consumer<State> failure);
        void acknowledge(String token, Consumer<Boolean> completion);
    }

    private final String packageName;
    private final PublicKey publicKey;
    private final Play play;
    private final Runnable changed;
    private State state;
    private long generation;
    private boolean closed;

    EditorEntitlement(String packageName, PublicKey publicKey, Play play, Runnable changed) {
        this.packageName = packageName;
        this.publicKey = publicKey;
        this.play = play;
        this.changed = changed;
        state = publicKey == null ? State.UNAVAILABLE : State.CHECKING;
    }

    State state() { return state; }
    boolean owned() { return !closed && state == State.OWNED; }

    void refresh() {
        if (closed || publicKey == null) return;
        long request = ++generation;
        setState(State.CHECKING);
        play.query(receipts -> handle(request, receipts), failure -> {
            if (current(request)) setState(failure == State.UNAVAILABLE ? State.UNAVAILABLE : State.ERROR);
        });
    }

    void purchaseUpdated(List<Receipt> receipts) {
        if (closed || publicKey == null) return;
        if (receipts == null || receipts.isEmpty()) {
            refresh();
            return;
        }
        handle(++generation, receipts);
    }

    private void handle(long request, List<Receipt> receipts) {
        if (!current(request)) return;
        Receipt unacknowledged = null;
        boolean pending = false, invalid = false;
        for (Receipt receipt : receipts) {
            if (!verify(receipt)) { invalid = true; continue; }
            if (receipt.purchased && receipt.acknowledged) { setState(State.OWNED); return; }
            if (receipt.purchased) unacknowledged = receipt;
            else if (receipt.pending) pending = true;
        }
        if (unacknowledged != null) {
            setState(State.CHECKING);
            play.acknowledge(unacknowledged.token, success -> {
                if (current(request)) setState(success ? State.OWNED : State.ERROR);
            });
        } else {
            setState(pending ? State.PENDING : invalid ? State.ERROR : State.LOCKED);
        }
    }

    void disconnected() {
        if (!closed && publicKey != null) { generation++; setState(State.UNAVAILABLE); }
    }

    void close() { closed = true; generation++; state = State.UNAVAILABLE; }

    private boolean current(long request) { return !closed && request == generation; }

    private void setState(State next) {
        state = next;
        changed.run();
    }

    private boolean verify(Receipt receipt) {
        if (receipt == null || receipt.json == null || receipt.signature == null || receipt.token == null
                || receipt.token.isEmpty() || receipt.purchased == receipt.pending) return false;
        try {
            // Google Play signs the original purchase JSON with this signature scheme.
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(publicKey);
            signature.update(receipt.json.getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getDecoder().decode(receipt.signature))) return false;
            JSONObject signed = new JSONObject(receipt.json);
            if (!packageName.equals(signed.optString("packageName"))
                    || !receipt.token.equals(signed.optString("token", signed.optString("purchaseToken")))) return false;
            if (receipt.purchased && signed.optInt("purchaseState", 0) != 0) return false;
            JSONArray products = signed.optJSONArray("productIds");
            return products != null
                    ? products.length() == 1 && PRODUCT_ID.equals(products.optString(0))
                    : PRODUCT_ID.equals(signed.optString("productId"));
        } catch (GeneralSecurityException | org.json.JSONException | IllegalArgumentException e) {
            return false;
        }
    }

    static PublicKey readPublicKey(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(value.replaceAll("\\s", ""))));
            return key instanceof RSAPublicKey && ((RSAPublicKey) key).getModulus().bitLength() >= 2048 ? key : null;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }
}
