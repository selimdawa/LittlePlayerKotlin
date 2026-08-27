<h1 align="center">Little Player - Kotlin</h1>

<p align="center">
  <a href="https://android-arsenal.com/api?level=24"><img alt="Min SDK" src="https://img.shields.io/badge/Min%20SDK-24-020290?style=flat-square&logo=android&logoColor=white"/></a>
  <a href="https://developer.android.com/about/versions/14"><img alt="Target SDK" src="https://img.shields.io/badge/Target%20SDK-37-0EB265?style=flat-square&logo=android&logoColor=white"/></a>
  <a href="https://kotlinlang.org"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.10-blue?style=flat-square&logo=kotlin&logoColor=white"/></a>
  <a href="https://opensource.org/licenses/MIT"><img alt="License" src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square"/></a>
</p>

<p align="center">
  <strong>Little Player</strong> is a high-performance, open-source Android audio player designed for a seamless listening experience. Built with the latest Jetpack libraries, it combines a beautiful UI with robust functionality.
</p>

---

## 📱 Screenshots

<div align="center">

| Songs | Albums | Search |
| :---: | :---: | :---: |
| <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgJYDZw6dpWpKUIknfvG7rWREPSdHtjnfrAlolao391uR5mQu1h6_PK5ywGRzL39VAWTmzLMZn5eZePkQfBz0Lt8IZ0z4UTMoxDV-1rOSdc7ykRYNHyQKF5yguGdG8YVURIV0tiM9FIkdCPWpo0q96aTUwLp6ro4nqhKJiIFSaKVCjMNUZN-xksPV4m6w/s2400/01.jpg" width="250"> | <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhRCfhUYysf5nkHxFDhDbHLWFQ0UaK7GULLc17PZXKLaEcNRprTdtvCd1o50eCRAETW5BB6rCZR2mz6R9jKyiY5SC6NK254ayeTzOlKTfquIx6PZhO4loEUY2hKakMqQl1pcuC-Ez_dLn2eiWII0EBPrwhhQ73kEyDh2_JmOJrAXKmOOk6M-kGfdhxDBw/s2400/02.jpg" width="250"> | <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEi26XBE-8xKgT1wqHMjCTtUnn4U6uocm8N-M-EHrCpFxtpi0S9484ccPBq1NimxJbmvybyENQ4aMenHg46kz3q3qffLldzPloQp853paQ0nx2ZpAGx3CarVzcbJRO7X_7ie65GC4M1MgQMxwdrlWDlMMrIvTOk5jRzeg5BXeWDReISodKpGUZYzv3I2JQ/s2400/03.jpg" width="250"> |

| Album Songs | Sort | Playing Now |
| :---: | :---: | :---: |
| <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjoFsIs6DiSAm_MZIkxGf8ZKfmLM-B4G-bSEEhT_skdU4269Njj4xwZmGn2A7CN6ZcVIOPBYGFP8wH0E6F13V5qtZ87TXYH-Z2Lmz207Z5QVFY0lG5fmu7mZIZZzlgMoAeoGS5c7cc9zzgDZ5frVbVwnjqlM8TsxeCy9wJpeYmA7YXXLmupt7wzQFFKFg/s2400/04.jpg" width="250"> | <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgeiX0zywXiAjfg34VDe1aLl9ojsi74z82TO8cuSguXUhIfEq4yxUAgk11x6PRAKM5WD8QtKTWea4a4STMey4RVynlqdYcHK5U-TwT-84GoNDBFAqgvSNiIsjt7bYRiz5S52QT1kCVma_vp3UwbpC5F4L9TEg96_3a15yTHdiVlQlGPZ459s2_LLLvLsg/s2400/05.jpg" width="250"> | <img src="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgAeP-nFWzihaGVMy1V8l-B8Z5Eapqm_-Xew2v5GtDgsWYnFPddnB5uvoh0IKlLAZxA5NmYjDrVMLMh2G3elioreC5pc107v_EF8vwIwDPgW-lFpRkDIwlDwDu0ANwhLllNIf4ujaizSQda_nImp0Lg2ergGPXt4T4j43flLPwWEKOd5fBYX_Wz9CPf3g/s2400/06.jpg" width="250"> |

</div>

---

## ✨ Features

- 🎵 **Powerful Audio Engine:** Powered by **Jetpack Media3 ExoPlayer** for high-quality playback and seamless transitions.
- 🌊 **Visual Experience:** Real-time **Audio Waveforms** and dynamic theming using **Palette API** to match the album art colors.
- 🏗️ **Modern Architecture:** Clean code following **MVVM** pattern, **Hilt** for Dependency Injection, and **Coroutines** for asynchronous tasks.
- 📂 **Offline Support:** Managed local database using **Room** and preferences with **DataStore**.
- 🎨 **Smart UI:** Beautifully crafted using **Material Design 3**, **ViewBinding**, and custom widgets for a premium feel.
- 📡 **Advanced Features:** **Google Cast** integration, audio tagging support via **JAudioTagger**, and fast image loading with **Coil**.

---

## 🛠️ Built With

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** Material Design 3, ViewBinding, Palette API
- **Media Engine:** [Jetpack Media3](https://developer.android.com/guide/topics/media/media3) (ExoPlayer, Cast)
- **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Networking/Images:** [Coil](https://coil-kt.github.io/coil/)
- **Audio Processing:** [Amplituda](https://github.com/lincollincol/Amplituda), [WaveformSeekBar](https://github.com/massoudss/WaveformSeekBar)
- **Async Operations:** Coroutines & Flow

---

## 🏗️ Architecture
The project follows the **Clean Architecture** principles and **MVVM (Model-View-ViewModel)** pattern:
- **UI Layer:** Fragments and Activities using ViewBinding.
- **ViewModel Layer:** Handles business logic and UI state using LiveData/Flow.
- **Repository Layer:** Abstracted data access from local sources.
- **Data Layer:** Room database and MediaStore for audio files.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17.
- Android SDK Level 37 (Compile SDK).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/selimdawa/LittlePlayer-Kotlin.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the app on your device or emulator.

---

## 📦 Project Structure
```text
app/src/main/java/com/flatcode/littleplayer/
├── di/             # Dependency Injection modules
├── data/           # Data sources (Local/Room)
├── model/          # Data models
├── repository/     # Data repositories
├── viewmodel/      # ViewModels
├── activity/       # Activities
├── fragment/       # UI Fragments
├── adapter/        # RecyclerView Adapters
├── service/        # Background Services (Media playback)
├── utils/          # Utility classes
└── widget/         # Custom UI components
```

---

## 🔗 Links & Resources
- **Download:** <a href='https://play.google.com/store/apps/details?id=com.flatcode.littleplayer'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="140px" style="vertical-align:middle;"/></a>
- **Legacy Version:** [Java Old Code Version](https://github.com/selimdawa/LittlePlayer/)
- **Author:** [Selim Dawa](https://github.com/selimdawa)

---

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
