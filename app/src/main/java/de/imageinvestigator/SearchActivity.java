package de.imageinvestigator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Hands the selected image to a provider's normal web upload control. */
public class SearchActivity extends Activity {
    private WebView browser;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Uri image = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        String service = getIntent().getStringExtra("service");
        String url = serviceUrl(service);
        if (image == null || !"content".equals(image.getScheme()) || url == null) {
            finish(); return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView instructions = new TextView(this);
        instructions.setPadding(20, 16, 20, 16);
        instructions.setText("Auf der Seite Upload oder Bild auswählen antippen. Image Investigator setzt dein Foto ein. Erst der Suchdienst verarbeitet den Upload.");
        root.addView(instructions);
        Button external = new Button(this);
        external.setText("Im normalen Browser öffnen (Bild dort manuell wählen)");
        external.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        root.addView(external);
        browser = new WebView(this);
        root.addView(browser, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowContentAccess(true);
        browser.setWebViewClient(new WebViewClient());
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                                       FileChooserParams params) {
                // Called when the person taps the provider's file upload field.
                // Keep all unrelated file inputs from receiving the selected image.
                String[] types = params.getAcceptTypes();
                if (types != null) for (String type : types) {
                    if (type != null && !type.isEmpty() && !type.startsWith("image/") && !type.equals(".jpg")
                            && !type.equals(".jpeg") && !type.equals(".png") && !type.equals(".webp")) {
                        callback.onReceiveValue(null);
                        return true;
                    }
                }
                callback.onReceiveValue(new Uri[]{image});
                return true;
            }
        });
        browser.loadUrl(url);
    }

    private static String serviceUrl(String service) {
        if ("yandex".equals(service)) return "https://yandex.com/images/";
        if ("bing".equals(service)) return "https://www.bing.com/visualsearch";
        if ("tineye".equals(service)) return "https://tineye.com/";
        if ("saucenao".equals(service)) return "https://saucenao.com/";
        return null;
    }

    @Override public void onBackPressed() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (browser != null) browser.destroy();
        super.onDestroy();
    }
}
