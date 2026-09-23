# Memora — Ambient AI Memory Layer for Android

> **An ambient AI memory layer for your phone. Capture deadlines, events, and tasks from your screen with a single tap using Groq & Gemini VLM—without ever leaving your active app.**

---

## The Problem
Every day, commitments arrive scattered across chat apps, emails, and web portals:
- *"We have to submit the hackathon prototype by September 30."* (WhatsApp)
- *"Batch 2 class is tomorrow at 7 PM."* (Telegram)
- *"Assignment deadline is Friday."* (Gmail)
- *"Portal application closes October 3."* (Chrome)

Normally, you have to manually copy-paste, switch apps, type dates, and create reminders. Things inevitably slip through the cracks.

## The Memora Solution
A small, non-intrusive floating capture button sits on the edge of your Android screen. Whenever you encounter something you need to remember:
1. **Tap the Memora button.**
2. Memora captures the screen, sends it to a high-speed Vision Language Model (VLM), and extracts the title, date, time, and category.
3. An overlay review card pops up with smart ambiguity chips (e.g. `[3:30 PM]` vs `[3:30 AM]`, `[Tomorrow]` vs `[Today]`).
4. Tap **Save Memory**—and continue chatting or browsing without interruption.

---

## Key Features

- **Dual-Engine VLM Architecture**:
  - **Groq Cloud (`qwen/qwen3.8-27b`)**: Sub-second **0.25-second** screenshot inference.
  - **Google Gemini (`gemini-3.5-flash-lite`)**: Reliable Google AI Studio fallback.
- **Non-Intrusive Ambient Capture**: Runs via Android `MediaProjection` and a foreground service; never forces the main app to the foreground.
- **Smart Temporal Disambiguation**: Injects real-time device clock anchors to normalize relative dates (`"tomorrow"` $\to$ actual calendar date, `"330"` $\to$ `3:30 PM`) and flags AM/PM ambiguity directly in the review overlay.
- **Compact Neo-Brutalist Floating Control**:
  - 38dp compact tactile yellow circle with Memora Shutter Logo.
  - Edge snapping to left/right screen borders.
  - Interactive **Drag-to-Remove**: Dragging reveals a bottom-center `✕` target with haptic feedback and red visual glow to dismiss anytime.
- **100% Local Storage**: Powered by Room Database with SQLite for instant, private on-device recall.
- **Categorization & Human Feedback Loop**: Categorizes into Hackathons, Academics, Discussions, Events, Tasks, and Notes. Remembers user manual corrections to personalize future suggestions.
- **Profile Customization**: Choose avatar presets, custom display name, and role headline.

---

## Tech Stack
- **UI Framework**: Android Jetpack Compose (Material 3 + Neo-Brutalist Design Tokens)
- **Language**: Kotlin 2.0+
- **Database**: Android Jetpack Room (Local SQLite)
- **VLM Engines**: Groq Cloud Vision API + Google Gemini API (OkHttp / JSON)
- **Background Engine**: Android Foreground Service + MediaProjection + WindowManager Overlay

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/vishal-raaj-dnd/Memora--Single-Tap-Commitment-Tracker.git
cd Memora--Single-Tap-Commitment-Tracker
```

### 2. Configure API Keys
Copy the example environment file:
```bash
cp .env.example .env
```
Open `.env` and add your free API key(s):
```env
# Optional: Google AI Studio key (free at https://aistudio.google.com)
GEMINI_API_KEY=your_gemini_key_here

# Optional: Groq Cloud key for ultra-fast 0.25s inference (free at https://console.groq.com)
GROQ_API_KEY=your_groq_key_here
```
*(Note: You can also configure keys directly inside the Memora App under Settings $\to$ AI Vision Engine).*

### 3. Build & Install
Open the project in **Android Studio** or build from command line:
```bash
# Compile debug APK
./gradlew assembleDebug

# Install to connected device via USB debugging
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)**: For the floating capture button and review card.
- **Screen capture (`MediaProjection`)**: To snapshot the screen only when the capture button is explicitly tapped.
- **Notifications (`POST_NOTIFICATIONS`)**: For foreground capture service status and deadline reminders.

---

## License
MIT License. Free to use, adapt, and build upon.
