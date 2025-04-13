package com.nmims.wakeywakey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MathProblemActivity extends Activity {
    private EditText answerEditText;
    private TextView questionTextView;
    private int correctAnswer;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private String ringtoneUri; // Stores the selected alarm sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dismiss_alarm);

        // Initialize UI elements
        answerEditText = findViewById(R.id.answerInput);
        questionTextView = findViewById(R.id.mathQuestionText);
        Button submitButton = findViewById(R.id.submitButton);

        // Get the ringtone URI from the intent
        ringtoneUri = getIntent().getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI); // 🔥 Fix key mismatch

        // Generate a math problem
        generateMathProblem();

        // Play alarm sound in loop
        playAlarmSound();

        // Start vibration
        startVibration();

        // Set button click listener
        submitButton.setOnClickListener(v -> checkAnswer());
    }

    private void generateMathProblem() {
        int num1 = (int) (Math.random() * 10) + 1;
        int num2 = (int) (Math.random() * 10) + 1;
        correctAnswer = num1 + num2;

        String question = "Solve: " + num1 + " + " + num2;
        questionTextView.setText(question);
    }

    private void playAlarmSound() {
        try {
            if (ringtoneUri != null && !ringtoneUri.equals("default")) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, Uri.parse(ringtoneUri));
                mediaPlayer.setLooping(true);

                // Fix potential crash with prepare()
                mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
                mediaPlayer.prepareAsync();
            } else {
                // Play default sound if no ringtone is set
                mediaPlayer = MediaPlayer.create(this, R.raw.happyever);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play alarm sound", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] vibrationPattern = {0, 500, 1000}; // Vibrate for 500ms, pause for 1000ms
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0)); // 🔥 Fix infinite loop
        }
    }

    private void checkAnswer() {
        String userInput = answerEditText.getText().toString().trim();
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Enter an answer!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int userAnswer = Integer.parseInt(userInput);
            if (userAnswer == correctAnswer) {
                stopAlarm();
                finish(); // Close the activity
            } else {
                Toast.makeText(this, "Wrong answer! Try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input! Enter a number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAlarm() {
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

        // 🔥 Release WakeLock if held
        if (AlarmReceiver.wakeLock != null && AlarmReceiver.wakeLock.isHeld()) {
            AlarmReceiver.wakeLock.release();
            AlarmReceiver.wakeLock = null;
        }

        Toast.makeText(this, "Alarm stopped!", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
