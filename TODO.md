# TODO

- [ ] Migrate CI from CircleCI to GitHub Actions.
  - Why: CircleCI only provides Linux machines on this plan, so the iOS simulator tests (`./gradlew :tests-ios:iosSimulatorArm64Test`) never run in CI, and iOS builds on Linux run close to the 4 GB memory limit (`commonizeNativeDistribution` was killed with exit code 137). GitHub Actions is free for public repositories, including macOS runners.
  - Port the existing jobs from `.circleci/config.yml`: dependency download, per-module builds, demo build and lint, JVM tests with JaCoCo, detekt, coverage upload (Codecov, Codacy), snapshot publishing on `develop` and release publishing on `main`.
  - Add a macOS job that builds the iOS targets and runs the iOS simulator tests.
  - Move the publishing secrets (Maven Central credentials, signing key) to GitHub repository secrets.
  - Remove the `-Pkotlin.commonizer.jvmArgs=-Xmx1g` workaround if the iOS builds move to macOS runners.
  - Revisit the memory workarounds in the `build_demo` job (one Gradle run per flavor, in-process Kotlin compiler, `-Xmx2560m`) and `checkReleaseBuilds = false` in `demo/build.gradle.kts`, which were only needed to fit in CircleCI's 4 GB.
  - Update the CI badge in `README.md` and the CircleCI mentions in `AGENTS.md`, then remove `.circleci/`.
