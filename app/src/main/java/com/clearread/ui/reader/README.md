# ClearRead

<p align="center">
  <img src="/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="ClearRead Icon" width="120"/>
</p>

<p align="center">
  <strong>A clean, ad-free, lightweight PDF reader for Android</strong>
</p>

<p align="center">
  Zero ads • Zero trackers • Fully offline
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform"/>
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"/>
  <img src="https://img.shields.io/github/v/release/Namikkemal/clearread" alt="Release"/>
</p>

---

## 📱 Screenshots

<p align="center">
  <img src="screenshots/home.jpg" width="250" />
  <img src="screenshots/reader.jpg" width="250" />
  <img src="screenshots/settings.jpg" width="250" />
</p>

---

## ✨ Features

- **📖 Clean Reading Experience** — No ads, no clutter, just your documents
- **🔍 Fast Search** — Find text across your entire PDF with highlighted results
- **🔖 Smart Bookmarks** — Save your place and jump back anytime
- **🎨 Material You** — Dynamic theming that adapts to your wallpaper
- **🌙 Dark Mode + AMOLED** — True black for AMOLED screens to save battery
- **⚡ Optimized for Low-End Devices** — Runs smoothly on 4GB RAM phones
- **🔒 Complete Privacy** — No data collection, no internet permission, fully offline
- **📂 Built-in File Explorer** — Browse and organize your PDFs easily

---

## 📥 Download

<a href="YOURPLAYSTORELINK">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">
</a>

Or build from source (see below)

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room (SQLite)
- **Design:** Material 3 / Material You
- **PDF Rendering:** Android Pdfium (built-in)

---

## 🤔 Why ClearRead?

I built ClearRead because I was tired of seeing my family struggle with bloated, ad-filled PDF readers on their budget phones. Every app I tried was either:

- Loaded with intrusive ads
- Asking for sketchy permissions
- Slow and laggy on low-end devices
- Tracking user behavior

So I decided to build what should have existed from the start: **a PDF reader that just works, respects your privacy, and runs fast on any device.**

No ads. No trackers.

---

## 🔧 Build from Source

```bash
# Clone the repository
git clone https://github.com/Namikkemal/clearread.git

# Open in Android Studio
cd clearread
# File → Open → Select the project folder

# Build the APK
./gradlew assembleRelease

# APK will be at: app/build/outputs/apk/release/
```

**Requirements:**
- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 26+ (supports Android 8.0 Oreo and above)

---

## 🌍 Privacy

ClearRead collects **zero data**.

- ❌ No analytics
- ❌ No crash reporting
- ❌ No ad networks
- ❌ No internet permission
- ✅ All files stay on your device

Read the full [Privacy Policy](PRIVACYPOLICYLINK).

---

## 🤝 Contributing

Contributions are welcome! Feel free to:

- 🐛 Report bugs via [Issues](https://github.com/Namikkemal/clearread/issues)
- 💡 Suggest features
- 🔧 Submit pull requests

---

## 📜 License