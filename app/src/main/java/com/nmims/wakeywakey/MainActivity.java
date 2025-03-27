package com.nmims.wakeywakey;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants; // Import HapticFeedbackConstants
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import Nullable
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat; // Import for transitions
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat; // Import for transitions
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections; // Import Collections
import java.util.List; // Import List

public class MainActivity extends AppCompatActivity implements AlarmAdapter.OnToggleAlarmListener, AlarmAdapter.OnItemClickListener {

    private static final String TAG = "MainActivity";

    private RecyclerView alarmsRecyclerView;
    private AlarmAdapter alarmAdapter; // Will now extend ListAdapter
    private FloatingActionButton fabAddAlarm;
    private TextView emptyView;
    private AlarmScheduler alarmScheduler;
    private AlarmDatabase alarmDatabase;
    private int nextAlarmId = -1; // Field to store the ID of the next alarm

    // Launcher for requesting permissions
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                    checkAndRequestExactAlarmPermission(); // Proceed to next check
                } else {
                    Log.w(TAG, "Notification permission denied.");
                    // Explain why it's needed or disable features
                    showPermissionRationale(getString(R.string.notification_permission_message), false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize Views ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
        fabAddAlarm = findViewById(R.id.fabAddAlarm);
        emptyView = findViewById(R.id.emptyView);

        // --- Initialize Components ---
        alarmScheduler = new AlarmScheduler(this);
        alarmDatabase = AlarmDatabase.getDatabase(this);

        // --- Setup UI ---
        setupRecyclerView();
        setupItemTouchHelper();

        // --- Set Listeners ---
        fabAddAlarm.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM); // Added haptic feedback
            checkPermissionsAndAddAlarm(); // Check permissions before proceeding
        });

        // --- Initial Checks & Data Loading ---
        checkAndRequestPermissions(); // Check necessary permissions on startup
        observeAlarms(); // Start observing alarm data from database
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check exact alarm permission when returning to the app
        if (hasNotificationPermission() && !alarmScheduler.hasExactAlarmPermission()) {
            Log.w(TAG, "Resumed: Exact alarm permission still not granted.");
            // Optionally show rationale again if needed
            // showPermissionRationale(getString(R.string.exact_alarm_permission_message), true);
        }
    }

    // --- Permission Handling ---

    private void checkPermissionsAndAddAlarm() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission(); // Request notification perm first
        } else if (!alarmScheduler.hasExactAlarmPermission()) {
            // If notification perm granted, check/request exact alarm perm
            showPermissionRationale(getString(R.string.exact_alarm_permission_message), true);
        } else {
            // All necessary permissions granted, proceed to add alarm
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
            startActivity(intent);
        }
    }

    private void checkAndRequestPermissions() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
        } else {
            // Only check exact alarm if notification permission is already granted
            checkAndRequestExactAlarmPermission();
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not needed before Android 13 (Tiramisu)
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Show rationale if previously denied without "Don't ask again"
                showPermissionRationale(getString(R.string.notification_permission_message), false);
            } else {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


    private void checkAndRequestExactAlarmPermission() {
        // Only prompt if exact alarm permission is specifically needed and not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmScheduler.hasExactAlarmPermission()) {
            showPermissionRationale(getString(R.string.exact_alarm_permission_message), true);
        }
        // Otherwise, assume granted or not needed for this OS version
    }

    private void showPermissionRationale(String message, boolean isForExactAlarm) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    // Direct user to the appropriate settings screen
                    if (isForExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmScheduler.requestExactAlarmPermission(); // Uses Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    } else if (!isForExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Direct to app's notification settings
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    // User cancelled - features requiring permission may not work
                    Toast.makeText(this, "Permission denied. Alarm features may be limited.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // --- UI Setup ---

    private void setupRecyclerView() {
        alarmAdapter = new AlarmAdapter(this, this); // Pass listeners
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmsRecyclerView.setAdapter(alarmAdapter);
        // Optional: Add animations
        // alarmsRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void observeAlarms() {
        // Observe LiveData from the database
        alarmDatabase.alarmDao().getAllAlarms().observe(this, alarms -> {
            Log.d(TAG, "Alarm list updated. Count: " + (alarms != null ? alarms.size() : 0));
            // Submit the list to the ListAdapter for efficient updates
            alarmAdapter.submitList(alarms);

            // Show/Hide empty view based on the list
            boolean isEmpty = (alarms == null || alarms.isEmpty());
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            alarmsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            // Recalculate and update the indicator for the next alarm
            calculateAndUpdateNextAlarmIndicator(alarms);
        });
    }

    // --- Logic for Next Alarm Indicator ---
    private void calculateAndUpdateNextAlarmIndicator(@Nullable List<Alarm> alarms) {
        // Run calculation on a background thread to avoid blocking UI
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            int foundNextAlarmId = -1;
            long minTriggerTime = Long.MAX_VALUE;
            long now = System.currentTimeMillis();

            if (alarms != null) {
                for (Alarm alarm : alarms) {
                    if (alarm.isEnabled()) {
                        // Use the scheduler's logic to find the exact next trigger time
                        long triggerTime = AlarmScheduler.calculateTriggerTime(alarm.getHour(), alarm.getMinute());
                        // Find the earliest trigger time that is after the current time
                        if (triggerTime > now && triggerTime < minTriggerTime) {
                            minTriggerTime = triggerTime;
                            foundNextAlarmId = alarm.getId();
                        }
                    }
                }
            }

            final int finalNextAlarmId = foundNextAlarmId;
            final long finalMinTriggerTime = (minTriggerTime == Long.MAX_VALUE) ? -1 : minTriggerTime; // Use -1 if no next alarm

            // Update the adapter on the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                // Only update if the next alarm actually changed
                // or if the previous next alarm is still the next one (to update relative time)
                boolean needsAdapterUpdate = (this.nextAlarmId != finalNextAlarmId) || (this.nextAlarmId != -1);
                this.nextAlarmId = finalNextAlarmId;

                if (needsAdapterUpdate) {
                    alarmAdapter.setNextAlarmInfo(this.nextAlarmId, finalMinTriggerTime);
                    // ListAdapter ideally handles redraws via DiffUtil, but for external state
                    // like the indicator/relative time not directly compared by DiffUtil,
                    // we might need to nudge it. notifyDataSetChanged is the simplest way.
                    alarmAdapter.notifyDataSetChanged(); // Inefficient, but ensures visual update
                    Log.d(TAG, "Next alarm info updated in adapter: ID=" + this.nextAlarmId);
                }
            });
        });
    }


    // --- Adapter Listener Callbacks ---

    @Override
    public void onToggle(Alarm alarm) {
        Log.d(TAG, "Toggling alarm: ID=" + alarm.getId() + " Enabled=" + alarm.isEnabled());

        // Recalculate trigger time if enabling
        if (alarm.isEnabled()) {
            long triggerTime = AlarmScheduler.calculateTriggerTime(alarm.getHour(), alarm.getMinute());
            alarm.setTimeInMillis(triggerTime); // Ensure time is set for correct scheduling/sorting
        }

        // Perform DB update and scheduling on background thread
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            alarmDatabase.alarmDao().update(alarm); // Update DB

            // Schedule or cancel based on new state
            if (alarm.isEnabled()) {
                // Re-check permission just in case, though unlikely to change mid-operation
                if (!alarmScheduler.hasExactAlarmPermission()) {
                    runOnUiThread(() -> showPermissionRationale(getString(R.string.exact_alarm_permission_message), true));
                    Log.w(TAG, "Post-toggle: Cannot schedule alarm " + alarm.getId() + " due to missing permission.");
                    // Maybe revert state in DB/UI if scheduling fails? Or just warn user.
                } else {
                    alarmScheduler.schedule(alarm); // Schedule if enabled
                }
            } else {
                alarmScheduler.cancel(alarm); // Cancel if disabled
            }

            // Trigger widget update after DB operation completes/is initiated
            // Run on UI thread as it sends a broadcast
            runOnUiThread(() -> NextAlarmWidgetProvider.updateAllWidgets(getApplicationContext())); // ADDED WIDGET UPDATE
        });
    }

    @Override
    public void onItemClick(Alarm alarm) {
        Log.d(TAG, "Clicked alarm item: ID=" + alarm.getId());
        Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
        intent.putExtra(SetAlarmActivity.EXTRA_ALARM_ID, alarm.getId()); // Pass ID for editing

        // --- Shared Element Transition (Setup needed) ---
        // Requires finding the specific view in the RecyclerView corresponding to the clicked 'alarm'
        // This setup is complex and omitted here for brevity. See previous explanations.
        // Example:
        // View timeTextView = findViewInRecyclerForAlarm(alarm.getId()); // Placeholder for finding view
        // if (timeTextView != null) {
        //     ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, timeTextView, "alarm_time_shared");
        //     startActivity(intent, options.toBundle());
        // } else {
        //     startActivity(intent); // Fallback if view not found
        // }
        startActivity(intent); // Start without transition for now
    }

    // --- Swipe to Delete ---

    private void setupItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Not supporting drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition(); // Use binding adapter position
                if (position == RecyclerView.NO_POSITION) {
                    Log.w(TAG, "Swiped item has NO_POSITION, ignoring delete.");
                    return; // Invalid position
                }

                // *** USE THE NEW PUBLIC METHOD FROM ADAPTER ***
                Alarm alarmToDelete = alarmAdapter.getAlarmAtPosition(position);
                // **********************************************

                // Add null check - crucial if getAlarmAtPosition can return null
                if (alarmToDelete == null) {
                    Log.e(TAG, "Attempted to delete alarm at invalid position: " + position);
                    // Maybe notify item changed to reset swipe state if needed
                    // alarmAdapter.notifyItemChanged(position);
                    return;
                }

                // Proceed with delete logic on background thread
                final Alarm finalAlarmToDelete = alarmToDelete; // Make effectively final
                AlarmDatabase.databaseWriteExecutor.execute(() -> {
                    alarmDatabase.alarmDao().delete(finalAlarmToDelete); // Delete from DB
                    alarmScheduler.cancel(finalAlarmToDelete); // Cancel scheduled alarm
                    Log.d(TAG,"Alarm deleted and cancelled: ID=" + finalAlarmToDelete.getId());

                    // Show Snackbar and Trigger Widget Update back on UI thread
                    runOnUiThread(() -> {
                        showUndoSnackbar(finalAlarmToDelete); // Show Undo option
                        NextAlarmWidgetProvider.updateAllWidgets(getApplicationContext()); // ADDED WIDGET UPDATE
                    });
                });
            }

            // Optional: Customize swipe appearance (background color, icon)
            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Example: You could draw a red background and delete icon here
                // using libraries like ItemTouchHelper.Callback or manually drawing on the canvas 'c'.
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        }).attachToRecyclerView(alarmsRecyclerView); // Attach helper to RecyclerView
    }

    // --- Undo Snackbar Logic ---

    private void showUndoSnackbar(final Alarm deletedAlarm) {
        // Use root view or CoordinatorLayout for Snackbar if available
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, R.string.alarm_deleted, Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM); // Haptic feedback for undo action

                    // Perform re-insertion and re-scheduling on background thread
                    AlarmDatabase.databaseWriteExecutor.execute(() -> {
                        // Insert the alarm again (Note: This generates a NEW primary key ID)
                        long newId = alarmDatabase.alarmDao().insert(deletedAlarm);
                        Log.d(TAG,"Undo: Alarm re-inserted with new ID: " + newId);

                        // If the original alarm was enabled, reschedule it using the NEW ID
                        if (deletedAlarm.isEnabled()) {
                            // Create a new Alarm instance reflecting the re-inserted state with the new ID
                            Alarm rescheduledAlarm = new Alarm(
                                    deletedAlarm.getHour(), deletedAlarm.getMinute(), true,
                                    deletedAlarm.getRingtoneResourceId(), deletedAlarm.getRingtoneName(), 0L // Trigger time will be recalculated
                            );
                            rescheduledAlarm.id = (int) newId; // Set the correct NEW ID

                            // Recalculate trigger time based on current time
                            long triggerTime = AlarmScheduler.calculateTriggerTime(rescheduledAlarm.getHour(), rescheduledAlarm.getMinute());
                            rescheduledAlarm.setTimeInMillis(triggerTime); // Update the time in the object

                            // Schedule using the new object with the correct new ID and time
                            alarmScheduler.schedule(rescheduledAlarm);
                        }

                        // Trigger widget update after UNDO operation completes/is initiated
                        // Run on UI thread as it sends a broadcast
                        runOnUiThread(() -> NextAlarmWidgetProvider.updateAllWidgets(getApplicationContext())); // ADDED WIDGET UPDATE
                    });
                })
                // Optional: Set anchor view if using CoordinatorLayout with FAB
                // .setAnchorView(fabAddAlarm)
                .show();
    }
}