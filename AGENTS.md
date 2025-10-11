# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android application module with `src/main/java/com/allday/detoxy` split by Clean Architecture (`domain`, `data`, `presentation`, `service`, `core`).
- `presentation/ui/`: Jetpack Compose screens by feature; `presentation/viewmodel/` houses Hilt ViewModels.
- `data/local/`: Room entities and DAOs; `core/di/` exposes them through Hilt modules.
- `docs/`: Product requirements, MVP checklist; update when delivering features.
- `working_history/`: Task-by-task logs (`YYYY-MM-DD_x.y.md`) that summarize work, verification, and commit IDs.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: Compile the debug APK used for local installs.
- `./gradlew assembleRelease`: Produce the release APK; run after passing QA.
- `./gradlew clean assembleDebug`: Guarantee a clean build when diagnosing issues.
- `./gradlew test`: Execute JVM unit tests across modules.
- `./gradlew connectedAndroidTest`: Run instrumented tests on an emulator or device.
- `./gradlew lint` / `./gradlew compileDebugKotlin`: Verify style and Kotlin compilation prior to PRs.

## Coding Style & Naming Conventions
- Kotlin with 4-space indentation; prefer expression body functions only for trivial returns.
- Classes/interfaces: UpperCamelCase (`FocusTimer`), functions & properties: lowerCamelCase.
- Compose state holders end with `State`; ViewModel `StateFlow` exposed via `uiState`.
- Keep Clean Architecture boundaries: UI talks to ViewModels, which depend on domain use cases.
- Run `./gradlew lint` before pushing; address warnings rather than suppressing them.

## Testing Guidelines
- Unit tests live beside source in `src/test/java`; instrumented specs in `src/androidTest/java`.
- Name tests `<Feature>Test` or `<Class>Test`; parameterized variants use `<Class>ParameterizedTest`.
- Cover timer lifecycle paths (start, finish, give-up) and permission fallbacks.
- Record executed commands and outcomes inside the matching `working_history/` entry.

## Commit & Pull Request Guidelines
- Craft commit messages with imperative verbs plus scope, e.g., `feat(timer): sync overlay on resume`.
- Each task produces a work-history note before the final commit; include build/test evidence and commit hashes.
- PRs should link to the related doc checklist item, describe architecture impacts, and attach relevant screenshots or logs (timer UI, overlay, permissions).
- Verify all services (Accessibility, Overlay, DND) reset state on failure paths before requesting review.
