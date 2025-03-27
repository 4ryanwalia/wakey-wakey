package com.nmims.wakeywakey; // Changed package

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;


// No local imports needed as Alarm, AlarmReceiver, MainActivity are in the same package now
// import com.nmims.wakeywakey.model.Alarm; removed
// import com.nmims.wakeywakey.receiver.AlarmReceiver; removed
// import com.nmims.wakeywakey.ui.MainActivity; removed


import java.util.Calendar;
import java.util.Locale;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    public void schedule(Alarm alarm) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "Exact alarm permission not granted. Cannot schedule alarm ID: " + alarm.getId());
            // Ensure toast runs on UI thread
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, R.string.exact_alarm_permission_message, Toast.LENGTH_LONG).show());
            requestExactAlarmPermission(); // Guide user again
            return;
        }

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule alarm.");
            return;
        }

        // Ensure the trigger time is correctly set/updated *before* scheduling
        long triggerTimeMillis = calculateTriggerTime(alarm.getHour(), alarm.getMinute());
        alarm.setTimeInMillis(triggerTimeMillis); // Update the alarm object itself

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getId());
        intent.putExtra(AlarmReceiver.EXTRA_RINGTONE_RES_ID, alarm.getRingtoneResourceId());
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_TIME_STR, String.format(Locale.getDefault(), "%02d:%02d", alarm.getHour(), alarm.getMinute()));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerTimeMillis, getMainActivityPendingIntent());

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            Log.i(TAG, "Alarm scheduled: ID=" + alarm.getId() + " at " + triggerTimeMillis);

            String toastMsg = String.format(context.getString(R.string.alarm_set_toast), alarm.getHour(), alarm.getMinute());
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show());

        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm.", se);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, R.string.exact_alarm_permission_message, Toast.LENGTH_LONG).show());
            requestExactAlarmPermission();
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: ID=" + alarm.getId(), e);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Error scheduling alarm.", Toast.LENGTH_SHORT).show());
        }
    }

    public void cancel(Alarm alarm) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot cancel alarm.");
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel(); // Also cancel the PendingIntent itself
        Log.i(TAG, "Alarm cancelled: ID=" + alarm.getId());
    }

    private PendingIntent getMainActivityPendingIntent() {
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context,
                -1, // Generic request code for this intent is okay
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Helper to calculate next trigger time
    public static long calculateTriggerTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the calculated time is in the past, add one day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }
}