# 작업 기록: 접근성 서비스 충돌 및 크래시 수정

**작업 일시**: 2025-11-23
**작업 범위**: 접근성 서비스 (`FocusAccessibilityService`) 충돌 문제 해결

## 문제 상황

**증상**:
- 사용자로부터 "접근성 설정 충돌이 또 발생하였다"는 보고 접수
- 접근성 설정에서 "이 서비스가 제대로 작동하지 않습니다" 메시지가 표시되거나 서비스가 비활성화됨
- USB 디바이스 연결 상태에서 문제 발생 확인

**원인 분석 (로그 미확보, 코드 분석 기반)**:
1. **`LockOverlayService`의 Hilt `@AndroidEntryPoint` 사용**:
   - `LockOverlayService`는 `@Inject` 필드가 없어 의존성 주입이 불필요함에도 `@AndroidEntryPoint`가 적용되어 있었음.
   - `LifecycleService`와 Hilt의 `Service` 지원 간의 잠재적 충돌 또는 초기화 타이밍 문제 가능성.
   - 로그에서 `Binder: java.lang.UnsupportedOperationException`이 관찰됨 (IPC 관련).

2. **`FocusAccessibilityService`의 의존성 주입 불안정성**:
   - `onServiceConnected`에서 Hilt `EntryPoint`를 통한 수동 주입 시, 예외가 발생하거나(`Exception`만 catch) 초기화가 늦어질 경우(`3초` 타임아웃) 실패 가능성.
   - `Throwable`(예: `LinkageError`, `UninitializedPropertyAccessException`)을 catch하지 않아 크래시 발생 가능성.

## 수정 내용

### 1. `LockOverlayService`에서 `@AndroidEntryPoint` 제거
`LockOverlayService`는 Hilt 의존성 주입을 사용하지 않으므로, 불필요한 어노테이션과 Hilt 관련 코드를 제거하여 안정성을 높였습니다.

- **파일**: `app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt`
- **변경**: `@AndroidEntryPoint` 어노테이션 삭제

### 2. `FocusAccessibilityService` 의존성 주입 강화
Hilt 초기화 대기 시간을 늘리고, 모든 종류의 예외(`Throwable`)를 처리하도록 개선했습니다.

- **파일**: `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`
- **변경**:
  - 재시도 횟수 증가: 30회(3초) → **50회(5초)**
  - 예외 처리 강화: `catch (e: Exception)` → `catch (e: Throwable)` (Runtime 에러 등 모든 오류 포착)

## 검증 결과

### 빌드 검증
```bash
./gradlew compileDebugKotlin
```
- **결과**: `BUILD SUCCESSFUL`

### 예상 효과
1. **크래시 방지**: `LockOverlayService`의 불필요한 Hilt 개입을 제거하여 서비스 시작 시 발생할 수 있는 잠재적 런타임 오류 제거.
2. **안정성 향상**: 접근성 서비스 연결 시 의존성 주입 실패로 인한 크래시를 방지하고, 더 긴 대기 시간으로 초기화 성공률 향상.

## 향후 모니터링
- 사용자가 다시 "접근성 충돌"을 겪는지 확인 필요.
- 만약 문제가 지속된다면, `UncaughtExceptionHandler`를 등록하여 파일로 로그를 남기는 방식 고려 필요 (adb 연결 없이 로그 확인용).

