package com.nmims.wakeywakey;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    private final OnToggleAlarmListener toggleListener;
    private final OnItemClickListener itemClickListener;
    private final OnSwipeToDeleteListener deleteListener;
    private int nextAlarmId = -1;
    private long nextAlarmTriggerTime = -1;

    private static final DiffUtil.ItemCallback<Alarm> DIFF_CALLBACK = new DiffUtil.ItemCallback<Alarm>() {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getTimeInMillis() == newItem.getTimeInMillis() &&
                    oldItem.isEnabled() == newItem.isEnabled() &&
                    oldItem.getRingtoneName().equals(newItem.getRingtoneName());
        }
    };

    public interface OnToggleAlarmListener {
        void onToggle(Alarm alarm);
    }

    public interface OnItemClickListener {
        void onItemClick(Alarm alarm);
    }

    public interface OnSwipeToDeleteListener {
        void onSwipe(Alarm alarm);
    }

    public AlarmAdapter(OnToggleAlarmListener toggleListener, OnItemClickListener itemClickListener, OnSwipeToDeleteListener deleteListener) {
        super(DIFF_CALLBACK);
        this.toggleListener = toggleListener;
        this.itemClickListener = itemClickListener;
        this.deleteListener = deleteListener;
    }

    public void setNextAlarmInfo(int alarmId, long triggerTime) {
        this.nextAlarmId = alarmId;
        this.nextAlarmTriggerTime = triggerTime;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_alarm, parent, false);
        return new AlarmViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm currentAlarm = getItem(position);

        String formattedTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(currentAlarm.getTimeInMillis()));
        holder.alarmTimeTextView.setText(formattedTime);
        holder.ringtoneNameTextView.setText(currentAlarm.getRingtoneName());
        

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAbsoluteAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && itemClickListener != null) {
                Alarm selectedAlarm = getItem(currentPosition);
                Intent editIntent = new Intent(holder.itemView.getContext(), SetAlarmActivity.class);
                editIntent.putExtra(SetAlarmActivity.EXTRA_ALARM_ID, selectedAlarm.getId());
                holder.itemView.getContext().startActivity(editIntent);
            }
        });

        boolean isNextAlarm = (currentAlarm.getId() == nextAlarmId);
        Context context = holder.itemView.getContext();
        if (isNextAlarm) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.cartoon_yellow));
            holder.cardView.setStrokeWidth(dpToPx(context, 2));
        } else {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.medium_gray));
            holder.cardView.setStrokeWidth(dpToPx(context, 1));
        }

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
    }

    private void stopAlarm(Context context, Alarm alarm) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.stopService(serviceIntent);
    }

    public ItemTouchHelper getSwipeToDeleteHelper() {
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAbsoluteAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Alarm alarmToDelete = getItem(position);
                    deleteListener.onSwipe(alarmToDelete);
                }
            }
        });
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView alarmTimeTextView;
        final TextView ringtoneNameTextView;;
        final TextView relativeTimeTextView;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.alarmCardView);
            alarmTimeTextView = itemView.findViewById(R.id.alarmTimeTextView);
            ringtoneNameTextView = itemView.findViewById(R.id.ringtoneNameTextView);
            relativeTimeTextView = itemView.findViewById(R.id.relativeTimeTextView);
        }
    }
}
