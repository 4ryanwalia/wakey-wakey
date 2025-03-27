package com.nmims.wakeywakey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager; // Import AudioManager
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "ALARM_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 123;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager; // Added AudioManager instance
    private int originalAlarmVolume = -1; // To store original volume
    private int currentAlarmId = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); // Initialize AudioManager
        createNotificationChannel();
        Log.d(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        if (intent == null) {
            Log.e(TAG, "Intent is null, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        int ringtoneResId = intent.getIntExtra(AlarmReceiver.EXTRA_RINGTONE_RES_ID, -1);
        currentAlarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);

        if (ringtoneResId == -1 || currentAlarmId == -1) {
            Log.e(TAG, "Invalid data, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // --- Set Volume to Maximum ---
        if (audioManager != null) {
            try {
                // Store original volume before changing it
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0); // Flag 0 means no UI shown
                Log.i(TAG, "Alarm volume set to MAX (" + maxVolume + "). Original was: " + originalAlarmVolume);
            } catch (Exception e) {
                Log.e(TAG, "Error setting alarm volume", e);
                // Continue anyway, will play at current volume
            }
        } else {
            Log.w(TAG, "AudioManager is null, cannot set volume.");
        }
        // ----------------------------

        startForeground(NOTIFICATION_ID, createNotification(currentAlarmId));

        // Play Ringtone
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            Uri ringtoneUri = Uri.parse("android.resource://" + getPackageName() + "/" + ringtoneResId);
            // Set stream type for MediaPlayer BEFORE preparing
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setDataSource(this, ringtoneUri);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting playback.");
                mp.start(); // Start playing
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                stopSelf(); // Stop service on error
                return true; // Error handled
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting/preparing MediaPlayer", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Vibrate Pattern
        long[] pattern = {0, 500, 200, 500, 200, 1000}; // Pause, Vibrate, Pause, Vibrate...
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 repeats pattern
            } else {
                // Deprecated in API 26
                vibrator.vibrate(pattern, 0); // 0 repeats pattern
            }
            Log.d(TAG, "Vibrating");
        }

        return START_NOT_STICKY; // Don't restart automatically if killed
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        // Stop player and vibrator
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }

        // --- Restore Original Volume ---
        if (audioManager != null && originalAlarmVolume != -1) { // Check if original volume was stored
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0);
                Log.i(TAG, "Alarm volume restored to: " + originalAlarmVolume);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring alarm volume", e);
            }
        }
        // ----------------------------

        stopForeground(true); // Remove notification
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.alarm_notification_channel_name);
            String description = getString(R.string.alarm_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH; // Ensure high importance for alarms
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Sound & Vibration are handled manually by the service
            channel.setSound(null, null);
            channel.enableVibration(false);
            // Optional: For heads-up display even in DND (requires user permission grant for channel)
            // channel.setBypassDnd(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created.");
            } else {
                Log.e(TAG, "Failed to get NotificationManager.");
            }
        }
    }

    private Notification createNotification(int alarmId) {
        // Intent to open AlarmActivity when notification tapped
        Intent notificationIntent = new Intent(this, AlarmActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                alarmId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent for the "STOP" action button
        Intent stopSelfIntent = new Intent(this, AlarmActivity.ActionReceiver.class);
        stopSelfIntent.setAction(AlarmActivity.ACTION_STOP_ALARM_SERVICE);
        stopSelfIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
        PendingIntent stopSelfPendingIntent = PendingIntent.getBroadcast(this,
                alarmId, // Use alarmId for uniqueness
                stopSelfIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.alarm_ringing_title))
                .setContentText("Your alarm is ringing!")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for alarms
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Essential category for alarms
                .setContentIntent(pendingIntent) // Action on tap
                .setOngoing(true) // Makes it non-dismissible by swiping
                .addAction(R.drawable.ic_alarm_off, getString(R.string.stop_alarm), stopSelfPendingIntent) // Add STOP action
                .setSilent(true);

        return builder.build();
    }
}