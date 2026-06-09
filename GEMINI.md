# GEMINI.md - HttpMocker Project Guide

## Project Overview

**HttpMocker** is a lightweight, multi-platform Kotlin library for mocking HTTP calls during testing and development. It
supports both **OkHttp** and **Ktor** HTTP client libraries and provides flexible configuration for unit tests,
integration tests, and offline/demo modes.

### Key Characteristics

- **Language:** Kotlin (multiplatform: JVM, Android, iOS)
- **Build System:** Gradle with multiplatform configuration
- **License:** Apache License 2.0
- **Current Version:** 2.0.0-alpha
- **Java Target:** JVM 21+
- **iOS Targets:** iosArm64, iosSimulatorArm64, iosX64
- **Publication:** Maven Central (Stable releases) and Sonatype snapshots

### Core Modules

- **mocker-core**: Platform-agnostic core mocking logic (multiplatform)
- **mocker-okhttp**: OkHttp interceptor implementation
- **mocker-ktor**: Ktor client engine implementation
- **Adapters** (6 options for scenario parsing):
    - jackson-adapter, gson-adapter, moshi-adapter, kotlinx-adapter, custom-adapter, sax-adapter

### Project Architecture

The project is a Gradle multimodule build with the following structure:

- **Root**: Configuration and publishing setup via shared `gradle/` scripts
- **Core Logic**: `mocker-core/` (multiplatform Kotlin, JVM 21)
- **HTTP Client Integration**: `mocker-okhttp/`, `mocker-ktor/` (ktor supports iOS)
- **Serialization Adapters**: One module per supported parser (Jackson, Gson, Moshi, KotlinX, custom JSON, SAX/XML)
- **Test Harness**: `tests/` module integrating all adapters and engines with JUnit 5 (JVM)
- **iOS Test Harness**: `tests-ios/` module integrating ktor with KotlinX and custom adapters for iOS targets
- **Demo App**: `demo/` for Android and JVM showcases

## Building and Running

### Prerequisites

- JDK 21+
- Gradle (wrapper included: `gradlew`/`gradlew.bat`)

### Key Build Commands

#### Build All Modules

```bash
./gradlew build
```

#### Run Tests (All)

```bash
./gradlew test
```

#### Run Specific Module Tests

```bash
./gradlew :mocker-core:test
./gradlew :tests:test
./gradlew :tests-ios:iosSimulatorArm64Test  # iOS simulator tests
```

#### Code Quality Checks

```bash
# Detekt (static analysis)
./gradlew detekt
```

#### Local Publication (for local Maven repository)

```bash
./publishLocal.sh          # macOS/Linux
./publishLocal.bat         # Windows
```

#### Snapshot Publication (to Sonatype)

```bash
./publishSnapshot.sh       # macOS/Linux
./publishSnapshot.bat      # Windows
```

#### Full Release Publication

```bash
./publish.sh               # macOS/Linux
./publish.bat              # Windows
```

### Gradle Properties

Key configuration in `gradle.properties`:

- **GROUP**: `fr.speekha.httpmocker`
- **VERSION_NAME**: `2.0.0-alpha`
- **kotlin.code.style**: `official`
- **Snapshot Mode**: Enable via system property: `-Dsnapshot=true`

## Development Conventions

### Language & Style

- **Language**: Kotlin with official code style
- **Static Analysis**: Detekt with custom configuration (`detekt.yml`)
- **JVM Target**: 21 (specified per-module in build.gradle.kts files)

### Module Structure

Each module follows the standard Kotlin multiplatform layout:

```
module-name/
├── build.gradle                 # Module-specific Gradle config
├── gradle.properties            # Module version/metadata
├── src/
│   ├── commonMain/kotlin/       # Shared code (all platforms)
│   ├── commonTest/kotlin/       # Shared tests
│   ├── jvmMain/kotlin/          # JVM-specific implementation
│   ├── jvmTest/kotlin/          # JVM tests
│   ├── iosMain/kotlin/          # iOS-specific (mocker-ktor)
│   └── resources/               # Config/data files
└── build/                       # Build output (ignored)
```

### Kotlin Multiplatform Configuration

- **Kotlin Version**: 2.3.21
- **Multiplatform Targets**:
    - `jvm`: JVM 21 with JUnit test framework
    - `ios`: iOS targets (iosArm64, iosSimulatorArm64, iosX64) in mocker-core and mocker-ktor
- **iOS Support**: Framework binaries generated for iOS targets (baseline name: `httpmocker`)
- **Coroutines**: `kotlinx-coroutines-core` 1.11.0 (commonMain dependency)

### Testing Standards

- **Test Framework**: JUnit 5 (Jupiter) 5.10.2 (JVM), Kotlin Test (iOS)
- **Mocking Library**: MockK 1.14.9 (JVM only)
- **Test Runner Config**: `useJUnitPlatform()` in tests module; kotlin-test framework for iOS
- **Mock Web Server**: OkHttp MockWebServer for HTTP testing (JVM)
- **Test Execution**: 
    - JVM/Android: All tests in `tests/` module integrate all adapters and HTTP engines
    - iOS: Integration tests in `tests-ios/` module (ktor + KotlinX/custom adapters)

### Dependency Management

All versions centralized in `gradle/libs.versions.toml`:

- Kotlin: 2.3.21
- Coroutines: 1.11.0
- OkHttp: 5.3.2
- Ktor: 2.3.13
- Jackson: 2.21.4, Gson: 2.14.0, Moshi: 1.15.2, KotlinX Serialization: 1.11.0
- SLF4J: 1.7.36 (logging API)
- Detekt: 1.23.8

### Publishing & Release

- **Maven Central**: via vanniktech maven publish plugin (v0.33.0)
- **Gradle Publish Plugin**: `com.vanniktech.maven.publish`
- **Snapshot Repository**: `https://oss.sonatype.org/content/repositories/snapshots`
- **Publication Scripts**: `publishLocal.sh`, `publishSnapshot.sh`, `publish.sh`
- **ProGuard Rules**: See `demo/proguard-rules.pro` for obfuscation guidance

### Code Organization

- **Package Naming**: `fr.speekha.httpmocker.*`
- **Main Classes Located In**: `mocker-core/src/commonMain/kotlin/fr/speekha/httpmocker/`
- **Public APIs**: Documented in README.md with examples for both OkHttp and Ktor
- **Common Abstractions**:
    - `RequestCallback`: Interface for dynamic mock responses
    - `ResponseDescriptor`: Data class for HTTP responses
    - `Mapper`: Interface for scenario file parsing (multiple implementations)
    - `FilingPolicy`: Interface for organizing mock scenario files

### Configuration Files

- **`.editorconfig`**: Cross-editor formatting standards
- **`detekt.yml`**: Custom static analysis rules
- **`.circleci/`**: CI/CD pipeline configuration
- **`.github/ISSUE_TEMPLATE/`**: Bug report and feature request templates
- **`gradle/`**: Shared gradle scripts:
    - `libs.versions.toml`: Centralized dependency versions
    - `versions.gradle.kts`: Gradle version extensions
    - `publish.gradle`: Publishing configuration for all modules
    - `coverage.gradle.kts`: Code coverage settings
    - `detekt.gradle`: Static analysis configuration

## Common Workflows

### Adding a New Feature

1. Create feature branch from `develop`
2. Implement in appropriate module(s) (core logic in `mocker-core`)
3. Add corresponding tests in `tests/` module
4. Run `./gradlew build` to verify compilation, tests, and linting
5. Ensure `./gradlew detekt` passes
6. Submit PR against `develop` branch (CircleCI validation required)

### Running All Checks Locally

```bash
./gradlew clean build detekt
```

### Debugging Tests

- Tests use JUnit 5 with parameterized test support
- MockK for mocking Kotlin classes
- OkHttp MockWebServer for HTTP protocol testing
- Run with IDE runner or: `./gradlew :tests:test --info`

### Working with Scenario Files

Scenarios are static mock definitions stored as JSON or XML:

- **JSON Format**: List of request-response pairs with regex body matching support
- **XML Format**: SAX-parsed alternative using `sax-adapter`
- **Organization**: Managed by `FilingPolicy` (e.g., `MirrorPathPolicy` for URL-based folder structure)
- **Examples**: See README.md for JSON and XML schema with request/response/error cases

### Local Testing with Demo App

- **demo/**: Android app with OkHttp and Ktor integration examples
- Use `./gradlew :demo:build` for apk generation
- Proguard rules in `demo/proguard-rules.pro` required for obfuscated builds

## Important Notes

### Multiplatform Status

- JVM/Android: Stable (production-ready)
- iOS: Supported with Ktor client integration (iosArm64, iosSimulatorArm64, iosX64 targets)

### Dependency Strategy

- Adapter modules allow choosing the serialization library matching your project
- SLF4J is used for logging (simple implementation in tests)
- All adapters optional; you can provide custom `Mapper` implementation

### Git & Commits

- Repository uses `develop` branch for main development
- `.circleci/` configuration runs tests automatically on push
- Commit messages should follow existing patterns (see recent history)

### Breaking Changes (2.0.0)

- Upgraded to OkHttp 5 API (from OkHttp 3 in v1.x)
- Kotlin 2.3.21 (significant language evolution)
- Ktor 2.3.13 compatibility

## File Locations Quick Reference

| Purpose                | Location                                                      |
| Purpose                | Location                                                      |
|------------------------|---------------------------------------------------------------|\n| Core Mocking Logic     | `mocker-core/src/commonMain/kotlin/` (multiplatform)          |
| OkHttp Integration     | `mocker-okhttp/src/main/kotlin/` (JVM/Android)                |
| Ktor Integration       | `mocker-ktor/src/commonMain/kotlin/` (multiplatform iOS+JVM)  |
| iOS-Specific Code      | `mocker-core/src/iosMain/` and `mocker-ktor/src/iosMain/`     |
| Serialization Adapters | `{jackson,gson,moshi,kotlinx,custom,sax}-adapter/src/`        |
| Integration Tests      | `tests/src/test/kotlin/` (JVM/Android)                        |
| iOS Integration Tests  | `tests-ios/src/iosTest/kotlin/` (iOS targets)                 |
| Test Resources         | `tests/src/test/resources/` and `demo/src/test/resources/`    |
| Documentation          | `README.md` (primary), `.github/ISSUE_TEMPLATE/` (templates)  |
| Gradle Config          | `build.gradle`, `settings.gradle`, `gradle/` (shared scripts) |
| Build Output           | `build/` (in each module, gitignored)                         |
| License                | `LICENSE.txt` (Apache 2.0)                                    |
| Template               | `license.template` (for generated files)                      |
