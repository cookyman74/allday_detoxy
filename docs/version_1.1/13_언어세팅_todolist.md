# 다국어 지원 기능 작업 계획서 (v1.0)

> **TDD 방법론 기반**: Red → Green → Refactor 사이클 적용  
> **작업 원칙**: 테스트 먼저 작성 → 최소 코드 구현 → 리팩터링  
> **참고 문서**: [99_TDD_plan.md](./99_TDD_plan.md)  
> **작업 결과서 템플릿**: [template.md](../working_history/version_1.1/template.md)  
> **버전**: v1.0

---

## 📋 작업 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | 다국어 지원 - 한국어/영어/스페인어/일본어/중국어 5개 언어 지원 |
| 영향 범위 | `res/values*/strings.xml`, `AppLocaleManager`, `LanguageSettingsScreen`, `NotificationHelper`, 모든 하드코딩된 문자열 |
| 위험 수준 | 🟠 High (앱 전반 영향, 번역 정확도 중요) |
| 참고 PRD | [13_언어세팅_prd.md](./13_언어세팅_prd.md) |
| 작업 브랜치 | `v1.1/feat/world_language_form` |

---

## 🚨 핵심 리스크 요약

| 리스크 | 영향 | 대응 방안 | 상태 |
|--------|------|----------|------|
| 번역 누락/오역 | 🔴 High | 모든 문자열 추출 후 체계적 번역 검수 | ⬜ |
| 텍스트 길이 차이로 UI 깨짐 | 🟠 Medium | 각 언어별 UI 레이아웃 테스트 | ⬜ |
| NotificationChannel 사용자 설정 초기화 | 🟠 Medium | 같은 ID로 재호출하여 이름만 갱신 | ⬜ |
| 하드코딩된 문자열 누락 | 🟡 Low | Lint 규칙으로 자동 검출 | ⬜ |

---

## 📦 Phase 1: 기반 구조 설정

> 📄 **목표**: Android 다국어 지원 기반 구조 구축 (locale_config, AppLocaleManager)  
> 📄 **PRD 참조**: 섹션 2️⃣ 기술 아키텍처

### 1.1 사전 작업

- [x] **[CONTEXT]** PRD 문서 검토
  - 파일: [13_언어세팅_prd.md](./13_언어세팅_prd.md)
  - 확인: 지원 언어, BCP-47 태그, 폴더 구조

- [x] **[ANALYSIS]** 현재 리소스 구조 분석
  - 파일: `app/src/main/res/values/strings.xml`
  - 확인: 기존 문자열 리소스 현황

- [x] **[ANALYSIS]** AndroidManifest.xml 확인
  - 파일: `app/src/main/AndroidManifest.xml`
  - 확인: `android:localeConfig` 미설정 상태 확인

### 1.2 본 작업

- [x] **[TASK-001]** locale_config.xml 생성
  - 파일: `app/src/main/res/xml/locale_config.xml`
  - 내용:
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
        <locale android:name="ko"/>
        <locale android:name="en"/>
        <locale android:name="es"/>
        <locale android:name="ja"/>
        <locale android:name="zh-Hans"/>
    </locale-config>
    ```

- [x] **[TASK-002]** AndroidManifest.xml에 localeConfig 추가
  - 파일: `app/src/main/AndroidManifest.xml`
  - 추가: `android:localeConfig="@xml/locale_config"`

- [x] **[TASK-003]** AppLocaleManager 유틸리티 생성
  - 파일: `core/locale/AppLocaleManager.kt`
  - 메서드:
    - `setAppLocale(languageTag: String?)` - 앱 언어 설정
    - `getAppLocale(): String?` - 현재 앱 언어 조회
    - `getSystemPrimaryLocale(): Locale` - 시스템 1순위 로케일

- [x] **[TASK-004]** 리소스 폴더 생성
  - 폴더 생성:
    - `app/src/main/res/values-en/`
    - `app/src/main/res/values-es/`
    - `app/src/main/res/values-ja/`
    - `app/src/main/res/values-b+zh+Hans/`
  - 각 폴더에 빈 `strings.xml` 생성

- [x] **[GREEN]** 빌드 검증
  ```bash
  ./gradlew compileDebugKotlin
  ```

### 1.3 사후 작업

- [ ] **[VERIFY]** locale_config.xml 정상 로드 확인
  - Android 13+ 에뮬레이터에서 시스템 설정 > 앱 > 언어 확인

- [x] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/언어세팅/Phase1_기반구조_2026-01-15.md`

- [x] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "feat(locale): add locale_config and AppLocaleManager"
  ```
  - 커밋: `7d24d3c`

---

## 📦 Phase 2: 문자열 추출

> 📄 **목표**: 하드코딩된 한국어 문자열을 strings.xml로 이동  
> 📄 **PRD 참조**: 섹션 6️⃣ 번역 대상 문자열

### 2.1 사전 작업

- [x] **[REVIEW]** Phase 1 작업 결과서 검토
  - 파일: `working_history/version_1.1/언어세팅/Phase1_기반구조_2026-01-15.md`

- [x] **[ANALYSIS]** 하드코딩 문자열 검색
  ```bash
  # Compose 파일에서 한글 검색
  grep -rn "Text(\"[가-힣]" app/src/main/java/
  grep -rn "stringResource" app/src/main/java/ | wc -l
  ```
  - 결과: 약 1,159개 하드코딩 문자열 발견 (30개+ 파일)

- [x] **[ANALYSIS]** 현재 strings.xml 구조 확인
  - 파일: `app/src/main/res/values/strings.xml`
  - 기존: 2개 문자열 (app_name, accessibility_service_description)

### 2.2 본 작업

- [/] **[TASK-001]** 화면 제목 문자열 추출
  - 대상: "집중 타이머", "설정", "스케줄", "리포트" 등
  - 작업: strings.xml에 추가 + 코드에서 `stringResource()` 사용
  - ✅ 완료: MainActivity.kt (4개 문자열)

- [/] **[TASK-002]** 버튼/라벨 문자열 추출
  - 대상: "시작하기", "취소", "저장", "삭제" 등
  - 작업: strings.xml에 추가 + 코드에서 `stringResource()` 사용
  - ✅ 완료: strings.xml에 ~20개 버튼 문자열 정의

- [/] **[TASK-003]** 설명 텍스트 문자열 추출
  - 대상: 권한 안내, 기능 설명, 도움말 텍스트
  - 작업: strings.xml에 추가 + 코드에서 `stringResource()` 사용
  - ✅ 완료: strings.xml에 ~50개 설명 문자열 정의

- [/] **[TASK-004]** 알림 메시지 문자열 추출
  - 대상: NotificationChannel 이름/설명, 알림 본문
  - 파일: `FocusTimerService.kt`, `NotificationHelper.kt`
  - 작업: strings.xml에 추가 + `context.getString()` 사용
  - ✅ 완료: strings.xml에 ~10개 알림 문자열 정의

- [/] **[TASK-005]** 에러 메시지 문자열 추출
  - 대상: 권한 거부 메시지, 오류 안내
  - 작업: strings.xml에 추가
  - ✅ 완료: strings.xml에 ~5개 에러 문자열 정의

- [x] **[GREEN]** 빌드 검증
  ```bash
  ./gradlew compileDebugKotlin
  ```
  - 결과: ✅ BUILD SUCCESSFUL

- [ ] **[REFACTOR]** 중복 문자열 정리
  - 동일 의미의 중복 문자열 통합
  - 일관된 네이밍 규칙 적용 (snake_case)
  - ⚠️ 남은 파일 추출 완료 후 진행 예정

### 2.3 사후 작업

- [ ] **[VERIFY]** 문자열 누락 검사
  ```bash
  # 하드코딩된 한글이 남아있는지 확인
  grep -rn "\"[가-힣]" app/src/main/java/ --include="*.kt"
  ```
  - ⚠️ 남은 파일 추출 완료 후 진행 예정

- [ ] **[LINT]** Lint 검사 (HardcodedText)
  ```bash
  ./gradlew lint
  ```

- [/] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/언어세팅/Phase2_문자열추출_2026-01-15.md`
  - 내용: 추출된 문자열 목록, 총 개수
  - 상태: 프로그레스 ~15% 기록

- [/] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "refactor(strings): extract hardcoded Korean strings to strings.xml"
  ```
  - 부분 커밋: `5e92a6c` (strings.xml 기반 구조 + MainActivity.kt)

---

## 📦 Phase 3: 번역 파일 생성

> 📄 **목표**: 영어/스페인어/일본어/중국어 번역 파일 생성  
> 📄 **PRD 참조**: 섹션 1.2 지원 언어

### 3.1 사전 작업

- [ ] **[REVIEW]** Phase 2 작업 결과서 검토
  - 확인: 추출된 전체 문자열 목록

- [ ] **[ANALYSIS]** 번역 대상 문자열 수 파악
  ```bash
  grep -c "<string" app/src/main/res/values/strings.xml
  ```

### 3.2 본 작업

- [ ] **[TASK-001]** 영어 번역 파일 생성
  - 파일: `app/src/main/res/values-en/strings.xml`
  - 모든 문자열 영어로 번역

- [ ] **[TASK-002]** 스페인어 번역 파일 생성
  - 파일: `app/src/main/res/values-es/strings.xml`
  - 모든 문자열 스페인어로 번역

- [ ] **[TASK-003]** 일본어 번역 파일 생성
  - 파일: `app/src/main/res/values-ja/strings.xml`
  - 모든 문자열 일본어로 번역

- [ ] **[TASK-004]** 중국어(간체) 번역 파일 생성
  - 파일: `app/src/main/res/values-b+zh+Hans/strings.xml`
  - 모든 문자열 중국어 간체로 번역

- [ ] **[TASK-005]** 복수형 처리 (plurals)
  - 영어 등 복수형이 필요한 언어에 `<plurals>` 리소스 추가
  ```xml
  <plurals name="minutes_remaining">
      <item quantity="one">%d minute remaining</item>
      <item quantity="other">%d minutes remaining</item>
  </plurals>
  ```

- [ ] **[GREEN]** 빌드 검증
  ```bash
  ./gradlew compileDebugKotlin
  ```

### 3.3 사후 작업

- [ ] **[VERIFY]** 번역 누락 검사
  - 각 언어별 strings.xml 문자열 수 동일 확인
  ```bash
  for lang in "" "-en" "-es" "-ja" "-b+zh+Hans"; do
    echo "values$lang: $(grep -c '<string' app/src/main/res/values$lang/strings.xml)"
  done
  ```

- [ ] **[VERIFY]** 포맷 파라미터 일치 확인
  - `%s`, `%d` 등 파라미터가 모든 번역에 동일하게 포함되어 있는지 확인

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/언어세팅/Phase3_번역파일_YYYY-MM-DD.md`
  - 내용: 각 언어별 번역 완료 문자열 수

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "feat(i18n): add translations for en, es, ja, zh-Hans"
  ```

---

## 📦 Phase 4: 설정 UI 구현

> 📄 **목표**: 언어 설정 화면 구현 및 즉시 적용 기능  
> 📄 **PRD 참조**: 섹션 3️⃣ UI 설계

### 4.1 사전 작업

- [ ] **[REVIEW]** Phase 3 작업 결과서 검토

- [ ] **[ANALYSIS]** 기존 설정 화면 구조 확인
  - 파일: `presentation/ui/settings/` 관련 파일
  - 확인: 기존 스타일, 패턴

### 4.2 본 작업

- [ ] **[TASK-001]** LanguageSettingsScreen Composable 생성
  - 파일: `presentation/ui/settings/LanguageSettingsScreen.kt`
  - UI:
    ```
    ○ 시스템 기본값 (자동)
    ○ 한국어
    ○ English
    ○ Español
    ○ 日本語
    ○ 中文 (简体)
    ```

- [ ] **[TASK-002]** 언어 선택 시 AppLocaleManager 호출
  - `AppCompatDelegate.setApplicationLocales()` 사용
  - 선택 즉시 적용

- [ ] **[TASK-003]** 설정 화면에 언어 설정 메뉴 추가
  - 파일: 기존 설정 화면 (일반 설정 또는 앱 설정 섹션)
  - 네비게이션: LanguageSettingsScreen으로 이동

- [ ] **[TASK-004]** NotificationChannel 이름 갱신 로직
  - 파일: `core/notification/NotificationHelper.kt`
  - 메서드: `updateNotificationChannelLocale(context)`
  - 언어 변경 시 호출

- [ ] **[GREEN]** 빌드 검증
  ```bash
  ./gradlew compileDebugKotlin
  ```

### 4.3 사후 작업

- [ ] **[VERIFY]** 언어 변경 즉시 적용 확인
  - 설정에서 언어 변경 → 앱 전체 UI 언어 변경 확인

- [ ] **[VERIFY]** NotificationChannel 이름 갱신 확인
  - 시스템 설정 > 앱 > 알림에서 채널 이름 확인

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/언어세팅/Phase4_설정UI_YYYY-MM-DD.md`

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "feat(settings): add language settings screen"
  ```

---

## 📦 Phase 5: 테스트 및 검증

> 📄 **목표**: 각 언어별 UI 레이아웃 및 기능 검증  
> 📄 **PRD 참조**: 섹션 7️⃣ 구현 단계 - Phase 5

### 5.1 사전 작업

- [ ] **[REVIEW]** Phase 4 작업 결과서 검토

- [ ] **[ANALYSIS]** 테스트 대상 정리
  - 각 언어별 주요 화면 목록
  - UI 깨짐 가능성 높은 부분 식별

### 5.2 본 작업

- [ ] **[TASK-001]** 영어 UI 테스트
  - 각 화면 텍스트 길이로 인한 UI 깨짐 확인
  - 스크린샷 캡처

- [ ] **[TASK-002]** 스페인어 UI 테스트
  - 스페인어는 영어보다 텍스트가 긴 경향이 있음
  - 버튼, 라벨 오버플로우 확인

- [ ] **[TASK-003]** 일본어 UI 테스트
  - CJK 폰트 렌더링 확인
  - 줄바꿈 동작 확인

- [ ] **[TASK-004]** 중국어 UI 테스트
  - CJK 폰트 렌더링 확인
  - 간체 문자 정상 표시 확인

- [ ] **[TASK-005]** 12/24시간 형식 테스트
  - 시스템 설정 변경 후 앱 내 시간 표시 확인
  - `DateFormat.getTimeFormat()` 정상 동작 확인

- [ ] **[TASK-006]** UI 깨짐 수정
  - 발견된 레이아웃 이슈 수정
  - 텍스트 축약 또는 줄바꿈 처리

### 5.3 사후 작업

- [ ] **[VERIFY]** 전체 언어 스크린샷 확보
  - 앱스토어 등록용 스크린샷 준비

- [ ] **[DOC]** 작업 결과서 작성
  - 파일: `working_history/version_1.1/언어세팅/Phase5_테스트검증_YYYY-MM-DD.md`
  - 내용: 각 언어별 테스트 결과, 수정 사항

- [ ] **[COMMIT]** 변경사항 커밋
  ```bash
  git commit -m "fix(i18n): fix UI layout issues for multi-language support"
  ```

---

## ✅ 최종 체크리스트

### 기능 검증
- [ ] 모든 Phase 작업 완료
- [ ] 5개 언어 전환 정상 작동
- [ ] 시스템 언어 자동 감지 작동
- [ ] NotificationChannel 현지화 작동
- [ ] 12/24시간 시스템 설정 반영
- [ ] 린터 경고 0개

### 문서화
- [ ] 각 Phase별 작업 결과서 작성 완료
- [ ] 앱스토어 다국어 메타데이터 준비

### 최종 커밋 및 PR
- [ ] 모든 변경사항 커밋 완료
- [ ] PR 생성 및 코드 리뷰 요청

---

## 📊 진행 체크리스트

### Phase 완료 조건
| Phase | 빌드 통과 | 린터 통과 | 결과서 작성 | 커밋 완료 | 상태 |
|-------|----------|----------|------------|----------|------|
| Phase 1 (기반 구조) | ✅ | ⬜ | ✅ | ✅ | ✅ 완료 |
| Phase 2 (문자열 추출) | ✅ | ⬜ | ✅ | 🟡 | 🟡 진행중 (~15%) |
| Phase 3 (번역 파일) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 4 (설정 UI) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Phase 5 (테스트 검증) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

---

## ⚠️ 주의사항

1. **번역 품질**: 기계 번역 사용 시 반드시 네이티브 검수 필요
2. **문자열 ID 규칙**: snake_case 사용, 의미가 명확하게 (예: `btn_start_timer`)
3. **포맷 파라미터**: `%s`, `%d` 등 파라미터 위치는 언어마다 다를 수 있음 → `%1$s` 형식 권장
4. **복수형 처리**: 영어 외 언어도 복수형 규칙 확인 필요
5. **NotificationChannel**: 삭제하지 않고 같은 ID로 재호출하여 사용자 설정 보존

---

## 🔗 관련 문서

- [13_언어세팅_prd.md](./13_언어세팅_prd.md) - 기획설계 문서
- [99_TDD_plan.md](./99_TDD_plan.md) - TDD 방법론 가이드
- [template.md](../working_history/version_1.1/template.md) - 작업 결과서 템플릿

---

## 📅 예상 일정

| Phase | 예상 소요 | 시작일 | 완료일 | 비고 |
|-------|----------|--------|--------|------|
| Phase 1 (기반 구조) | 1일 | - | - | locale_config, AppLocaleManager |
| Phase 2 (문자열 추출) | 1일 | - | - | 약 130개 문자열 예상 |
| Phase 3 (번역 파일) | 2일 | - | - | 4개 언어 번역 |
| Phase 4 (설정 UI) | 0.5일 | - | - | LanguageSettingsScreen |
| Phase 5 (테스트 검증) | 1일 | - | - | 각 언어별 UI 테스트 |
| **Total** | **5.5일** | - | - | - |

---

**작성일**: 2026-01-07  
**작성자**: AI Assistant  
**최종 수정일**: 2026-01-07  
**상태**: 🟡 검토 대기
