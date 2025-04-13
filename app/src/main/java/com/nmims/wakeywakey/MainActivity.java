package com.nmims.wakeywakey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        AlarmAdapter.OnToggleAlarmListener,
        AlarmAdapter.OnItemClickListener,
        AlarmAdapter.OnSwipeToDeleteListener {

    private static final String TAG = "MainActivity";
    private RecyclerView alarmsRecyclerView;
    private AlarmAdapter alarmAdapter;
    private FloatingActionButton fabAddAlarm;
    private TextView emptyView;
    private AlarmScheduler alarmScheduler;
    private AlarmDatabase alarmDatabase;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
        fabAddAlarm = findViewById(R.id.fabAddAlarm);
        emptyView = findViewById(R.id.emptyView);

        alarmScheduler = new AlarmScheduler(this);
        alarmDatabase = AlarmDatabase.getDatabase(this);

        setupRecyclerView();
        enableSwipeToDelete();

        fabAddAlarm.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            checkPermissionsAndAddAlarm();
        });

        checkUserLoggedIn();
        observeAlarms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasNotificationPermission() && !alarmScheduler.hasExactAlarmPermission()) {
            Log.w(TAG, "Resumed: Exact alarm permission still not granted.");
        }
    }

    private void checkUserLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        auth.signOut();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void checkPermissionsAndAddAlarm() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
        } else if (!alarmScheduler.hasExactAlarmPermission()) {
            showPermissionRationale("Exact alarm permission is required.", true);
        } else {
            startActivity(new Intent(MainActivity.this, SetAlarmActivity.class));
        }
    }
    private void showPermissionRationale(String message, boolean isForExactAlarm) {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    if (isForExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_SHORT).show()
                )
                .show();
    }


    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // For older versions, notifications are granted by default.
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }


    private void setupRecyclerView() {
        alarmAdapter = new AlarmAdapter(this, this, this); // Fixed constructor
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmsRecyclerView.setAdapter(alarmAdapter);
    }

    private void observeAlarms() {
        alarmDatabase.alarmDao().getAllAlarms().observe(this, alarms -> {
            alarmAdapter.submitList(alarms);
            boolean isEmpty = (alarms == null || alarms.isEmpty());
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            alarmsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                List<Alarm> currentList = alarmAdapter.getCurrentList();
                if (position >= 0 && position < currentList.size()) {
                    Alarm deletedAlarm = currentList.get(position);
                    onSwipeToDelete(deletedAlarm);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(alarmsRecyclerView);
    }

    @Override
    public void onToggle(Alarm alarm) {
        alarm.setEnabled(!alarm.isEnabled());
        AlarmDatabase.databaseWriteExecutor.execute(() ->
                alarmDatabase.alarmDao().update(alarm));
        runOnUiThread(() -> alarmAdapter.notifyDataSetChanged());
    }

    @Override
    public void onItemClick(Alarm alarm) {
        Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
        intent.putExtra("ALARM_ID", alarm.getId());
        startActivity(intent);
    }



    public void onSwipeToDelete(Alarm alarm) {
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            alarmDatabase.alarmDao().delete(alarm);

            runOnUiThread(() -> {
                Snackbar.make(alarmsRecyclerView, "Alarm deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            AlarmDatabase.databaseWriteExecutor.execute(() -> {
                                alarmDatabase.alarmDao().insert(alarm);
                            });
                        })
                        .show();
            });
        });
    }

    @Override
    public void onSwipe(Alarm alarm) {
        onSwipeToDelete(alarm); // Fixed missing method implementation
    }
}

