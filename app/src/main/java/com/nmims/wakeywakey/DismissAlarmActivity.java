package com.nmims.wakeywakey;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class DismissAlarmActivity extends Activity {

    private int correctAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dismiss_alarm);

        TextView questionText = findViewById(R.id.questionText);
        EditText answerInput = findViewById(R.id.answerInput);
        Button submitButton = findViewById(R.id.submitButton);

        int num1 = new Random().nextInt(10) + 1;
        int num2 = new Random().nextInt(10) + 1;
        correctAnswer = num1 + num2;

        questionText.setText("Solve: " + num1 + " + " + num2 + " = ?");

        submitButton.setOnClickListener(v -> {
            try {
                int userAnswer = Integer.parseInt(answerInput.getText().toString().trim());
                if (userAnswer == correctAnswer) {
                    stopAlarm();
                } else {
                    Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid number!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopAlarm() {
        stopService(new Intent(this, AlarmService.class));
        finish();
    }
}