# Instagram Clone (KMP)

A Kotlin Multiplatform Instagram-style clone targeting Android and iOS with shared UI using Compose Multiplatform.

## Preview
<p align="center">
  <img src="docs/images/profile-preview.png" alt="Profile screen preview" width="280" />
</p>

## Features
- Profile screen with editable profile data (username, full name, bio, website)
- Instagram-like highlights strip with gradient story rings
- Clickable highlights with story viewer behavior
- Multi-image highlight stories with:
  - horizontal swipe pager
  - tap-left / tap-right navigation
  - auto-advance progress bars
- Post grid with image + video posts
- Post detail screen with:
  - Android/iOS video playback
  - like and comment actions
  - local-only persistence for likes/comments (no server write)

## Data Source
- App reads profile data from GitHub Pages JSON:
  - `mock-api/v1/profile.json`
- If you change mock data locally, push/deploy to GitHub Pages so devices load the latest payload.

## Project Structure
- `composeApp/src/commonMain` shared UI/domain/data logic
- `composeApp/src/androidMain` Android-specific implementations (video player, back handler)
- `composeApp/src/iosMain` iOS-specific implementations (video player, back handler)
- `iosApp` iOS app entry project for Xcode
- `mock-api` JSON payload used by remote profile API

## Build
### Android
```bash
./gradlew :composeApp:assembleDebug
```

### iOS (Kotlin framework compile)
```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

### Tests
```bash
./gradlew :composeApp:testDebugUnitTest
```

## Tech Stack
- Kotlin Multiplatform
- Compose Multiplatform
- Koin
- Ktor
- Coil 3
- Media3 (Android video)
- AVPlayerViewController (iOS video)
