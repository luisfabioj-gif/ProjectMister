package com.projectmister.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/** Small offline portrait library. Assignment is persisted, independent of club and ageing. */
final class PortraitManager {
    interface Loaded { void accept(Bitmap bitmap); }
    private final Context context;
    private final SharedPreferences prefs;
    private final LruCache<Integer, Bitmap> cache = new LruCache<Integer, Bitmap>(4 * 1024 * 1024) {
        protected int sizeOf(Integer key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    PortraitManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences("project_mister_portraits_v1", Context.MODE_PRIVATE);
    }
    void load(int identity, int age, boolean player, boolean female, Loaded loaded) {
        String key = (player ? "player_" : female ? "female_" : "staff_") + identity;
        int fallback = female ? 36 + Math.floorMod(identity,6) : player ? (age <= 24 ? Math.floorMod(identity, 18) : 18 + Math.floorMod(identity, 12))
                : 30 + Math.floorMod(identity, 6);
        int index = prefs.getInt(key, fallback);
        if (index < 0 || index >= 42) index = fallback;
        prefs.edit().putInt(key, index).apply();
        final int id = index;
        Bitmap hit = cache.get(id);
        if (hit != null) { loaded.accept(hit); return; }
        if (closed) return;
        worker.execute(() -> {
            Bitmap bitmap = null;
            try (InputStream in = context.getAssets().open(String.format(java.util.Locale.ROOT, "portraits/person_%02d.webp", id))) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeStream(in, null, options);
                if (bitmap != null) cache.put(id, bitmap);
            } catch (Exception e) { android.util.Log.w("BOSSXI", "Optional portrait unavailable", e); }
            final Bitmap result = bitmap;
            if (!closed) main.post(() -> { if (!closed) loaded.accept(result); });
        });
    }
    void close() { closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null); cache.evictAll(); }
}
