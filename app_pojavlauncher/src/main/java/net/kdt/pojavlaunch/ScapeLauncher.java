package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;

public class ScapeLauncher extends BaseActivity {

    private TextView settings;
    private Button playHD;
    private Button playSD;
    private static final int FILE_SELECT_CODE_JSON = 0;
    private static final int FILE_SELECT_CODE_ZIP = 1;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ProgressLayout mProgressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_launcher);
        settings = findViewById(R.id.settings);
        playHD = findViewById(R.id.playHD);
        playSD = findViewById(R.id.playSD);
        mProgressLayout = findViewById(R.id.progress_layout);

        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(mProgressLayout);

        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);

        playHD.setOnClickListener(view -> {
            if(!runtimeReady()) return;
            Intent intent = new Intent(ScapeLauncher.this, MainActivity.class);
            startActivity(intent);
        });

        playSD.setOnClickListener(view -> {
            if(!runtimeReady()) return;
            Intent intent = new Intent(ScapeLauncher.this, JavaGUILauncherActivity.class);
            startActivity(intent);
        });
        settings.setOnClickListener(view -> showBottomDialog());
        checkForUpdate();
    }

    /** FastLane: asks GitHub for the newest release and offers to download it if it's newer than this app. */
    private void checkForUpdate() {
        final int mine = BuildConfig.FL_BUILD;
        if (mine <= 0) return; // a local build, not from GitHub
        new Thread(() -> {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(
                        "https://api.github.com/repos/teamslayergames-tech/fastlane-mobile/releases/latest").openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                String body;
                try (java.io.InputStream in = c.getInputStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    body = out.toString("UTF-8");
                }
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"tag_name\"\\s*:\\s*\"build-(\\d+)\"").matcher(body);
                if (!m.find()) return;
                final int latest = Integer.parseInt(m.group(1));
                if (latest <= mine) return;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    new android.app.AlertDialog.Builder(ScapeLauncher.this)
                            .setTitle("FastLane update available")
                            .setMessage("A newer version of the FastLane app is out (build " + latest + "; you have build " + mine + ").\n\nDownload it now? Install it over this one - your settings are kept.")
                            .setPositiveButton("Download", (d, w) -> {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
                                            "https://github.com/teamslayergames-tech/fastlane-mobile/releases/latest/download/FastLane.apk")));
                                } catch (Exception ignored) { }
                            })
                            .setNegativeButton("Later", null)
                            .show();
                });
            } catch (Exception ignored) { /* offline or GitHub unreachable: just skip */ }
        }, "FastLane-update-check").start();
    }


    private boolean runtimeReady(){
        if(mProgressLayout.hasProcesses()){
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showBottomDialog() {
        MyDialogFragment dialog = new MyDialogFragment();
        dialog.show(getSupportFragmentManager(), "tag");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
    }
}