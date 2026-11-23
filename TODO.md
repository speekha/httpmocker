# iOS Follow-up Tasks

Tasks identified during the iOS/Kotlin Native implementation review. Work through these one at a time, starting with the high-priority implementation and test coverage items.

## High Priority

- [x] Report iOS directory-creation failures from `IosFileAccessor.mkdir()`.
  - Files: `mocker-core/src/iosMain/kotlin/fr/speekha/httpmocker/io/IosFileAccessor.kt`
  - `mkdir()` now checks `NSFileManager.createDirectoryAtPath` and throws `IOException` with the failed path.
  - Added iOS tests for nested creation, repeated creation of an existing directory, and an existing-file collision.
  - Native `NSError` details are not available through the current Kotlin/Native Foundation binding; improve diagnostics under the later logging task if the binding/API permits it.

- [x] Define the supported iOS framework distribution model.
  - Decision: the supported channel is the per-target iOS klibs published to Maven. The consuming app's own shared Kotlin module depends on the modules it needs and builds the app's single framework (as `demo-ios` does). No per-combination frameworks are published.
  - Verified: `mocker-core`, `mocker-ktor`, `kotlinx-adapter` and `custom-adapter` publish klibs for each iOS target; root module metadata resolves each target, and `mocker-ktor` brings in `mocker-core` transitively.
  - The `iosX64` target was dropped from all modules (current Xcode no longer runs on Intel Macs). Published targets are `iosArm64` and `iosSimulatorArm64`. This is a breaking change for consumers still declaring `iosX64()`: mention it in the release notes.

- [ ] Follow-ups to the iOS distribution decision.
  - Optionally remove the unused `binaries.framework` blocks from `mocker-core`, `mocker-ktor`, `kotlinx-adapter` and `custom-adapter` (frameworks built per module cannot be combined in one app).
  - Add a check that a fresh project can consume the published klibs (the demo uses project dependencies).
  - Optional: one all-inclusive static XCFramework for Swift-only users, to be designed with the Swift facade task.

- [x] Add iOS integration tests for native file access, static mocks, and recording.
  - `tests-ios/.../io/IosFileAccessorTest.kt`: directories, nested paths, read/write round trips, empty files, multibyte UTF-8, missing-file and missing-folder failures.
  - `tests-ios/.../client/ktor/IosIntegrationTests.kt`: static scenarios from a temp folder (inline body, body file, 404), `DISABLED`/`MIXED`/`ENABLED` pass-through, `RECORD` writing nested files, replay of recorded files, appended records, and recording failures with `failOnRecordingError` on and off.
  - `FakeNetworkEngine.kt` replaces the real network as delegate engine, so the tests need no connectivity.
  - Bug found and fixed: `IosStreamWriter.write()` crashed on empty byte arrays (`addressOf(0)` on an empty pinned array), which also broke recording of empty response bodies.

- [ ] Replace or clarify the cross-platform `NetworkError.exceptionType` contract.
  - Files: `mocker-core/src/commonMain/kotlin/fr/speekha/httpmocker/model/Responses.kt`, `mocker-core/src/iosMain/kotlin/fr/speekha/httpmocker/ClassloaderUtils.kt`.
  - Introduce a cross-platform error category/code, or explicitly document exception type strings as JVM-only metadata.
  - Ensure iOS error behavior is predictable and test it.

## API And Interoperability

- [ ] Design a Swift-oriented iOS facade for the Ktor integration.
  - Files: likely a new iOS-facing API module or iOS source set alongside `mocker-ktor`.
  - Avoid exposing Ktor engine factories, generic configuration types, receiver DSLs, and Kotlin-scoped extension methods as the primary Swift API.
  - Provide explicit configuration methods and a protocol-friendly response provider/responder interface.
  - Keep the existing Kotlin DSL for Kotlin users.
  - Add a minimal Swift usage sample that does not require demo-specific Kotlin wrappers.

- [ ] Define lifecycle, cancellation, threading, and callback-retention contracts for Swift-facing APIs.
  - Files: Swift facade and `demo-ios` wrappers such as `GithubDemo` and `MockDemoRunner`.
  - Expose cancellation and/or `close()` for objects that own coroutine scopes or HTTP clients.
  - Document callback delivery thread and object-retention behavior.
  - Prefer Swift `async`/`await` consumption where Kotlin/Native interop supports it; otherwise return a cancellable operation handle.
  - Update the iOS demo to follow the finalized contract.

- [ ] Reduce the public Swift exposure of low-level file abstractions.
  - Files: `mocker-core/src/commonMain/kotlin/fr/speekha/httpmocker/io/`, Ktor builder APIs.
  - Evaluate making `FileAccessor`, `StreamReader`, and `StreamWriter` internal Kotlin implementation details.
  - Keep supported Swift-facing path or URL-based recording/configuration APIs.
  - Avoid requiring Swift consumers to work with Kotlin `ByteArray`, generic `use`, or Kotlin function types.
  - Remove public documentation that directs callers to implementation types such as `IosFileAccessor` unless they are intentionally part of the supported API.

## Reliability And Diagnostics

- [ ] Harden iOS stream length and text-decoding behavior.
  - Files: `mocker-core/src/iosMain/kotlin/fr/speekha/httpmocker/io/IosStreams.kt`.
  - Check `NSData.length` is within `Int.MAX_VALUE` before converting it to `Int`.
  - Throw a useful `IOException` for unsupported sizes.
  - Define and document the supported scenario encoding, expected to be UTF-8 unless intentionally broadened.
  - Wrap malformed-input errors with a useful file/decoding error.

- [ ] Improve iOS logger diagnostics.
  - Files: `mocker-core/src/iosMain/kotlin/fr/speekha/httpmocker/IosLogger.kt`.
  - Include exception class/type and stack trace or native `NSError` domain/code in error logs.
  - Evaluate replacing `println` with an iOS-native logging mechanism such as `NSLog` or OSLog.
  - Add tests or a small verification sample for the chosen logging behavior where practical.

- [ ] Replace the demo's integer-based mode setter with a stable, validated API.
  - Files: `demo-ios/src/iosMain/kotlin/fr/speekha/httpmocker/demo/GithubDemo.kt`.
  - Do not rely on `Mode.entries[index]` from Swift.
  - Use explicit mode methods, a validated stable mapping, or a Swift-visible mode type.
  - Ensure invalid values cannot crash the Swift caller.

## Build And Compatibility

- [x] Include the Intel iOS Simulator slice in the demo XCFramework, or intentionally remove its build target.
  - Resolved by removing the `iosX64` target from every module, including `demo-ios` and its `assembleXCFramework` task.

- [ ] Migrate CI from CircleCI to GitHub Actions.
  - Why: CircleCI only provides Linux machines on this plan, so the iOS simulator tests (`./gradlew :tests-ios:iosSimulatorArm64Test`) never run in CI, and iOS builds on Linux run close to the 4 GB memory limit (`commonizeNativeDistribution` was killed with exit code 137). GitHub Actions is free for public repositories, including macOS runners.
  - Port the existing jobs from `.circleci/config.yml`: dependency download, per-module builds, demo build and lint, JVM tests with JaCoCo, detekt, coverage upload (Codecov, Codacy), snapshot publishing on `develop` and release publishing on `main`.
  - Add a macOS job that builds the iOS targets and runs the iOS simulator tests.
  - Move the publishing secrets (Maven Central credentials, signing key) to GitHub repository secrets.
  - Remove the `-Pkotlin.commonizer.jvmArgs=-Xmx1g` workaround if the iOS builds move to macOS runners.
  - Update the CI badge in `README.md` and the CircleCI mentions in `AGENTS.md`, then remove `.circleci/`.
