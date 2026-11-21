package net.kdt.pojavlaunch.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

/**
 * FastLane: keeps the game running while the app is in the background.
 * Without a foreground service, newer Android versions freeze a backgrounded app after a few seconds;
 * the game then stops talking to the server and gets logged out. Started when a game screen opens,
 * stopped when it closes. Shows a quiet "FastLane is running" notification (tap it to go back to the game).
 */
public class GameKeepAliveService extends Service {
    private static final int NOTIFICATION_ID = 2009;

    public static void start(Context ctx, Class<?> gameActivity) {
        try {
            Intent i = new Intent(ctx, GameKeepAliveService.class);
            i.putExtra("activity", gameActivity.getName());
            ContextCompat.startForegroundService(ctx, i);
        } catch (Throwable ignored) { }
    }

    public static void stop(Context ctx) {
        try { ctx.stopService(new Intent(ctx, GameKeepAliveService.class)); } catch (Throwable ignored) { }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Tools.buildNotificationChannel(getApplicationContext());
        PendingIntent open = null;
        try {
            String cls = intent != null ? intent.getStringExtra("activity") : null;
            if (cls != null) {
                Intent back = new Intent(this, Class.forName(cls));
                back.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                open = PendingIntent.getActivity(this, 0, back, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            }
        } catch (Throwable ignored) { }
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("FastLane is running")
                .setContentText("You stay logged in while the app is in the background.")
                .setSmallIcon(R.drawable.notif_icon)
                .setOngoing(true)
                .setNotificationSilent();
        if (open != null) b.setContentIntent(open);
        startForeground(NOTIFICATION_ID, b.build());
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // the app was swiped away: quit the game completely (don't leave it running hidden)
        stopForeground(true);
        stopSelf();
        super.onTaskRemoved(rootIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
