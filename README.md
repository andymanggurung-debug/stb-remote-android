# 📱 STB Remote Android Companion

A lightweight, modern native Android application (Kotlin + Jetpack Compose) to control your STB bots and forward torrent magnets, URLs, and YouTube links directly into your STB download queue via Telegram Bot API.

---

## ✨ Features

- **🚀 System Share Sheet Integration**: Tap **Share** anywhere on Android (Chrome, YouTube, Telegram, Reddit) → Select **Send to STB** → Instantly queues on your STB in the background.
- **🧲 Direct Magnet Links**: Tap any `magnet:` link in mobile Chrome / Firefox → Automatically opens the app and sends to STB.
- **⚡ Works Anywhere (No VPN Required)**: Communicates through the public Telegram Bot API. Works seamlessly over 4G/5G mobile data, public Wi-Fi, or home network.
- **🎮 Quick Controls**: Send bot commands (`/status`, `/speed`, `/list`, `/cleanup`) with one tap.
- **🎨 Modern Material 3 UI**: Clean dark theme with status indicators.

---

## 🛠️ How to Build & Install

### Option A: Open in Android Studio
1. Copy or clone the `stb-android-app` folder to your computer.
2. Open **Android Studio** → **Open Project** → Select `stb-android-app`.
3. Connect your Android phone (or use an emulator).
4. Click **Run ▶** or go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Install the generated `.apk` file onto your phone.

### Option B: Automatic Cloud Build via GitHub (No Android Studio Needed!)
1. Push this folder to your GitHub account (can be private or public).
2. The included GitHub Actions workflow (`.github/workflows/build-apk.yml`) will automatically trigger.
3. Once finished (~2 minutes), go to the **Actions** tab on GitHub, click the latest run, and download the compiled **`STB-Remote-APK`** artifact directly!

---

## ⚙️ Initial App Setup

1. Open **STB Remote** on your phone.
2. Enter your **Bot Token** and **Telegram User ID** (found in `/opt/stb-bots/.env`).
3. Tap **Save & Test Connection**.
4. You will receive a confirmation message in Telegram!

Now, whenever you share any link or click a magnet on your phone, select **Send to STB**!
