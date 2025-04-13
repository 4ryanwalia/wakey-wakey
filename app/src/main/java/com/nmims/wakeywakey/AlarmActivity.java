package com.nmims.wakeywakey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmActivity extends AppCompatActivity {

    private static final String TAG = "AlarmActivity";
    static final String ACTION_STOP_ALARM_SERVICE = "com.nmims.wakeywakey.ACTION_STOP_ALARM_SERVICE";
    public static final String EXTRA_ALARM_ID = "alarm_id";


    private TextView alarmTimeText;
    private Button stopAlarmButton;
    private ImageView alarmIcon;
    private int currentAlarmId = -1;

    private ActionReceiver stopActionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_WakeyWakey_AlarmScreen);
        setContentView(R.layout.activity_alarm);

        // Ensuring screen wakes up and stays on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        alarmTimeText = findViewById(R.id.alarmTimeText);
        stopAlarmButton = findViewById(R.id.stopAlarmButton);
        alarmIcon = findViewById(R.id.alarmIcon);

        // Get the alarm time and ID from Intent// Log this
        currentAlarmId = getIntent().getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);


        // Set button listener
        stopAlarmButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            stopAlarm();
        });

        // Register receiver
        registerStopReceiver();
        Log.d(TAG, "AlarmActivity created for alarm ID: " + currentAlarmId);
    }

    private void stopAlarm() {
        Log.d(TAG, "Stop button clicked for alarm ID: " + currentAlarmId);
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopService(stopIntent); // Stop the alarm service
        finish(); // Close the activity
    }

    private void registerStopReceiver() {
        stopActionReceiver = new ActionReceiver();
        IntentFilter filter = new IntentFilter(ACTION_STOP_ALARM_SERVICE);
        registerReceiver(stopActionReceiver, filter); // Register receiver

        Log.d(TAG, "Stop action receiver registered.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stopActionReceiver != null) {
            try {
                unregisterReceiver(stopActionReceiver);
                Log.d(TAG, "Stop action receiver unregistered.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered.");
            }
            stopActionReceiver = null;
        }
        Log.d(TAG, "AlarmActivity destroyed for alarm ID: " + currentAlarmId);
    }

    // Receiver class to handle notification actions
    public static class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_STOP_ALARM_SERVICE.equals(intent.getAction())) {
                int alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1); // Retrieve the alarm ID
                Log.d(TAG, "Received STOP action for alarm ID: " + alarmId);

                // Stop the service
                Intent stopServiceIntent = new Intent(context, AlarmService.class);
                context.stopService(stopServiceIntent);

                // Close notification drawer
                Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeDrawer);
            }
        }
    }
}
