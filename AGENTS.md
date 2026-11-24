# HttpMocker Project - Agent Guidelines

## Project Overview
HttpMocker is a Kotlin multiplatform library for mocking HTTP calls in OkHttp and Ktor clients.
Supports JVM, Android, and iOS platforms.

## Build System
- **Build Tool**: Gradle
- **Language**: Kotlin
- **Gradle Wrapper**: `./gradlew` (Unix/Linux/Mac), `gradlew.bat` (Windows)

## Key Commands

### Linting & Code Quality
```bash
# Run Detekt (Kotlin static analysis)
./gradlew detekt

# Run all verification tasks
./gradlew check
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for a specific module (e.g., mocker-core)
./gradlew :mocker-core:test

# Run iOS tests on the simulator (not included in ./gradlew test, which only runs JVM tests)
./gradlew :tests-ios:iosSimulatorArm64Test
```

### Build
```bash
# Build all modules
./gradlew build

# Build a specific module
./gradlew :mocker-core:build
```

### Publishing
```bash
# Publish locally
./publishLocal.sh  (or ./publishLocal.bat on Windows)

# Publish to snapshot repository (passes -Dsnapshot=true, which appends -SNAPSHOT to the version)
./publishSnapshot.sh  (or ./publishSnapshot.bat on Windows)

# Publish artifacts
./publish.sh  (or ./publish.bat on Windows)
```

## Code Quality Standards
- **Linter**: Detekt (configured in `detekt.yml`)
- **Max Issues**: 0 (zero tolerance policy)
- Must pass `./gradlew detekt` before completion
- Code follows Kotlin conventions

## File Locations

| Purpose                  | Location                                                  |
|--------------------------|-----------------------------------------------------------|
| Core mocking logic       | `mocker-core/src/commonMain/kotlin/`                      |
| OkHttp integration       | `mocker-okhttp/src/main/kotlin/` (JVM/Android)            |
| Android WebView support  | `mocker-webview/src/main/kotlin/` (Android, OkHttp only)  |
| Ktor integration         | `mocker-ktor/src/commonMain/kotlin/` (JVM and iOS)        |
| iOS-specific code        | `mocker-core/src/iosMain/` and `mocker-ktor/src/iosMain/` |
| Serialization adapters   | `{jackson,gson,moshi,kotlinx,custom,sax}-adapter/src/`    |
| JVM integration tests    | `tests/src/test/kotlin/`                                  |
| iOS integration tests    | `tests-ios/src/iosTest/kotlin/`                           |
| Android demo app         | `demo/` (`okhttp` and `ktor` flavors)                     |
| iOS demo app             | `demo-ios/` (Kotlin framework + Xcode project in `iosApp/`) |
| Dependency versions      | `gradle/libs.versions.toml`                               |
| Shared Gradle scripts    | `gradle/publish.gradle`, `gradle/detekt.gradle`, `gradle/coverage.gradle.kts` |

## Contribution Workflow
- `develop` is the main development branch; `main` holds releases
- Create feature branches from `develop` and open pull requests against `develop`
- CircleCI (`.circleci/config.yml`) runs on `develop` and `main`; it must pass before merging
- Follow the existing commit message style (see recent history)

## Important Notes
- iOS implementation was generated with Gemini and may contain bugs
- Always run linting after making changes
- The project uses multiple JSON/XML adapters for scenarios:
  - Jackson, Gson, Moshi (JVM/Android only)
  - KotlinX Serialization and custom JSON (all platforms, including iOS)
  - SAX-based XML (JVM/Android only)
