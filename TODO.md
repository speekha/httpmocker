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
  - Verified: `mocker-core`, `mocker-ktor`, `kotlinx-adapter` and `custom-adapter` publish klibs for `iosArm64`, `iosSimulatorArm64` and `iosX64`; root module metadata resolves each target, and `mocker-ktor` brings in `mocker-core` transitively.

- [ ] Follow-ups to the iOS distribution decision.
  - Document the iOS setup in the README: a shared KMP module depending on `mocker-ktor`, an adapter and the Darwin engine, producing the app framework.
  - Optionally remove the unused `binaries.framework` blocks from `mocker-core`, `mocker-ktor`, `kotlinx-adapter` and `custom-adapter` (frameworks built per module cannot be combined in one app).
  - Add a check that a fresh project can consume the published klibs (the demo uses project dependencies).
  - Optional: one all-inclusive static XCFramework for Swift-only users, to be designed with the Swift facade task.

- [ ] Add iOS integration tests for native file access, static mocks, and recording.
  - Files: `tests-ios/src/iosTest/`, iOS file-access implementation as needed.
  - Test `IosFileAccessor.mkdir()`, nested `getFile()` paths, reading, writing, empty files, and multibyte UTF-8 content.
  - Test static scenario loading from a real writable iOS path.
  - Test `RECORD` mode writing nested scenario files.
  - Test read/write/directory-creation failures.
  - Test actual `DISABLED` and `MIXED` pass-through behavior with a deterministic delegate rather than an external service.

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

- [ ] Include the Intel iOS Simulator slice in the demo XCFramework, or intentionally remove its build target.
  - Files: `demo-ios/build.gradle.kts`.
  - `assembleXCFramework` currently builds `iosX64` but does not pass that framework to `xcodebuild -create-xcframework`.
  - Either add the `iosX64` framework argument or remove the unused target/task dependency.
  - Verify the resulting XCFramework works on Apple Silicon and Intel simulator environments supported by the project.
