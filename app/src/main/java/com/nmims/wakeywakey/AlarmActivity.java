package com.nmims.wakeywakey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants; // Import HapticFeedbackConstants
import android.view.WindowManager;
// Removed animation imports if using MotionLayout
// import android.view.animation.Animation;
// import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AlarmActivity extends AppCompatActivity {

    private static final String TAG = "AlarmActivity";
    static final String ACTION_STOP_ALARM_SERVICE = "com.nmims.wakeywakey.ACTION_STOP_ALARM_SERVICE";

    private TextView alarmTimeText;
    private Button stopAlarmButton;
    private ImageView alarmIcon;
    private int currentAlarmId = -1;

    private ActionReceiver stopActionReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the specific theme BEFORE setting content view
        setTheme(R.style.Theme_WakeyWakey_AlarmScreen);

        setContentView(R.layout.activity_alarm);

        // --- Show over lock screen & turn screen on ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // --- Find Views ---
        alarmTimeText = findViewById(R.id.alarmTimeText);
        stopAlarmButton = findViewById(R.id.stopAlarmButton);
        alarmIcon = findViewById(R.id.alarmIcon);

        // --- Get Data ---
        String timeString = getIntent().getStringExtra(AlarmReceiver.EXTRA_ALARM_TIME_STR);
        currentAlarmId = getIntent().getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);

        if (timeString != null) {
            alarmTimeText.setText(timeString);
        }

        // --- Start Animations (handled by MotionLayout autoTransition or programmatically if needed) ---
        // Removed AnimationUtils code

        // --- Set Listeners ---
        stopAlarmButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); // Added haptic feedback
            stopAlarm();
        });

        // --- Register Receiver ---
        registerStopReceiver();

        Log.d(TAG, "AlarmActivity created for alarm ID: " + currentAlarmId);
    }


    private void stopAlarm() {
        Log.d(TAG, "Stop button clicked for alarm ID: " + currentAlarmId);
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopService(stopIntent);
        finish(); // Finish the activity
    }


    private void registerStopReceiver() {
        stopActionReceiver = new ActionReceiver();
        IntentFilter filter = new IntentFilter(ACTION_STOP_ALARM_SERVICE);
        ContextCompat.registerReceiver(this, stopActionReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "Stop action receiver registered.");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        if (stopActionReceiver != null) {
            try {
                unregisterReceiver(stopActionReceiver);
                Log.d(TAG, "Stop action receiver unregistered.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver already unregistered or never registered?");
            }
            stopActionReceiver = null;
        }
        Log.d(TAG, "AlarmActivity destroyed for alarm ID: " + currentAlarmId);
    }


    // Inner class receiver for Notification Action
    public static class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_STOP_ALARM_SERVICE.equals(intent.getAction())) {
                int alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1);
                Log.d(TAG, "Received STOP action from notification for alarm ID: " + alarmId);

                Intent stopServiceIntent = new Intent(context, AlarmService.class);
                context.stopService(stopServiceIntent);

                // Close the notification drawer
                Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeDrawer);

                // Optional: If AlarmActivity might still be visible, send a broadcast
                // to tell it to finish itself. Requires AlarmActivity to register
                // another receiver for that specific action.
            }
        }
    }
}