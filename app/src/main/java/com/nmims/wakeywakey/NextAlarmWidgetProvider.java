package com.nmims.wakeywakey;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AppWidgetProvider for displaying the next alarm time.
 */
public class NextAlarmWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "NextAlarmWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(TAG, "onUpdate called for IDs: " + java.util.Arrays.toString(appWidgetIds));

        // Run background task to find next alarm
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            Alarm nextAlarm = findNextAlarm(context);
            updateWidgets(context, appWidgetManager, appWidgetIds, nextAlarm);
        });
    }

    /**
     * Finds the next enabled alarm from the database.
     */
    private Alarm findNextAlarm(Context context) {
        Log.d(TAG, "findNextAlarm: Searching for next alarm...");
        Alarm nextAlarm = null;
        long minTriggerTime = Long.MAX_VALUE;

        try {
            AlarmDatabase db = AlarmDatabase.getDatabase(context);
            List<Alarm> enabledAlarms = db.alarmDao().getAllEnabledAlarmsSync();

            if (enabledAlarms != null && !enabledAlarms.isEmpty()) {
                long now = System.currentTimeMillis();

                for (Alarm alarm : enabledAlarms) {
                    long triggerTime = AlarmScheduler.calculateTriggerTime(alarm.getHour(), alarm.getMinute());

                    if (triggerTime > now && triggerTime < minTriggerTime) {
                        minTriggerTime = triggerTime;
                        nextAlarm = alarm;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "findNextAlarm: Error retrieving alarms", e);
        }

        Log.d(TAG, "findNextAlarm: Found alarm: " + (nextAlarm != null ? nextAlarm.getId() : "None"));
        return nextAlarm;
    }

    /**
     * Updates all widgets with the next alarm time.
     */
    private void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Alarm nextAlarm) {
        Log.d(TAG, "updateWidgets: Updating widgets...");

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String alarmText = (nextAlarm != null && nextAlarm.isEnabled()) ?
                timeFormat.format(Calendar.getInstance().getTime()) :
                "No Alarm Set";

        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_alarm);
            views.setTextViewText(R.id.widget_alarm_time_text_simple, alarmText);

            // Set click action to open the main app
            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root_simple, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
        }

        Log.d(TAG, "updateWidgets: Widget update complete.");
    }

    /**
     * Handles system broadcast events for the widget.
     */
    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, NextAlarmWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        if (widgetIds != null && widgetIds.length > 0) {
            Log.d(TAG, "updateAllWidgets: Updating widgets...");
            AlarmDatabase.databaseWriteExecutor.execute(() -> {
                NextAlarmWidgetProvider provider = new NextAlarmWidgetProvider();
                Alarm nextAlarm = provider.findNextAlarm(context);
                provider.updateWidgets(context, appWidgetManager, widgetIds, nextAlarm);
            });
        } else {
            Log.d(TAG, "updateAllWidgets: No widget instances found.");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive: " + intent.getAction());
    }
}
