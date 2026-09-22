package de.imageinvestigator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private Uri selected;
    private ImageView preview;
    private TextView detail;
    private LinearLayout actions;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 36);
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        scroll.addView(root);
        TextView title = new TextView(this);
        title.setText("Image Investigator"); title.setTextSize(26); title.setTextColor(Color.BLACK);
        root.addView(title);
        detail = new TextView(this);
        detail.setText("Wähle ein Foto oder teile es aus einer anderen App hierher. Jede Websuche wird einzeln gestartet.");
        detail.setTextSize(16); detail.setPadding(0, 18, 0, 18);
        root.addView(detail);
        addButton(root, "Bild auswählen (auch aus Google Fotos)", () -> {
            Intent pick = android.os.Build.VERSION.SDK_INT >= 33
                    ? new Intent(MediaStore.ACTION_PICK_IMAGES)
                    : new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            startActivityForResult(pick, 1);
        });
        addButton(root, "Ähnliche Bilder in ausgewählten Fotos finden", this::chooseComparisonPhotos);
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true); preview.setMaxHeight(650);
        root.addView(preview);
        actions = new LinearLayout(this); actions.setOrientation(LinearLayout.VERTICAL);
        root.addView(actions);
        setContentView(scroll);
        acceptIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); acceptIntent(intent);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 1 && result == RESULT_OK && data != null) select(data.getData());
        if (request == 2 && result == RESULT_OK && data != null) compareSelected(data);
    }
    @Override protected void onDestroy() {
        worker.shutdownNow(); super.onDestroy();
    }
    private void chooseComparisonPhotos() {
        if (selected == null) {
            Toast.makeText(this, "Zuerst ein Referenzbild auswählen", Toast.LENGTH_SHORT).show(); return;
        }
        Intent pick = android.os.Build.VERSION.SDK_INT >= 33
                ? new Intent(MediaStore.ACTION_PICK_IMAGES)
                : new Intent(Intent.ACTION_GET_CONTENT);
        pick.setType("image/*"); pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (android.os.Build.VERSION.SDK_INT >= 33) pick.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 50);
        startActivityForResult(pick, 2);
    }
    private void compareSelected(Intent data) {
        List<Uri> candidates = new ArrayList<>();
        if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++)
            candidates.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) candidates.add(data.getData());
        if (candidates.isEmpty()) return;
        final Uri reference = selected;
        detail.setText("Vergleiche " + candidates.size() + " ausgewählte Bilder …");
        worker.execute(() -> {
            List<Result> results = new ArrayList<>();
            int errors = 0;
            try {
                long expected = ImageHash.compute(getContentResolver(), reference);
                for (Uri uri : candidates) {
                    try {
                        int distance = Long.bitCount(expected ^ ImageHash.compute(getContentResolver(), uri));
                        results.add(new Result(uri, distance));
                    } catch (Exception ex) { errors++; }
                }
                results.sort(Comparator.comparingInt(r -> r.distance));
                final int failed = errors;
                runOnUiThread(() -> showComparison(results, failed));
            } catch (Exception ex) {
                runOnUiThread(() -> detail.setText("Referenzbild konnte nicht verglichen werden."));
            }
        });
    }
    private void showComparison(List<Result> results, int errors) {
        if (isFinishing() || isDestroyed()) return;
        actions.removeAllViews();
        detail.setText("Bildvergleich: " + results.size() + " geprüft, " + errors + " nicht lesbar. Kleinere Distanz bedeutet ähnlicher Bildinhalt; dies ist keine Gesichtssuche.");
        int count = Math.min(20, results.size());
        for (int i = 0; i < count; i++) {
            Result hit = results.get(i);
            addButton(actions, "Bild " + (i + 1) + " · Bilddistanz " + hit.distance + "/64", () -> select(hit.uri));
        }
    }
    private static final class Result {
        final Uri uri; final int distance;
        Result(Uri uri, int distance) { this.uri = uri; this.distance = distance; }
    }
    private void acceptIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = Intent.ACTION_SEND.equals(intent.getAction()) ? intent.getParcelableExtra(Intent.EXTRA_STREAM) : intent.getData();
        if (uri != null && "content".equals(uri.getScheme())) select(uri);
    }
    private void select(Uri uri) {
        if (uri == null) return;
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, opts);
            if (opts.outWidth <= 0 || opts.outHeight <= 0) throw new IllegalArgumentException("Kein lesbares Bild");
            selected = uri;
            // Decode a bounded preview; loading a full-resolution camera image into a View
            // can exhaust the heap before the user can start a search.
            BitmapFactory.Options thumbnailOptions = new BitmapFactory.Options();
            thumbnailOptions.inSampleSize = 1;
            int largestSide = Math.max(opts.outWidth, opts.outHeight);
            while (largestSide / thumbnailOptions.inSampleSize > 1200)
                thumbnailOptions.inSampleSize *= 2;
            try (InputStream thumbnailStream = getContentResolver().openInputStream(uri)) {
                Bitmap thumbnail = BitmapFactory.decodeStream(thumbnailStream, null, thumbnailOptions);
                if (thumbnail == null) throw new IllegalArgumentException("Kein lesbares Bild");
                preview.setImageBitmap(thumbnail);
            }
            detail.setText("Bild: " + opts.outWidth + " × " + opts.outHeight + " Pixel\nWähle einen Suchdienst. Bei dessen Upload-Schaltfläche setzt die App dieses Bild ein.");
            showActions();
        } catch (Exception ex) { Toast.makeText(this, "Bild konnte nicht geöffnet werden", Toast.LENGTH_LONG).show(); }
    }
    private void showActions() {
        actions.removeAllViews();
        addButton(actions, "Bild an Google/Lens teilen (falls angeboten)", () -> shareImage(null));
        addButton(actions, "Yandex Bilder · Bild einsetzen", () -> searchWith("yandex"));
        addButton(actions, "Bing Visual Search · Bild einsetzen", () -> searchWith("bing"));
        addButton(actions, "TinEye · Bild einsetzen", () -> searchWith("tineye"));
        addButton(actions, "SauceNAO · Bild einsetzen", () -> searchWith("saucenao"));
        addButton(actions, "Bild an eine andere App senden", () -> shareImage(null));
        TextView note = new TextView(this);
        note.setText("Auf der Suchseite Upload/Bild auswählen antippen. Das ausgewählte Foto wird dann automatisch eingesetzt; der jeweilige Dienst erhält es beim Absenden. Manche Anbieter sperren eingebettete Browser oder ändern ihre Upload-Seite. SafeSearch dort prüfen.");
        note.setPadding(0, 18, 0, 12); actions.addView(note);
        String[] sites = {"x.com", "instagram.com", "facebook.com", "tiktok.com", "pinterest.com"};
        for (String site : sites) addButton(actions, "Öffentliche Suche: " + site, () -> {
            EditText terms = new EditText(this);
            terms.setHint("Name, Nutzername oder Bildbeschreibung");
            new AlertDialog.Builder(this).setTitle("Auf " + site + " suchen")
                    .setView(terms).setNegativeButton("Abbrechen", null)
                    .setPositiveButton("Suchen", (dialog, which) -> {
                        String query = "site:" + site + " " + terms.getText().toString().trim();
                        try { openSite("https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")); }
                        catch (java.io.UnsupportedEncodingException ignored) { }
                    }).show();
        });
    }
    private void shareImage(String packageName) {
        if (selected == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/*"); send.putExtra(Intent.EXTRA_STREAM, selected);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (packageName != null) send.setPackage(packageName);
        try { startActivity(Intent.createChooser(send, "Bild suchen mit")); }
        catch (Exception ex) { Toast.makeText(this, "Keine passende App installiert", Toast.LENGTH_SHORT).show(); }
    }
    private void searchWith(String service) {
        if (selected == null) return;
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("service", service);
        intent.putExtra(Intent.EXTRA_STREAM, selected);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    private void openSite(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception ex) { Toast.makeText(this, "Kein Browser verfügbar", Toast.LENGTH_SHORT).show(); }
    }
    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this); button.setText(label);
        parent.addView(button); button.setOnClickListener(v -> action.run());
    }
}
