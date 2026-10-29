package io.github.hatake716.rocketboy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import java.io.InputStream;

/** Runs in the test APK's own UID, with no dependency on the game's Kotlin runtime. */
public class ScoreShareReceiverActivity extends Activity {
    @Override @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String summary;
        try {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            Bitmap bitmap;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(stream);
            }
            if (bitmap == null) throw new IllegalStateException("No image decoded");
            String size = bitmap.getWidth() + "x" + bitmap.getHeight();
            bitmap.recycle();
            summary = "IMAGE RECEIVED " + size + "\nUID " + android.os.Process.myUid() + "\n"
                + getIntent().getStringExtra(Intent.EXTRA_TEXT);
        } catch (Exception error) {
            summary = "SHARE FAILED " + error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        TextView label = new TextView(this);
        label.setText(summary); label.setTextSize(20f); label.setPadding(24, 320, 24, 24);
        setContentView(label);
    }
}
