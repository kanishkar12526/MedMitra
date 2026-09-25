# Project Plan

Medicine Reminder & Health Schedule App
Problem Statement: Develop a mobile application that helps users manage medication schedules and general health routines. The application should notify users about scheduled medicines and maintain a history of completed or missed reminders.
Expected Features:
- Medicine scheduling
- Reminder notifications
- Dosage/time information
- Missed-dose tracking
- Daily schedule
- History dashboard
Challenge Level: Moderate-High

## Project Brief

# Project Brief: MedMitra

## Features
1. **Smart Medicine Scheduling & Reminders**: Comprehensive daily scheduling with dosage trackers, supported by automated notifications, voice reminders, and seamless WearOS integration.
2. **Medicine Identification & Info**: Users can capture a photo of a medicine using their device's camera. The app identifies the medicine and provides an easy-to-understand brief regarding its use, side effects, and impact on the body.
3. **Emergency SOS & Fall Detection**: Integrated safety features leveraging device and wearable sensors to detect falls, paired with a quick-access SOS system to alert predefined emergency contacts immediately.
4. **Health History Dashboard**: A tracking dashboard that maintains a reliable history of completed routines and missed doses, aiding users and caretakers in monitoring health adherence.

## High-Level Tech Stack
* **Language & Core Architecture**: Kotlin with Coroutines and StateFlow for asynchronous, reactive data handling.
* **UI Framework**: Jetpack Compose (Material Design 3).
* **Navigation & Adaptive Strategy**: 
  * Strictly using **Jetpack Navigation 3** (state-driven navigation) for managing screens across the app.
  * **Compose Material Adaptive** library to build fully responsive layouts that adapt perfectly to phones, foldables, and tablets.
* **Camera & AI Integration**: 
  * **CameraX** for consistent, high-quality image capture.
  * **Google AI (Gemini) or ML Kit + Medical API** to process the image, perform object recognition, and retrieve comprehensive medical information.
* **Wearables Integration**: Wear OS Compose library for building the smart watch extensions (fall detection and wrist-based alerts).
* **Background Tasks**: WorkManager for reliable, timely scheduling of background reminder notifications.

## Implementation Steps

### Task_1_CoreNavAndAdaptiveUI: Set up Jetpack Navigation 3, Compose Material Adaptive layouts, and basic routing for Dashboard, Camera, and Settings.
- **Status:** COMPLETED
- **Updates:** Completed Task 1: Setup dependencies, created Routes, implemented AppNavigation with NavigationSuiteScaffold and NavDisplay for adaptive UI on phones and tablets. Created placeholder screens for Dashboard, Camera, and Settings. Project builds successfully.
- **Acceptance Criteria:**
  - App builds successfully
  - Navigation 3 handles routing between screens
  - Adaptive layout scaffolding works on phone and tablet

### Task_2_SchedulingAndDashboard: Implement WorkManager for scheduling, Room database for local storage, and the main dashboard UI showing medication history, missed doses, and upcoming schedules. Add TTS functionality.
- **Status:** COMPLETED
- **Updates:** Implemented Room database with Medication and Schedule entities. Set up WorkManager for exact-time local notifications. Implemented TTSManager for voice-based announcements. Built Dashboard UI to show history and upcoming meds, and wired it with DashboardViewModel. Added appropriate permissions to Manifest. App compiles successfully.
- **Acceptance Criteria:**
  - Dashboard UI implemented
  - WorkManager schedules notifications
  - Voice-based TTS medication reminders work

### Task_3_CameraXAndGemini: Integrate CameraX for capturing medicine photos and connect to Google AI (Gemini) API to identify medicine and display details.
- **Status:** COMPLETED
- **Updates:** Added Gemini API key to local.properties and exposed via BuildConfig. Implemented CameraX capture and Gemini AI analysis in CameraScreen and CameraViewModel. App successfully displays medicine information. Created README.md. Project builds without errors.
- **Acceptance Criteria:**
  - CameraX preview and capture works
  - Gemini API_KEY is integrated properly
  - API successfully identifies medicine and returns info
  - App does not crash

### Task_4_WearOSAndSOS: Set up Wear OS Compose module, integrate fall detection triggers, implement 20-sec fallback SOS auto-alert system, and synchronize with mobile app.
- **Status:** COMPLETED
- **Updates:** Implemented SOS and Fall Detection via accelerometer sensor peak monitoring in FallDetector.kt and SOSViewModel.kt. Added 20-second countdown dialog with vibration, cancel option, emergency SMS dispatch, and ACTION_DIAL intent. Added Emergency Contact configuration in SettingsScreen. Integrated global SOS overlay in AppNavigation. Unit tests passed and assembleDebug built successfully.
- **Acceptance Criteria:**
  - WearOS module builds successfully
  - Wear Compose UI displays correctly
  - SOS trigger mechanism functions with a 20-second countdown

### Task_5_RunAndVerify: Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, report critical UI issues, and test all end-to-end flows.
- **Status:** COMPLETED
- **Updates:** Resolved build and runtime issues. Implemented a complete Glassmorphism design system in GlassComponents.kt (GlassBackground, GlassCard, GlassButton, GlassTextField, GlassTopAppBar, GlassDialogSurface). Refactored DashboardScreen, CameraScreen, SettingsScreen, and SOSDialog with glassmorphic cards and vibrant ambient gradients. Verified with assembleDebug and unit tests passing.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - all user requirements are met without UI overlapping
- **Duration:** N/A

