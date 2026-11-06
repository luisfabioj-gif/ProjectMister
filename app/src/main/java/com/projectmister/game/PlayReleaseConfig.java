package com.projectmister.game;

import android.content.Context;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Properties;

/** Public distribution information only. Never put an upload key or credentials here. */
final class PlayReleaseConfig {
    final PublicKey billingKey;
    final String privacyUrl, supportEmail;

    private PlayReleaseConfig(Properties values) {
        billingKey = EditorEntitlement.readPublicKey(values.getProperty("billing_public_key", ""));
        privacyUrl = httpsUrl(values.getProperty("privacy_url", "").trim());
        String email = values.getProperty("support_email", "").trim();
        supportEmail = email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ? email : "";
    }

    static PlayReleaseConfig load(Context context) {
        Properties values = new Properties();
        try (InputStream in = context.getAssets().open("play-release.properties")) {
            values.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException | IllegalArgumentException e) {
            values.clear();
        }
        return new PlayReleaseConfig(values);
    }

    private static String httpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null) return value;
        } catch (IllegalArgumentException ignored) { }
        return "";
    }
}
