package com.zx.keep.accounts;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.zx.keep.accounts.modules.CameraModule;
import com.zx.keep.accounts.security.AuditLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String ASSET_BASE = "https://appassets.localhost/";
    private static final long BACK_PRESS_INTERVAL = 2000;
    private static final int FILE_CHOOSER_REQUEST = 5001;
    private static final int PERM_REQ_CODE = 9001;
    private static final int MANAGE_STORAGE_REQ = 9002;
    private static final String TAG = "MainActivity";
    private CameraModule cameraModule;
    private ValueCallback<Uri[]> filePathCallback;
    private JSBridge jsBridge;
    private long lastBackPressTime = 0;
    private String pendingCallbackId;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private PermissionManager permissionManager;
    private WebView webView;
    private boolean permissionsResolved = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setupImmersiveStatusBar();
        this.jsBridge = new JSBridge();
        PermissionManager pm = new PermissionManager(this);
        this.permissionManager = pm;
        pm.setActivity(this);
        CameraModule cm = new CameraModule(this);
        this.cameraModule = cm;
        cm.setActivity(this);
        ModuleRegistry.registerAll(this);
        initPermissionLauncher();
        createWebView();
        loadWebContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.webView != null) this.webView.onResume();
        if (!permissionsResolved && Build.VERSION.SDK_INT >= 23) {
            permissionsResolved = true;
            resolvePermissions();
        }
    }

    private void resolvePermissions() {
        List<String> ungranted = this.permissionManager.getUngrantedPermissions();

        ArrayList<String> normalPerms = new ArrayList<>();
        for (String p : ungranted) {
            if (!"android.permission.MANAGE_EXTERNAL_STORAGE".equals(p)) {
                normalPerms.add(p);
            }
        }

        boolean needManage = false;
        if (Build.VERSION.SDK_INT >= 30) {
            needManage = !Environment.isExternalStorageManager();
        }

        if (!normalPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    normalPerms.toArray(new String[0]), PERM_REQ_CODE);
        } else if (needManage) {
            openManageStorageSettings();
        }
    }

    private void openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_REQ);
            } catch (Exception e) {
                AuditLogger.logError("Main", "manageStorage: " + e.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERM_REQ_CODE) return;

        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            openManageStorageSettings();
        }
    }

    private void setupImmersiveStatusBar() {
        Window window = getWindow();
        // Clear translucent status flag, enable drawing behind system bars
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // Transparent status bar — WebView content renders behind it
        window.setStatusBarColor(0x00000000);
        // Dark icons on light/transparent status bar
        if (Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void createWebView() {
        this.webView = new WebView(this);

        // Enable GPU hardware acceleration for WebView rendering
        this.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        setContentView(this.webView, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(0);

        // Performance: enable caching for CDN resources
        settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);

        WebView.setWebContentsDebuggingEnabled(true);
        this.webView.addJavascriptInterface(this.jsBridge, "AndroidBridge");
        this.webView.addJavascriptInterface(new NativeBridge(this), "NativeBridge");
        this.webView.addJavascriptInterface(new WebAppBridge(this), "zxui");
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith("bridge://")) return false;
                handleBridgeUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(ASSET_BASE)) {
                    String assetPath = url.substring(28);
                    int queryIdx = assetPath.indexOf('?');
                    if (queryIdx >= 0) {
                        assetPath = assetPath.substring(0, queryIdx);
                    }
                    try {
                        InputStream is = MainActivity.this.getAssets().open(assetPath);
                        String mime = guessMimeType(assetPath);
                        return new WebResourceResponse(mime,
                                (mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json"))
                                        ? "UTF-8" : null, is);
                    } catch (IOException e) {
                        return new WebResourceResponse("text/plain", "UTF-8", 404,
                                "Not Found", null, null);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectBridgeReady();
                injectStatusBarHeight();
            }
        });
        this.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebConsole", "[" + consoleMessage.messageLevel() + "] " +
                        consoleMessage.message() + " (" + consoleMessage.sourceId() + ":" +
                        consoleMessage.lineNumber() + ")");
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = callback;
                try {
                    MainActivity.this.startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
            }
        });
    }

    private void loadWebContent() {
        this.webView.loadUrl("file:///android_asset/index.html");
    }

    private String guessMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".wasm")) return "application/wasm";
        return "application/octet-stream";
    }

    private void injectBridgeReady() {
        this.webView.evaluateJavascript(
                "javascript:if (typeof window.onBridgeReady === 'function') {" +
                        "  window.onBridgeReady();}" +
                        "if (typeof window.dispatchEvent === 'function') {" +
                        "  window.dispatchEvent(new Event('bridgeReady'));}", null);
    }

    private void injectStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int height = id > 0 ? getResources().getDimensionPixelSize(id) : 0;
        this.webView.evaluateJavascript(
                "javascript:window.__statusBarHeight=" + height +
                        ";if(typeof window.__onStatusBarReady==='function'){" +
                        "window.__onStatusBarReady(" + height + ");}", null);
    }

    private void handleBridgeUrl(String url) {
        Log.d(TAG, "Bridge URL: " + url);
    }

    public class WebAppBridge {
        private final WeakReference<MainActivity> activityRef;

        public WebAppBridge(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        private MainActivity getActivity() {
            MainActivity act = this.activityRef.get();
            if (act != null) return act;
            throw new IllegalStateException("Activity destroyed");
        }

        @JavascriptInterface
        public String 获取外部存储目录() {
            return "/storage/emulated/0/";
        }

        @JavascriptInterface
        public boolean 判断指定文件(String path) {
            if (path == null) return false;
            try {
                return new File(path).exists();
            } catch (Exception e) {
                Log.e(TAG, "fileExists error", e);
                return false;
            }
        }

        @JavascriptInterface
        public boolean 保存指定文件(String path, String data) {
            if (path == null) return false;
            try {
                File file = new File(path);
                // data is null/empty → create directory
                if (data == null || data.isEmpty()) {
                    return file.mkdirs();
                }
                // strip data URI prefix if present
                int idx = data.indexOf("base64,");
                if (idx >= 0) {
                    data = data.substring(idx + 7);
                }
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "saveFile error", e);
                return false;
            }
        }

        @JavascriptInterface
        public void 执行JAVA(String code) {
            if (code != null && code.contains("killProcess")) {
                final MainActivity activity = getActivity();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.restartApp();
                    }
                });
            } else {
                Log.w(TAG, "WebAppBridge executeJAVA blocked: " + code);
            }
        }
    }

    public class NativeBridge {
        private final WeakReference<MainActivity> activityRef;

        public NativeBridge(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        private MainActivity getActivity() {
            MainActivity act = this.activityRef.get();
            if (act != null) return act;
            throw new IllegalStateException("Activity destroyed");
        }

        @JavascriptInterface
        public void takePhoto(final String callbackId) {
            final MainActivity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.pendingCallbackId = callbackId;
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    File dir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    if (dir != null && !dir.exists()) dir.mkdirs();
                    try {
                        File photoFile = File.createTempFile("JPEG_" + timestamp + "_", ".jpg", dir);
                        Uri uri = FileProvider.getUriForFile(activity,
                                activity.getPackageName() + ".fileprovider", photoFile);
                        CameraModule.setPendingPhotoFile(photoFile);
                        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                        intent.putExtra("output", uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            activity.startActivityForResult(intent, 2001);
                        } catch (Exception e) {
                            CameraModule.setPendingPhotoFile(null);
                            activity.callbackToJS(callbackId, activity.jsonError("Failed to launch camera: " + e.getMessage()));
                        }
                    } catch (Exception e) {
                        activity.callbackToJS(callbackId, activity.jsonError("Failed to create photo file: " + e.getMessage()));
                    }
                }
            });
        }

        @JavascriptInterface
        public void pickFromGallery(final String callbackId) {
            final MainActivity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.pendingCallbackId = callbackId;
                    try {
                        activity.startActivityForResult(
                                new Intent("android.intent.action.PICK",
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 2002);
                    } catch (Exception e) {
                        activity.callbackToJS(callbackId, activity.jsonError("Failed to open gallery: " + e.getMessage()));
                    }
                }
            });
        }

        @JavascriptInterface
        public void requestPermissions(final String callbackId, final String permissionsJson) {
            final MainActivity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.pendingCallbackId = callbackId;
                    try {
                        JSONArray arr = new JSONArray(permissionsJson);
                        int len = arr.length();
                        String[] permissions = new String[len];
                        for (int i = 0; i < len; i++) {
                            permissions[i] = arr.getString(i);
                        }
                        for (int i = 0; i < len; i++) {
                            if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(permissions[i])) {
                                if (Build.VERSION.SDK_INT >= 30) {
                                    Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                    activity.startActivity(intent);
                                    activity.callbackToJS(callbackId, "{\"success\":true,\"info\":\"manage_storage_settings_opened\"}");
                                }
                                return;
                            }
                        }
                        java.util.ArrayList<String> list = new java.util.ArrayList<>();
                        for (int i = 0; i < len; i++) {
                            if (!"android.permission.MANAGE_EXTERNAL_STORAGE".equals(permissions[i])) {
                                list.add(permissions[i]);
                            }
                        }
                        if (!list.isEmpty()) {
                            activity.permissionLauncher.launch(list.toArray(new String[0]));
                        } else {
                            activity.callbackToJS(callbackId, activity.jsonError("No permissions to request"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "requestPermissions error", e);
                        activity.callbackToJS(callbackId, activity.jsonError(e.getMessage()));
                    }
                }
            });
        }

        @JavascriptInterface
        public void openAppSettings(final String callbackId) {
            final MainActivity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                        if (callbackId != null && !callbackId.isEmpty()) {
                            activity.callbackToJS(callbackId, "{\"success\":true,\"info\":\"settings_opened\"}");
                        }
                    } catch (Exception e) {
                        if (callbackId != null && !callbackId.isEmpty()) {
                            activity.callbackToJS(callbackId, activity.jsonError(e.getMessage()));
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_STORAGE_REQ) return;

        String cbid = this.pendingCallbackId;
        this.pendingCallbackId = null;

        if (requestCode == 2001) {
            File photoFile = CameraModule.getPendingPhotoFile();
            if (resultCode == RESULT_OK && photoFile != null && photoFile.exists()) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("type", "photo");
                    result.put("path", photoFile.getAbsolutePath());
                    callbackToJS(cbid, result.toString());
                } catch (Exception e) {
                    callbackToJS(cbid, jsonError(e.getMessage()));
                }
            } else {
                callbackToJS(cbid, jsonError("Photo cancelled or failed"));
            }
            CameraModule.setPendingPhotoFile(null);
            return;
        }

        if (requestCode == 2002) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("type", "gallery");
                    result.put("uri", data.getData().toString());
                    callbackToJS(cbid, result.toString());
                } catch (Exception e) {
                    callbackToJS(cbid, jsonError(e.getMessage()));
                }
            } else {
                callbackToJS(cbid, jsonError("No image selected"));
            }
            return;
        }

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (this.filePathCallback != null) {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    this.filePathCallback.onReceiveValue(new Uri[]{data.getData()});
                } else {
                    this.filePathCallback.onReceiveValue(null);
                }
                this.filePathCallback = null;
            }
        }
    }

    private void initPermissionLauncher() {
        this.permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> result) {
                        String cbid = pendingCallbackId;
                        pendingCallbackId = null;
                        try {
                            JSONObject json = new JSONObject();
                            json.put("success", true);
                            for (String perm : result.keySet()) {
                                Boolean granted = result.get(perm);
                                String status = (granted != null && granted) ? "granted" : "denied";
                                json.put(perm, status);
                                AuditLogger.logPermission("request", perm, status);
                            }
                            callbackToJS(cbid, json.toString());
                        } catch (Exception e) {
                            AuditLogger.logError("Main", "permissionLauncher: " + e.getMessage());
                            callbackToJS(cbid, jsonError(e.getMessage()));
                        }
                    }
                });
    }

    private void callbackToJS(String callbackId, String json) {
        if (this.webView == null || callbackId == null || callbackId.isEmpty()) return;
        String escaped = json.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", "\\n").replace("\r", "\\r");
        this.webView.evaluateJavascript(
                "javascript:(function(){" +
                        "if(typeof window.__nativeCallbacks==='object'&&" +
                        "typeof window.__nativeCallbacks['" + callbackId + "']==='function'){" +
                        "window.__nativeCallbacks['" + callbackId + "']('" + escaped + "');" +
                        "delete window.__nativeCallbacks['" + callbackId + "'];}})()", null);
    }

    private String jsonError(String message) {
        return "{\"success\":false,\"error\":\"" +
                message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.webView != null) this.webView.onPause();
    }

    @Override
    public void onBackPressed() {
        if (this.webView != null && this.webView.canGoBack()) {
            this.webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastBackPressTime < BACK_PRESS_INTERVAL) {
            restartApp();
        } else {
            this.lastBackPressTime = now;
            Toast.makeText(this, "再按一次重启应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.webView != null) {
            this.webView.loadUrl("about:blank");
            this.webView.clearHistory();
            this.webView.destroy();
            this.webView = null;
        }
        super.onDestroy();
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }
}
