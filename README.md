# ⏰ WakeyWakey – Smart Alarm App

**WakeyWakey** is a smart Android alarm application designed to *actually wake you up*. The alarm cannot be dismissed unless the user solves a **math challenge**, helping prevent oversleeping and mindless snoozing.

---

## 🎯 Goal & Purpose

This project aims to build a reliable alarm clock for Android users who often ignore or snooze regular alarms. By integrating **math-based dismissal**, we ensure mental alertness as the first task of the day.

---

## ⚙️ Key Features

- 🔢 **Math Challenge Alarm Dismissal**  
  Alarm can only be stopped by solving a random math problem correctly.

- ⏱️ **Set One-Time or Repeating Alarms**  
  Flexible scheduling options with customizable time and days.

- 🔒 **Full-Screen Alarm Lock**  
  Alarm rings over lock screen using `USE_FULL_SCREEN_INTENT`.

- 🔋 **Power-Resilient Alarms**  
  Uses `BOOT_COMPLETED` broadcast to re-register alarms after a device restart.

- 📶 **Foreground Service & Wake Lock**  
  Ensures the alarm rings even when the app is in the background or the device is idle.

- 🎵 **Custom Ringtone & Vibration**  
  Choose from built-in tones or upload your own, with haptic feedback support.

- 💬 **Auto-Dismiss Timeout**  
  Automatically stops the alarm after a fixed timeout if unanswered (configurable).

---

## 🧠 How It Works

When an alarm triggers:
1. A full-screen alarm interface takes over the screen.
2. A random **math question** (e.g., 12 + 7 × 3) is shown.
3. The user must correctly solve it to disable the alarm.
4. Wrong answers = the alarm keeps ringing!

---

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/4626fd37-623a-4f71-ad35-6849f68e24e5" alt="Alarm Screen" width="250"/>
  <img src="https://github.com/user-attachments/assets/0ed15c5d-7d35-4731-975a-e8550f62d3ca" alt="Set Alarm" width="250"/>
  <img src="https://github.com/user-attachments/assets/d23ef233-3623-446e-9e91-fa35c1b1164c" alt="Math Puzzle" width="250"/>
</p>


> Add your screenshots in the `assets` or `screenshots/` folder and replace the image URLs above.

---

## 🛠️ Tech Stack

- **Platform**: Android (Java/Kotlin)
- **Alarm Logic**: AlarmManager, ForegroundService, WakeLock
- **Math Engine**: Random equation generator
- **Persistence**: Room Database for saved alarms

---

## 🚀 Getting Started

1. Clone this repository:

```bash
git clone https://github.com/4ryanwalia/wakey-wakey.git
