package com.nmims.wakeywakey;

import android.content.Context; // Import Context
import android.graphics.Color; // Import Color for indicator example
import android.text.format.DateUtils; // Import DateUtils
import android.view.HapticFeedbackConstants; // Import HapticFeedbackConstants
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView if using icon indicator
import android.widget.TextView; // Import TextView

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView; // Import CardView
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.recyclerview.widget.DiffUtil; // Import DiffUtil
import androidx.recyclerview.widget.ListAdapter; // Import ListAdapter
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView; // Import MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

// Changed to extend ListAdapter
public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    private final OnToggleAlarmListener toggleListener;
    private final OnItemClickListener itemClickListener;
    private int nextAlarmId = -1; // ID of the next alarm to ring
    private long nextAlarmTriggerTime = -1; // Trigger time of the next alarm

    public interface OnToggleAlarmListener {
        void onToggle(Alarm alarm);
    }

    public interface OnItemClickListener {
        void onItemClick(Alarm alarm);
    }

    // Constructor for ListAdapter
    public AlarmAdapter(OnToggleAlarmListener toggleListener, OnItemClickListener itemClickListener) {
        super(DIFF_CALLBACK); // Pass DiffUtil callback to super's constructor
        this.toggleListener = toggleListener;
        this.itemClickListener = itemClickListener;
    }

    // Method for MainActivity to update which alarm is next
    public void setNextAlarmInfo(int alarmId, long triggerTime) {
        this.nextAlarmId = alarmId;
        this.nextAlarmTriggerTime = triggerTime;
        // Note: Ideally, DiffUtil should handle updates if indicator state changes affect comparison,
        // or use payload updates. Calling notifyDataSetChanged() from MainActivity is simpler for now.
    }


    // **** ADDED PUBLIC METHOD TO FIX PROTECTED ACCESS ERROR ****
    /**
     * Retrieves the Alarm object at the specified position in the current list.
     * @param position The position of the item.
     * @return The Alarm object at the position, or null if position is out of bounds.
     */
    public Alarm getAlarmAtPosition(int position) {
        // Basic bounds check
        if (position >= 0 && position < getCurrentList().size()) {
            return getItem(position); // Call protected method from within subclass
        }
        return null;
    }
    // ***********************************************************


    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_alarm, parent, false);
        return new AlarmViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm currentAlarm = getItem(position); // Use getItem() from ListAdapter

        holder.alarmTimeTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", currentAlarm.getHour(), currentAlarm.getMinute()));
        holder.ringtoneNameTextView.setText(currentAlarm.getRingtoneName());

        // Set switch state without triggering listener
        holder.alarmSwitch.setOnCheckedChangeListener(null);
        holder.alarmSwitch.setChecked(currentAlarm.isEnabled());
        holder.alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); // Added haptic feedback
            if (toggleListener != null) {
                int currentPosition = holder.getBindingAdapterPosition(); // Use getBindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Alarm alarmToToggle = getItem(currentPosition);
                    // Create a new instance or modify carefully if needed, depending on DiffUtil behavior
                    // For simplicity, assume direct modification is okay here OR listener handles cloning.
                    alarmToToggle.setEnabled(isChecked);
                    toggleListener.onToggle(alarmToToggle);
                }
            }
        });

        // Set click listener for editing
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                int currentPosition = holder.getBindingAdapterPosition(); // Use getBindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(getItem(currentPosition));
                }
            }
        });

        // --- Next Alarm Indicator Logic ---
        boolean isNextAlarm = (currentAlarm.getId() == nextAlarmId);

        // Example 1: Change Card Background/Outline
        Context context = holder.itemView.getContext(); // Get context for colors
        if (isNextAlarm) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.cartoon_yellow));
            holder.cardView.setStrokeWidth(dpToPx(context, 2)); // Example: 2dp width
        } else {
            holder.cardView.setStrokeWidth(0); // Remove outline
        }

        // Example 2: Show/Hide Relative Time TextView
        if (isNextAlarm && nextAlarmTriggerTime > 0) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    nextAlarmTriggerTime,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            holder.relativeTimeTextView.setText("Rings " + relativeTime);
            holder.relativeTimeTextView.setVisibility(View.VISIBLE);
        } else {
            holder.relativeTimeTextView.setVisibility(View.GONE);
        }

        // Example 3: Show/Hide an Indicator Icon (If you added one)
        // holder.nextAlarmIndicator.setVisibility(isNextAlarm ? View.VISIBLE : View.GONE);
    }

    // Helper to convert dp to pixels
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }


    // ViewHolder includes views for indicator/relative time
    // Make sure IDs match your list_item_alarm.xml
    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView alarmTimeTextView;
        final TextView ringtoneNameTextView;
        final MaterialSwitch alarmSwitch;
        final TextView relativeTimeTextView;
        // final ImageView nextAlarmIndicator; // Uncomment if using an icon indicator

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView; // Assuming root is MaterialCardView
            alarmTimeTextView = itemView.findViewById(R.id.alarmTimeTextView);
            ringtoneNameTextView = itemView.findViewById(R.id.ringtoneNameTextView);
            alarmSwitch = itemView.findViewById(R.id.alarmSwitch);
            relativeTimeTextView = itemView.findViewById(R.id.relativeTimeTextView);
            // nextAlarmIndicator = itemView.findViewById(R.id.nextAlarmIndicator); // Uncomment if using
        }
    }


    // --- DiffUtil Callback (Essential for ListAdapter) ---
    private static final DiffUtil.ItemCallback<Alarm> DIFF_CALLBACK = new DiffUtil.ItemCallback<Alarm>() {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            // Compare all fields that affect the visual representation
            return oldItem.getHour() == newItem.getHour() &&
                    oldItem.getMinute() == newItem.getMinute() &&
                    oldItem.isEnabled() == newItem.isEnabled() &&
                    oldItem.getRingtoneResourceId() == newItem.getRingtoneResourceId() &&
                    java.util.Objects.equals(oldItem.getRingtoneName(), newItem.getRingtoneName()) &&
                    oldItem.getTimeInMillis() == newItem.getTimeInMillis(); // Include time for sorting consistency
            // Add label, repeat days etc. if they are displayed
        }
    };
}