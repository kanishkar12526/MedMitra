# MedMitra 💊❤️
**AI Health Companion & Background SOS Shield**

MedMitra is a comprehensive, production-ready Native Android health application built with Kotlin, Jetpack Compose, and Material 3. It provides on-time medication voice reminders, 100% offline prescription scanning, smartwatch vitals tracking, senior accessibility mode, and automated 24/7 emergency SOS dispatch with live GPS location links.

---

## 🚀 Key Features

* **🚨 One-Tap & Automated Emergency SOS**:
  - Automatically places direct phone calls (`ACTION_CALL`) and dispatches emergency SMS to family contacts during SOS triggers.
  - Attaches live Google Maps GPS location links (`https://maps.google.com/?q=lat,lng`) to emergency SMS messages.
* **👴 Senior Accessibility Mode**:
  - Toggleable high-visibility UI mode with enlarged buttons (72dp height), large high-contrast typography (30sp), and oversized touch targets for elderly ease-of-use.
* **🇮🇳 Multilingual Audio Reminders (Tamil, Hindi, Telugu, etc.)**:
  - Enunciated voice reminders in **Tamil** (`"ஆரோக்கியமாக இருக்க உங்கள் மருந்து [Medicine] உட்கொள்ளுங்கள்"`), Hindi, Telugu, Kannada, Malayalam, Marathi, Gujarati, Bengali, and English.
  - Speech synthesis tuned for clarity (`0.88f` rate, `1.02f` pitch) with `USAGE_ALARM` audio enforcement.
* **📄 100% Offline Doctor Prescription Scanning**:
  - Uses Google ML Kit On-Device Text Recognition and custom NLP regex rules to extract drug names, dosages, and schedules (e.g. `1-0-1`, `TID`) with zero internet required.
* **🤖 Google Gemini AI Integration**:
  - Cloud-assisted pill identification and side-effect explanations using Gemini 1.5 Flash.
* **⌚ Smartwatch Health Vitals & Fall Detection**:
  - Monitors accelerometer sensor impact spikes for fall detection with continuous 24/7 background foreground service execution (`MedMitraForegroundService`).
  - Tracks Heart Rate, SpO2 Oxygen, Body Temp, and Daily Steps with real-time alert thresholds.

---

## 🛠️ Technology Stack

* **Language**: Kotlin 2.0 (Coroutines, StateFlow, SharedFlow)
* **UI Engine**: Jetpack Compose, Material Design 3, Jetpack Navigation 3
* **Local Persistence**: Room Database (SQLite ORM) & DataStore Preferences
* **Background Jobs**: WorkManager & Foreground Service (`FOREGROUND_SERVICE_TYPE_HEALTH`)
* **ML & AI**: On-Device Google ML Kit Text Recognition & Google Gemini AI SDK
* **Sensors & Audio**: SensorManager (Accelerometer), `AudioTrack` PCM Siren Generator, TextToSpeech API

---

## ⚙️ Setup & Building

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kanishkar12526/MedMitra.git
   cd MedMitra
   ```

2. **Add Gemini API Key**:
   Add your Gemini API key to `secrets.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Build and Run**:
   Open in Android Studio (2026.1+ recommended) or run with Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

---
*Built with ❤️ using Native Kotlin & Jetpack Compose.*
