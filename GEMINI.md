# GEMINI.md - HttpMocker Project Guide

## Project Overview

**HttpMocker** is a lightweight, multi-platform Kotlin library for mocking HTTP calls during testing and development. It
supports both **OkHttp** and **Ktor** HTTP client libraries and provides flexible configuration for unit tests,
integration tests, and offline/demo modes.

### Key Characteristics

- **Language:** Kotlin (multiplatform: JVM, Android, iOS in development)
- **Build System:** Gradle with multiplatform configuration
- **License:** Apache License 2.0
- **Current Version:** 2.0.0-alpha
- **Java Target:** JVM 11+
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
- **Core Logic**: `mocker-core/` (multiplatform Kotlin, JVM 11)
- **HTTP Client Integration**: `mocker-okhttp/`, `mocker-ktor/`
- **Serialization Adapters**: One module per supported parser (Jackson, Gson, Moshi, KotlinX, custom JSON, SAX/XML)
- **Test Harness**: `tests/` module integrating all adapters and engines with JUnit 5
- **Demo Apps**: `demo/` and `demo2/` for Android and JVM showcases

## Building and Running

### Prerequisites

- JDK 11+ (configured in gradle.properties: `kotlin.jvmargs=-Xmx1536m`)
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
```

#### Code Quality Checks

```bash
# KtLint (Kotlin linting) - applied to tests and demo only
./gradlew ktlintCheck ktlintFormat

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
- **Linting**: KtLint (applied only to test and demo modules via `gradle/ktlint.gradle`)
- **Static Analysis**: Detekt with custom configuration (`detekt.yml`)
- **JVM Target**: 11 (specified per-module in build.gradle files)

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
    - `jvm`: JVM 11 with JUnit test framework
    - `ios`: iOS target (in mocker-ktor, development status)
- **Coroutines**: `kotlinx-coroutines-core` 1.5.2 (commonMain dependency)

### Testing Standards

- **Test Framework**: JUnit 5 (Jupiter) 1.8.1
- **Mocking Library**: MockK 1.10.2
- **Test Runner Config**: `useJUnitPlatform()` in tests module
- **Mock Web Server**: OkHttp MockWebServer for HTTP testing
- **Test Execution**: All tests in `tests/` module integrate all adapters and HTTP engines

### Dependency Management

All versions centralized in `gradle/versions.gradle`:

- Kotlin: 2.3.21
- Coroutines: 1.5.2
- OkHttp: 4.9.2
- Ktor: 1.6.4
- Jackson, Gson, Moshi, KotlinX Serialization (with specific versions)
- SLF4J: 1.7.32 (logging API)

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
    - `versions.gradle`: Centralized dependency versions
    - `publish.gradle`: Publishing configuration for all modules
    - `coverage.gradle`: Code coverage settings
    - `detekt.gradle`: Static analysis configuration
    - `ktlint.gradle`: Kotlin linting configuration
    - `dokka.gradle`: Kotlin documentation generation
    - `sources.gradle`: Source artifact generation

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
./gradlew clean build detekt ktlintCheck
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

### Local Testing with Demo Apps

- **demo/**: Android app with OkHttp and Ktor integration examples
- **demo2/**: Additional Android test app
- Use `./gradlew :demo:build` for apk generation
- Proguard rules in `demo/proguard-rules.pro` required for obfuscated builds

## Important Notes

### Multiplatform Status

- JVM/Android: Stable (production-ready)
- iOS: Alpha status - contributions welcome for implementation and deployment

### Dependency Strategy

- Adapter modules allow choosing the serialization library matching your project
- SLF4J is used for logging (simple implementation in tests)
- All adapters optional; you can provide custom `Mapper` implementation

### Git & Commits

- Repository uses `develop` branch for main development
- `.circleci/` configuration runs tests automatically on push
- Commit messages should follow existing patterns (see recent history)

### Breaking Changes (2.0.0)

- Upgraded to OkHttp 4 API (from OkHttp 3 in v1.x)
- Kotlin 2.3.21 (significant language evolution)
- Ktor 1.6.4 compatibility

## File Locations Quick Reference

| Purpose                | Location                                                      |
|------------------------|---------------------------------------------------------------|
| Core Mocking Logic     | `mocker-core/src/commonMain/kotlin/`                          |
| OkHttp Integration     | `mocker-okhttp/src/main/kotlin/`                              |
| Ktor Integration       | `mocker-ktor/src/commonMain/kotlin/`                          |
| Serialization Adapters | `{jackson,gson,moshi,kotlinx,custom,sax}-adapter/src/`        |
| Integration Tests      | `tests/src/test/kotlin/`                                      |
| Test Resources         | `tests/src/test/resources/` and `demo/src/test/resources/`    |
| Documentation          | `README.md` (primary), `.github/ISSUE_TEMPLATE/` (templates)  |
| Gradle Config          | `build.gradle`, `settings.gradle`, `gradle/` (shared scripts) |
| Build Output           | `build/` (in each module, gitignored)                         |
| License                | `LICENSE.txt` (Apache 2.0)                                    |
| Template               | `license.template` (for generated files)                      |
