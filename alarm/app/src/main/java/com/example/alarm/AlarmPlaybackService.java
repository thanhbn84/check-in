package com.example.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.ArrayList;
import java.util.HashMap;

public class AlarmPlaybackService extends Service {
    static final int NOTIFICATION_ID = 700;
    final Handler handler = new Handler(Looper.getMainLooper());
    final HashMap<Integer, Playback> active = new HashMap<>();
    PowerManager.WakeLock wakeLock;
    Vibrator vibrator;
    boolean foreground = false;

    static boolean start(Context context, int id, String label, int durationSeconds, boolean soundEnabled, boolean vibrate) {
        try {
            Intent intent = new Intent(context, AlarmPlaybackService.class)
                    .putExtra("alarm_id", id)
                    .putExtra("alarm_label", label)
                    .putExtra("duration_seconds", durationSeconds)
                    .putExtra("sound_enabled", soundEnabled)
                    .putExtra("vibrate", vibrate);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
            return true;
        } catch (RuntimeException error) {
            android.util.Log.e("AlarmPlayback", "Unable to start playback service", error);
            return false;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            int id = intent == null ? -1 : intent.getIntExtra("alarm_id", -1);
            String label = intent == null ? "Báo thức" : intent.getStringExtra("alarm_label");
            int durationSeconds = intent == null ? 10 : intent.getIntExtra("duration_seconds", 10);
            boolean soundEnabled = intent == null || intent.getBooleanExtra("sound_enabled", true);
            boolean vibrate = intent != null && intent.getBooleanExtra("vibrate", false);
            if (active.containsKey(id)) finish(active.get(id));
            Playback playback = new Playback(id, label == null || label.isEmpty() ? "Báo thức" : label,
                    Math.max(10, Math.min(30, durationSeconds)), soundEnabled, vibrate);
            active.put(id, playback);
            if (wakeLock == null) {
                PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":AlarmPlayback");
                wakeLock.setReferenceCounted(false);
            }
            if (!wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L);
            showNotification(playback);
            if (playback.soundEnabled) play(playback);
            else startStopCountdown(playback);
            return START_NOT_STICKY;
        } catch (RuntimeException error) {
            android.util.Log.e("AlarmPlayback", "Playback service failed", error);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
    }

    void showNotification(Playback playback) {
        String channelId = playback.soundEnabled ? "alarm_playback" : "alarm_silent";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, "Alarm", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
        Intent stop = new Intent(this, StopReceiver.class);
        PendingIntent stopPending = PendingIntent.getBroadcast(this, 700, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, channelId) : new Notification.Builder(this);
        Icon stopIcon = Icon.createWithResource(this, android.R.drawable.ic_media_pause);
        String status = playback.soundEnabled ? "Đang reo" : "Im lặng · chỉ thông báo";
        if (playback.vibrate) status += " · có rung";
        Notification notification = builder.setSmallIcon(R.drawable.ic_alarm_notification)
                .setContentTitle(playback.label)
                .setContentText(status + " · " + playback.durationSeconds + " giây · Vuốt để tắt")
                .setCategory(Notification.CATEGORY_ALARM)
                .setDeleteIntent(stopPending)
                .addAction(new Notification.Action.Builder(stopIcon, "TẮT", stopPending).build())
                .build();
        if (!foreground) {
            startForeground(NOTIFICATION_ID, notification);
            foreground = true;
        } else {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
        }
    }

    void play(Playback playback) {
        if (!active.containsKey(playback.id)) return;
        try {
            android.net.Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (uri == null) {
                if (playback.vibrate) startStopCountdown(playback);
                else finish(playback);
                return;
            }
            MediaPlayer player = new MediaPlayer();
            playback.player = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.setOnErrorListener((failed, what, extra) -> { finish(playback); return true; });
            player.prepare();
            AudioDeviceInfo headset = AlarmAudio.preferConnectedHeadset(this, player);
            if (headset != null) player.setVolume(0f, 0f);
            player.start();
            if (headset == null) startStopCountdown(playback);
            else waitForHeadsetRoute(playback, headset, android.os.SystemClock.elapsedRealtime() + 2500L);
        } catch (Exception error) {
            android.util.Log.e("AlarmPlayback", "Unable to play alarm sound", error);
            finish(playback);
        }
    }

    void waitForHeadsetRoute(Playback playback, AudioDeviceInfo target, long deadline) {
        playback.routeTask = new Runnable() {
            @Override public void run() {
                if (playback.player == null || !active.containsKey(playback.id)) return;
                AudioDeviceInfo routed = playback.player.getRoutedDevice();
                if (routed != null && routed.getId() == target.getId()) {
                    android.util.Log.i("AlarmPlayback", "Headset route is active before alarm audio is unmuted");
                    playback.player.setVolume(1f, 1f);
                    playback.routeTask = null;
                    startStopCountdown(playback);
                } else if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    android.util.Log.w("AlarmPlayback", "Headset route did not become active before timeout");
                    playback.player.setVolume(1f, 1f);
                    playback.routeTask = null;
                    startStopCountdown(playback);
                } else {
                    handler.postDelayed(this, 80L);
                }
            }
        };
        handler.post(playback.routeTask);
    }

    void startStopCountdown(Playback playback) {
        startVibration(playback);
        playback.stopTask = () -> finish(playback);
        handler.postDelayed(playback.stopTask, playback.durationSeconds * 1000L);
    }

    void startVibration(Playback playback) {
        if (!playback.vibrate) return;
        try {
            if (vibrator == null) vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            long[] pattern = {0, 450, 350};
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            else vibrator.vibrate(pattern, 0);
            playback.vibrationActive = true;
        } catch (RuntimeException error) {
            android.util.Log.w("AlarmPlayback", "Unable to vibrate for alarm", error);
        }
    }

    void finish(Playback playback) {
        if (playback.stopTask != null) handler.removeCallbacks(playback.stopTask);
        if (playback.routeTask != null) handler.removeCallbacks(playback.routeTask);
        if (playback.player != null) {
            try { if (playback.player.isPlaying()) playback.player.stop(); } catch (Exception ignored) { }
            try { playback.player.release(); } catch (Exception ignored) { }
            playback.player = null;
        }
        active.remove(playback.id);
        boolean anotherVibration = false;
        for (Playback running : active.values()) if (running.vibrationActive) anotherVibration = true;
        if (!anotherVibration && vibrator != null) vibrator.cancel();
        if (active.isEmpty()) {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            if (foreground) {
                if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
                else stopForeground(true);
                foreground = false;
            }
            stopSelf();
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        for (Playback playback : new ArrayList<>(active.values())) {
            if (playback.player != null) {
                try { playback.player.release(); } catch (Exception ignored) { }
            }
        }
        active.clear();
        if (vibrator != null) vibrator.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    static class Playback {
        final int id, durationSeconds;
        final String label;
        final boolean soundEnabled, vibrate;
        boolean vibrationActive;
        MediaPlayer player;
        Runnable stopTask;
        Runnable routeTask;
        Playback(int id, String label, int durationSeconds, boolean soundEnabled, boolean vibrate) { this.id = id; this.label = label; this.durationSeconds = durationSeconds; this.soundEnabled = soundEnabled; this.vibrate = vibrate; }
    }
}
