# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/java/com/allday/detoxy/` follows Clean Architecture: `domain` defines use cases and models, `data` integrates Room persistence, `presentation` hosts Jetpack Compose UI and ViewModels, `service` manages long-running timers, and `core` shares cross-cutting utilities.
- UI code lives under `presentation/ui/FeatureNameScreen.kt`, backed by Hilt ViewModels in `presentation/viewmodel/`.
- Persistence artifacts reside in `data/local/` (Room entities and DAOs) and are bound through modules in `core/di/`.
- Instrumentation assets belong in `app/src/androidTest/` as needed; reusable resources stay in `app/src/main/res/`.
- `docs/` tracks product requirements and MVP checklists; update the relevant page whenever you deliver a feature.
- `working_history/` keeps chronological task logs (`YYYY-MM-DD_x.y.md`) summarizing scope, verification commands, and resulting commit IDs.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build an installable debug APK for local testing.
- `./gradlew clean assembleDebug` — clear intermediates before reproducing build issues.
- `./gradlew assembleRelease` — produce the release artifact after QA approval.
- `./gradlew test` — run JVM unit tests (JUnit) across modules.
- `./gradlew connectedAndroidTest` — execute instrumentation tests on an attached device or emulator.
- `./gradlew lint` and `./gradlew compileDebugKotlin` — enforce style rules and verify Kotlin compilation prior to pull requests.

## Coding Style & Naming Conventions
- Kotlin with 4-space indentation; place braces on new lines for classes and functions.
- Prefer expression bodies only for single-line returns; otherwise favor descriptive block bodies.
- Classes and interfaces use UpperCamelCase; functions, properties, and locals use lowerCamelCase. Compose state holders end with `State`, and public `StateFlow` exposures use `uiState`.
- Preserve Clean Architecture boundaries: UI → ViewModel → domain use cases → repositories; the data layer must stay presentation-agnostic.

## Testing Guidelines
- Unit tests live alongside code in `src/test/java`; instrumentation specs reside in `src/androidTest/java`.
- Name tests `<Feature>Test` or `<Class>Test`; parameterized suites become `<Class>ParameterizedTest`.
- Cover timer lifecycle transitions (start, resume, finish, give-up), permission degradations, and background service recovery.
- Log executed verification commands and outcomes in the corresponding `working_history` entry.
- Run `./gradlew test` and, when platform integrations change, `./gradlew connectedAndroidTest` before requesting review.

## Commit & Pull Request Guidelines
- Write imperative commit messages with scope tags, e.g., `feat(timer): sync overlay on resume`, and keep each commit focused on one logical change.
- Before committing, capture a `working_history` note with summary, verification evidence, and resulting commit hashes.
- Pull requests must link to the relevant `docs/` checklist item, describe architecture impacts, and attach screenshots or logs when UI, overlay, or permission flows change.
- Confirm Accessibility, Overlay, and DND services reset to a safe state on failure paths before seeking review, and flag any residual risk in the PR description.
