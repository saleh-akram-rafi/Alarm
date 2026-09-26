# Discipline Alarm ⏰📸

> **A smart, discipline-enforcing Android alarm application that forces users to physically leave their bed and scan a pre-registered physical object (bathroom sink, mirror, study table, water bottle, etc.) using live CameraX capture and deep neural network feature verification before the alarm can be dismissed.**

---

## 📱 Features

- **Scan to Dismiss**: Cannot dismiss the alarm from bed. Requires physical movement and live camera verification of the pre-registered object.
- **Deep Feature Recognition (No Pixel Matching)**: Uses TensorFlow Lite MobileNet feature embeddings with Cosine Similarity metric. Invariant to camera shake, slight angle shifts, and lighting variations.
- **Precomputed Embedding Cache**: Extracts and serializes reference vectors during object registration; morning waking inference completes in under 80ms with single-frame processing.
- **Night Dashboard**: High-contrast, dark-mode banner showing active countdown, next alarm schedule, mission target, and snooze settings.
- **Bundled Discipline Audio**: Integrated `Kafir.mp3` ("Ya Kafir, Ya Kafir...") directly into `res/raw/` and `assets/sounds/` with progressive fade-in and infinite wake-up looping.
- **Android 12–16 Exact Alarm Reliability**: Leverages `AlarmManager.setExactAndAllowWhileIdle()` with `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.
- **Full-Screen Lockscreen UI**: Wakes device and displays over keyguard with `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and `FLAG_KEEP_SCREEN_ON`.
- **Foreground Audio Service**: Continuous audio playback via `ExoPlayer` using `USAGE_ALARM` and transient audio focus to override system volume.
- **Progressive Volume Fade-In**: Smoothly ramps up volume over 4 seconds to prevent acoustic shock.
- **Snooze Discipline**: Allows strict snooze limits (e.g. max 1 snooze, 5 minutes) or complete snooze disabling.
- **Reboot Resilience**: Automatically reschedules all active alarms upon device reboot via `BootReceiver` and `TimeChangeReceiver`.
- **Comprehensive Permissions Monitor**: In-app dashboard verifying Exact Alarm, Notification, Camera, and Battery Optimization permissions.

---

## 🏛️ Architecture

Discipline Alarm is built adhering strictly to **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Unidirectional Data Flow (UDF)** with **Dagger Hilt** and **Jetpack Compose**.

```text
com.discipline.alarm/
│
├── data/                       # Data Layer (Database, Network, System APIs)
│   ├── local/
│   │   ├── entity/             # AlarmEntity, MissionEntity, AlarmWithMission
│   │   ├── dao/                # AlarmDao, MissionDao
│   │   ├── converter/          # TypeConverters (FloatArray to ByteArray BLOB)
│   │   └── AppDatabase.kt      # Room Database configuration
│   ├── repository/             # AlarmRepositoryImpl, MissionRepositoryImpl
│   ├── alarm/                  # AndroidAlarmScheduler (AlarmManager)
│   ├── audio/                  # AlarmAudioPlayer (ExoPlayer + Vibrator)
│   └── mapper/                 # Entity <-> Domain model mappers
│
├── domain/                     # Domain Layer (Pure Kotlin Business Logic)
│   ├── model/                  # Alarm, Mission, DayOfWeek, VerificationResult
│   ├── repository/             # AlarmRepository, MissionRepository interfaces
│   ├── alarm/                  # AlarmScheduler interface
│   └── usecase/                # Use Cases for Alarms and Missions
│
├── presentation/               # Presentation Layer (Jetpack Compose + MVVM)
│   ├── theme/                  # Material 3 Dark High-Contrast Theme
│   ├── navigation/             # Screen routes and NavGraph
│   ├── home/                   # HomeScreen, NightDashboardBanner, AlarmCard
│   ├── alarm/                  # AlarmEditScreen, TimePicker, ThresholdSlider
│   ├── mission/                # MissionRegisterScreen (3-shot camera slots)
│   ├── trigger/                # AlarmActivity, AlarmTriggerScreen, Host
│   ├── verify/                 # VerificationScreen (Live CameraX Viewfinder)
│   └── permissions/            # PermissionsScreen (Live system status)
│
├── camera/                     # CameraX Manager (PreviewView, ImageCapture, Torch)
│
├── ml/                         # Computer Vision & Verification
│   ├── ImageProcessor.kt       # Luminance checks, Center crop, Normalization
│   ├── SimilarityEngine.kt     # Cosine Similarity & Multi-reference matching
│   ├── FeatureExtractor.kt     # Neural vector interface
│   ├── TFLiteFeatureExtractor.kt# MobileNetV2 + Spatial Descriptor fallback
│   └── VerificationEngine.kt   # Pipeline coordinating diagnostics & threshold
│
├── service/                    # Foreground Services & Notifications
│   ├── AlarmForegroundService.kt
│   └── NotificationHelper.kt
│
├── receiver/                   # System Broadcast Receivers
│   ├── AlarmReceiver.kt        # Receives AlarmManager triggers
│   ├── BootReceiver.kt         # Handles ACTION_BOOT_COMPLETED
│   └── TimeChangeReceiver.kt   # Handles TIME_SET & TIMEZONE_CHANGED
│
├── util/                       # Utilities (DateTime, Permissions, Storage)
├── di/                         # Hilt Dependency Injection Modules
└── MainActivity.kt             # Launcher Activity
```

---

## 🛠️ Setup & Prerequisites

- **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer (Jellyfish / Koala recommended)
- **JDK**: Java 17 or higher
- **Android SDK**:
  - `compileSdk`: 34 (Android 14) / 35
  - `targetSdk`: 34
  - `minSdk`: 29 (Android 10+)
- **Device / Emulator**: Android 10+ with Camera support

---

## 🚀 Build APK & Run

### Command Line (Windows PowerShell / Bash)

1. **Clone or navigate to the workspace**:
   ```powershell
   cd "C:\Users\USER\Documents\Project\Alarm"
   ```

2. **Run Unit Tests**:
   ```powershell
   ./gradlew test
   ```

3. **Build Debug APK**:
   ```powershell
   ./gradlew assembleDebug
   ```
   The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install onto connected device**:
   ```powershell
   ./gradlew installDebug
   ```

5. **Build Release APK**:
   ```powershell
   ./gradlew assembleRelease
   ```

---

## 🔐 Permissions Strategy

Discipline Alarm requests only legitimate, standard Android permissions:

| Permission | Reason |
| :--- | :--- |
| `SCHEDULE_EXACT_ALARM` | Required on Android 12+ (API 31+) to trigger alarm at exact seconds. |
| `USE_EXACT_ALARM` | Required on Android 13/14+ for alarm clock applications. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14 requirement for persistent, unkillable audio playback. |
| `USE_FULL_SCREEN_INTENT` | Allows waking up the display directly over the lockscreen. |
| `CAMERA` | Live CameraX viewfinder and photo verification of physical mission target. |
| `WAKE_LOCK` | Keeps CPU awake while alarm audio is sounding and until verification. |
| `RECEIVE_BOOT_COMPLETED` | Reschedules active alarms after phone reboot. |
| `VIBRATE` | Physical haptic alert during alarm ringing. |

---

## 🧠 ML Verification System

### Why Not Pixel Comparison?
Comparing raw image pixels fails completely in practice due to camera shake, sensor noise, ambient lighting variations, distance changes, and perspective shifts.

### Deep Learning Pipeline
1. **Luminance & Quality Check**:
   - Computes weighted luminance $Y = 0.299R + 0.587G + 0.114B$.
   - Fails early with helpful advice if room is completely dark ($< 30$) or blindingly overexposed ($> 240$).
2. **Preprocessing**:
   - Center crops to 1:1 aspect ratio.
   - Rescales to $224 \times 224$ RGB.
   - Normalizes to range $[-1.0, 1.0]$.
3. **Feature Embedding Extraction**:
   - Evaluates image through MobileNet neural feature extractor to produce a **1280-dimensional unit feature vector**.
   - *Intelligent Fallback*: If `.tflite` model asset is absent, dynamically utilizes a built-in 512-dimensional spatial color-gradient moment descriptor, ensuring zero downtime.
4. **Cosine Similarity Metric**:
   $$\text{CosineSim}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2}$$
5. **Multi-Angle Matching**:
   $$\text{Score} = \max(\text{sim}(E_{\text{live}}, E_{\text{ref1}}), \text{sim}(E_{\text{live}}, E_{\text{ref2}}), \text{sim}(E_{\text{live}}, E_{\text{ref3}}))$$
6. **Decision**:
   - If $\text{Score} \ge \text{Threshold}$ (default 75%) $\rightarrow$ `PASS` (Stops alarm service, dismisses UI).
   - If $\text{Score} < \text{Threshold} \rightarrow$ `FAIL` (Prompts user with targeted advice: "Turn on room light", "Match angle", "Hold steady").

---

## ⏰ Alarm Engine Lifecycle

```text
User Sets Alarm
       │
[AndroidAlarmScheduler]
       │
AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerTime)
       │
       ▼ (At scheduled millisecond)
[AlarmReceiver]
       │
Acquires WakeLock (10s)
       │
ContextCompat.startForegroundService(AlarmForegroundService)
       │
[AlarmForegroundService]
       ├── Builds High Priority Notification (IMPORTANCE_HIGH, bypass DND)
       ├── Launches FullScreenIntent to [AlarmActivity]
       ├── ExoPlayer plays looped audio (USAGE_ALARM, AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
       └── Acquires 15-minute safety WakeLock cutoff
       │
[AlarmActivity] (Shows over lockscreen)
       │
User taps "SCAN MISSION"
       │
[VerificationScreen] (CameraX live preview)
       │
Capture -> [VerificationEngine]
       │
     PASS?
    ├── YES ➔ Stop Foreground Service ➔ Dismiss UI ➔ Reschedule if repeating
    └── NO  ➔ Show failure reason & guidance tips ➔ Allow Retry
```

---

## ⚠️ Known Android Limitations & Best Practices

1. **Explicit Force Stop**:
   - If a user opens Android System Settings and manually taps **"Force Stop"**, the Android OS disables all scheduled `AlarmManager` alarms until the user re-launches the app. No application on Android can bypass this security policy.
2. **Device Shutdown / Power Off**:
   - If the device is completely powered down, no code runs. Once powered back on, Discipline Alarm's `BootReceiver` automatically restores all enabled alarms.
3. **Aggressive OEM Battery Optimization (Xiaomi, Huawei, OnePlus, Samsung)**:
   - Some vendors apply aggressive background execution policies. Discipline Alarm includes a built-in battery optimization checker directing users to whitelist the app from battery restrictions.
4. **Gallery Images Disabled**:
   - Discipline Alarm does not allow picking photos from the device gallery for verification. It strictly requires a live camera capture to guarantee physical presence.

---

## 🧪 Testing

The project includes unit tests covering:
- **`DateTimeUtilsTest`**: Tests one-shot scheduling, time rollover (scheduling tomorrow when time passed), repeating weekday offsets, and countdown generation.
- **`SimilarityEngineTest`**: Tests cosine similarity formulas, identical vectors (1.0), orthogonal vectors (0.0), zero vector edge cases, and best match selection.
- **`AlarmMapperTest`**: Tests bidirectional conversion between Room database entities and clean domain models.
- **`ConvertersTest`**: Tests binary serialization of `FloatArray` embeddings to SQLite BLOB `ByteArray`.
- **`ScheduleAlarmUseCaseTest`**: Tests inserting/updating alarms and exact AlarmManager coordination.
- **`SnoozeAlarmUseCaseTest`**: Tests snooze limit enforcement and counter increments.
- **`DismissAlarmUseCaseTest`**: Tests one-shot disabling vs repeating alarm next-trigger rescheduling.

Run tests using Gradle:
```bash
./gradlew test
```
