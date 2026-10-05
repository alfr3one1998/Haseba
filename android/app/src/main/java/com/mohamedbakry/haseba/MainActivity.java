package com.mohamedbakry.haseba;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.util.Base64;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView webView;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(241,238,231));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setDomStorageEnabled(true);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new ShareBridge(), "AndroidShare");
        setContentView(webView);
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            webView.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            webView.requestApplyInsets();
        }
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }
    private class ShareBridge {
        @JavascriptInterface public void shareText(String text) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intent, "مشاركة عرض السعر"));
            });
        }
        @JavascriptInterface public void shareImage(String data, String caption) {
            if (data == null || !data.startsWith("data:image/png;base64,") || data.length() > 12000000) return;
            try {
                File directory = new File(getCacheDir(), "quotes");
                if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException("cache");
                File image = new File(directory, "cake-quote-" + System.currentTimeMillis() + ".png");
                byte[] bytes = Base64.decode(data.substring(data.indexOf(',') + 1), Base64.DEFAULT);
                try (FileOutputStream stream = new FileOutputStream(image)) { stream.write(bytes); }
                runOnUiThread(() -> {
                    android.net.Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", image);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/png");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.putExtra(Intent.EXTRA_TEXT, caption);
                    intent.setClipData(android.content.ClipData.newRawUri("عرض السعر", uri));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "مشاركة عرض السعر"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "تعذرت مشاركة الصورة", Toast.LENGTH_SHORT).show());
            }
        }
    }
    @Override protected void onDestroy() {
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}
