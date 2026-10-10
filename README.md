# UltraWide Pro Camera (আল্ট্রা-ওয়াইড ক্যামেরা ও স্টুডিও)

Professional Android Ultra-Wide Camera & Photo Studio app built with Jetpack Compose, CameraX, and Material 3.

## ✨ Features
- **0.5x Ultra-Wide Angle Lens:** Direct physical lens access or wide-angle enhancement simulation with dedicated 0.5x HUD badge.
- **Smooth 0.5x to 10x Zoom:** Quick lens presets (0.5x, 1x, 2x, 5x, 10x) and fine-tuning slider.
- **Pro Manual Controls:**
  - Manual Focus (AF / MF with Macro 0.0 to Infinity 1.0 focus distance slider).
  - Exposure Compensation (-3.0 EV to +3.0 EV with reset button).
  - Tap-to-Focus with animated focus reticle.
  - RAW DNG / High-Resolution capture toggle.
- **Live Color Filters:** Vivid 0.5x, Golden Hour, Cinematic Teal & Orange, Noir B&W, Cyberpunk, Vintage 70s, Moody Forest.
- **Photo & Video Recording:** Photo capture with countdown timer (3s, 5s, 10s), smooth video recording with live counter and audio mute toggle.
- **Composition Aids:** Virtual horizon level gauge (turns green when level) & Rule of Thirds / Golden Ratio grids.
- **Automatic File Organizer:** Automatically segregates files into `UltraWideCamera/Photos` and `UltraWideCamera/Videos`.
- **In-App Media Hub & Pro Photo Editor:** Browse organized media with folder storage stats, fullscreen photo viewer, video player, and full editing suite (exposure, contrast, saturation, warmth, tint, vignette, rotate/flip, before/after comparison).

## 🚀 GitHub Actions Automated Build
This repository includes a continuous integration workflow located at `.github/workflows/build.yml`.
When you push to `main` or trigger the workflow manually:
1. Runs all unit and Robolectric tests.
2. Compiles and packages `app-debug.apk`.
3. Uploads the ready-to-install APK artifact directly under GitHub Actions.

## 🛠️ Local Build Instructions
```bash
# Clone the repository
git clone <your-repo-url>
cd <repo-folder>

# Run tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
