package com.nmims.wakeywakey;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NextAlarmWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "NextAlarmWidget";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static Runnable updateRunnable;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(TAG, "onUpdate called for IDs: " + java.util.Arrays.toString(appWidgetIds));

        // Initialize the Runnable with the context
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (context != null) {
                    updateAllWidgets(context);
                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        };

        // Start the updating process
        handler.post(updateRunnable);
    }

    private Alarm findNextAlarm(Context context) {
        Log.d(TAG, "Searching for the next alarm...");
        Alarm nextAlarm = null;
        long minTriggerTime = Long.MAX_VALUE;

        try {
            AlarmDatabase db = AlarmDatabase.getDatabase(context);
            List<Alarm> enabledAlarms = db.alarmDao().getAllEnabledAlarmsSync();

            if (enabledAlarms != null && !enabledAlarms.isEmpty()) {
                long now = System.currentTimeMillis();

                for (Alarm alarm : enabledAlarms) {
                    // Create an instance of AlarmScheduler to call the non-static method
                    AlarmScheduler alarmScheduler = new AlarmScheduler(context);
                    long triggerTime = alarmScheduler.calculateTriggerTime(alarm.getHour(), alarm.getMinute());

                    if (triggerTime > now && triggerTime < minTriggerTime) {
                        minTriggerTime = triggerTime;
                        nextAlarm = alarm;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving alarms", e);
        }

        Log.d(TAG, "Found alarm: " + (nextAlarm != null ? nextAlarm.getId() : "None"));
        return nextAlarm;
    }

    private void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Alarm nextAlarm) {
        Log.d(TAG, "Updating widgets...");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        long currentTime = System.currentTimeMillis();

        String alarmText;
        String countdownText;

        if (nextAlarm != null && nextAlarm.isEnabled()) {
            // Create an instance of AlarmScheduler to call calculateTriggerTime
            AlarmScheduler alarmScheduler = new AlarmScheduler(context);
            long alarmTime = alarmScheduler.calculateTriggerTime(nextAlarm.getHour(), nextAlarm.getMinute());
            alarmText = "Next Alarm: " + timeFormat.format(alarmTime);
            countdownText = getCountdownText(alarmTime - currentTime);
        } else {
            alarmText = "No Alarm Set";
            countdownText = "";
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_alarm);
            views.setTextViewText(R.id.widget_alarm_time_text_simple, alarmText);
            views.setTextViewText(R.id.widget_countdown_text, countdownText);

            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root_simple, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, views);
        }
        Log.d(TAG, "Widget update complete.");
    }

    private String getCountdownText(long millis) {
        if (millis <= 0) return "Alarm Ringing Now!";
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "Time Left: %02dh %02dm %02ds", hours, minutes, seconds);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, NextAlarmWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        if (widgetIds != null && widgetIds.length > 0) {
            Log.d(TAG, "Updating all widgets...");
            AlarmDatabase.databaseWriteExecutor.execute(() -> {
                NextAlarmWidgetProvider provider = new NextAlarmWidgetProvider();
                Alarm nextAlarm = provider.findNextAlarm(context);
                provider.updateWidgets(context, appWidgetManager, widgetIds, nextAlarm);
            });
        } else {
            Log.d(TAG, "No widget instances found.");
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        handler.post(updateRunnable);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive: " + intent.getAction());
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            updateAllWidgets(context);
        }
    }
}
