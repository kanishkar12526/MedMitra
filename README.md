# MedMitra 💊❤️

A comprehensive Medicine Reminder & Health Schedule mobile application developed in Flutter. MedMitra is designed to help users manage their medication schedules, track daily health routines, and stay protected with advanced background safety features.

## 🚀 Features

- **Medicine Scheduling**: Easily add medications, along with their precise dosage and time.
- **Reminder Notifications**: Punctual local push notifications remind you exactly when it's time to take your pills.
- **Daily Dashboard**: View your upcoming and pending medicines for the day.
- **Missed-Dose Tracking**: Mark doses as completed ✅ or missed ❌.
- **History Dashboard**: A detailed historical log of all your medication intake.
- **Fall Detection Safety**: Utilizes device accelerometer sensors to detect sudden hard impacts simulating a fall, triggering an SOS alert prompt.
- **Heart Rate Monitor**: A mock interface demonstrating how users can track their heart rate over time.

## 🛠️ Technology Stack

- **Framework**: [Flutter](https://flutter.dev/)
- **State Management**: [Provider](https://pub.dev/packages/provider)
- **Local Database**: [SQFlite](https://pub.dev/packages/sqflite) (SQLite for Flutter)
- **Notifications**: [flutter_local_notifications](https://pub.dev/packages/flutter_local_notifications)
- **Sensors**: [sensors_plus](https://pub.dev/packages/sensors_plus) (for Fall Detection)
- **Date & Time**: [intl](https://pub.dev/packages/intl)

## 📁 Project Structure

```text
lib/
├── main.dart                      # App entry point & Bottom Navigation
├── models/
│   └── medicine.dart              # Medicine Data Model
├── providers/
│   └── medicine_provider.dart     # State Management for Medicines
├── screens/
│   ├── add_medicine_screen.dart   # Form to schedule new medicines
│   ├── heart_rate_screen.dart     # Mockup Heart Rate Tracking UI
│   ├── history_screen.dart        # Log of completed & missed doses
│   └── home_screen.dart           # Daily Schedule Dashboard
└── services/
    ├── db_helper.dart             # SQLite operations
    ├── fall_detection_service.dart # Accelerometer listener logic
    └── notification_service.dart  # Local Alarm & Push Notification setup
```

## ⚙️ How to Run

1. **Clone the repository** (if hosted):
   ```bash
   git clone <your-repo-link>
   cd MedMitra
   ```

2. **Fetch Dependencies**:
   ```bash
   flutter pub get
   ```

3. **Run the App**:
   - Ensure you have a connected Android/iOS device or an emulator running.
   - Execute the following command:
   ```bash
   flutter run
   ```

## 📱 Android Permissions Configured
This app has been pre-configured with the following permissions for alarms and background sensors in `AndroidManifest.xml`:
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`
- `SCHEDULE_EXACT_ALARM`
- `USE_EXACT_ALARM`
- `POST_NOTIFICATIONS`

---
*Built with ❤️ using Flutter.*
