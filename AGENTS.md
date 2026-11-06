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

# Publish to snapshot repository
./publishSnapshot.sh  (or ./publishSnapshot.bat on Windows)

# Publish artifacts
./publish.sh  (or ./publish.bat on Windows)
```

## Code Quality Standards
- **Linter**: Detekt (configured in `detekt.yml`)
- **Max Issues**: 0 (zero tolerance policy)
- Must pass `./gradlew detekt` before completion
- Code follows Kotlin conventions

## Important Notes
- iOS implementation was generated with Gemini and may contain bugs
- Always run linting after making changes
- The project uses multiple JSON/XML adapters for scenarios:
  - Jackson, Gson, Moshi, KotlinX Serialization (all platforms)
  - Custom JSON and SAX implementations
